package ma.ac.emi.simplesoftphone.sip;

import ma.ac.emi.simplesoftphone.rtp.RtpLink;
import ma.ac.emi.simplesoftphone.ui.SipCall;

import javax.sdp.*;
import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;

public class SipLink {
    private SipStack sipStack;
    SipProvider sipProvider;
    MessageFactory messageFactory;
    HeaderFactory headerFactory;
    AddressFactory addressFactory;
    private ListeningPoint listeningPoint;

    private String ip;
    private int sipPort;

    String remoteSipAddress;
    String proxySipAddress;
    String divertSipAddress;

    private int rtpPort;

    int remoteRtpPort;

    int tag = (new Random()).nextInt();
    Address contactAddress;
    ContactHeader contactHeader;

    ServerTransaction transaction;
    Dialog dialog;

    private RtpLink rtpLink;

    SipCall ui;

    public SipLink(String ip, int sipPort, int rtpPort, SipCall ui) throws InvalidArgumentException {
        this.ip = ip;
        this.sipPort = sipPort;
        this.rtpPort = rtpPort;
        this.ui = ui;
        initSIPStack();
    }

    private void initSIPStack() throws InvalidArgumentException {
        try {
            this.ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "stack");
        try {
            this.sipStack = sipFactory.createSipStack(properties);
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
        }
        try {
            this.messageFactory = sipFactory.createMessageFactory();
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
        }
        try {
            this.headerFactory = sipFactory.createHeaderFactory();
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
        }
        try {
            this.addressFactory = sipFactory.createAddressFactory();
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
        }
        try {
            String protocol = "udp";
            this.listeningPoint = this.sipStack.createListeningPoint(this.ip, this.sipPort, protocol);
        } catch (TransportNotSupportedException e) {
            e.printStackTrace();
        }
        try {
            this.sipProvider = this.sipStack.createSipProvider(this.listeningPoint);
        } catch (ObjectInUseException e) {
            e.printStackTrace();
        }

        try {
            this.contactAddress = this.addressFactory.createAddress("sip:" + this.ip + ":" + this.sipPort);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        this.contactHeader = this.headerFactory.createContactHeader(contactAddress);


        SipListenerClient sipListener = new SipListenerClient(this);
        try {
            this.sipProvider.addSipListener(sipListener);
        } catch (TooManyListenersException e) {
            e.printStackTrace();
        }
        System.out.println("sip:" + this.ip + ":" + this.sipPort + "\n");
    }

    private String createSDPData(int localBasePort) {
        try {
            SdpFactory sdpFactory = SdpFactory.getInstance();
            SessionDescription sessDescr = sdpFactory.createSessionDescription();

            Version v = sdpFactory.createVersion(0);
            //o=
            Origin o = sdpFactory.createOrigin("1234", 0, 0, "IN", "IP4", ip);
            //"s=-"
            SessionName s = sdpFactory.createSessionName("-");
            //c=
            Connection c = sdpFactory.createConnection("IN", "IP4", ip);
            //"t=0 0"
            TimeDescription t = sdpFactory.createTimeDescription();
            Vector timeDescs = new Vector();
            timeDescs.add(t);
            // -------- Description du media Audio
            String[] formats = {"0", "4", "18"};
            MediaDescription am = sdpFactory.createMediaDescription(
                    "audio",
                    localBasePort,
                    1,
                    "RTP/AVP",
                    formats);
            Vector mediaDescs = new Vector();
            mediaDescs.add(am);
            sessDescr.setVersion(v);
            sessDescr.setOrigin(o);
            sessDescr.setConnection(c);
            sessDescr.setSessionName(s);
            sessDescr.setTimeDescriptions(timeDescs);

            if (mediaDescs.size() > 0) {
                sessDescr.setMediaDescriptions(mediaDescs);
            }
            return sessDescr.toString();
        } catch (SdpException e) {
            e.printStackTrace();
        }
        return "";
    }

    public SessionDescription getSDPData(Request request) {
        SessionDescription sessionDescription = null;
        try {
            byte[] rawContent = request.getRawContent();
            String sdpContent = new String(rawContent, "UTF-8");
            SdpFactory sdpFactory = SdpFactory.getInstance();
            sessionDescription = sdpFactory.createSessionDescription(sdpContent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sessionDescription;
    }

    public void takeCall() {
        try {
            Request request = transaction.getRequest();

            Response response = messageFactory.createResponse(Response.OK, request);
            ((ToHeader) response.getHeader(ToHeader.NAME)).setTag(String.valueOf(tag));
            response.addHeader(contactHeader);

            ContentTypeHeader contentTypeHeader
                    = headerFactory.createContentTypeHeader("application", "sdp");
            String sdpData = createSDPData(rtpPort);
            response.setContent(sdpData, contentTypeHeader);

            transaction.sendResponse(response);

            SessionDescription sessionDescription = getSDPData(request);
            Connection c = sessionDescription.getConnection();
            String remoteRtpAddress = RtpLink.audioUriFromAddress(c.getAddress(), remoteRtpPort);

            FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
            remoteSipAddress = fromHeader.getAddress().getURI().toString();
            ui.addSentMessage(response.toString());
            startRtp(remoteRtpAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hangUp() {
        try {
            Request request = transaction.getRequest();
            Response response;
            response = messageFactory.createResponse(Response.DECLINE, request);

            ((ToHeader) response.getHeader(ToHeader.NAME)).setTag(String.valueOf(tag));
            response.addHeader(contactHeader);

            ContentTypeHeader contentTypeHeader
                    = headerFactory.createContentTypeHeader("application", "sdp");

            String sdpData = createSDPData(rtpPort);
            response.setContent(sdpData, contentTypeHeader);

            transaction.sendResponse(response);
            ui.addSentMessage(request.toString());
            stopRtp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void call(String address) {
        try {
            remoteSipAddress = address;

            String sdpData = createSDPData(rtpPort);
            Request request = createRequest(Request.INVITE, address, sdpData);

            ClientTransaction inviteTid = sipProvider.getNewClientTransaction(request);
            inviteTid.sendRequest();

            dialog = inviteTid.getDialog();
            ui.addSentMessage(request.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void register(String proxyAddress) {
        try {
            remoteSipAddress = proxyAddress;

            String sdpData = createSDPData(rtpPort);
            Request request = createRequest(Request.REGISTER, proxyAddress, sdpData);

            sipProvider.sendRequest(request);

            ui.addSentMessage(request.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerService(String divertAddress) {
        divertSipAddress = divertAddress;
    }

    public void cancelService() {
        divertSipAddress = null;
    }


    public void cancelCall() {
        try {
            Request request = createRequest(Request.CANCEL, remoteSipAddress);

            ClientTransaction endTid = this.sipProvider.getNewClientTransaction(request);
            dialog.sendRequest(endTid);

            ui.addSentMessage(request.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void endCall() {
        try {
            Request request = createRequest(Request.BYE, remoteSipAddress, null, dialog.getCallId());
            ClientTransaction endTid = this.sipProvider.getNewClientTransaction(request);
            dialog.sendRequest(endTid);

            stopRtp();
            ui.addSentMessage(request.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startRtp(String remoteAddress) {
        rtpLink = RtpLink.start(
                RtpLink.audioUriFromAddress(ip, rtpPort),
                remoteAddress
        );
    }

    public void stopRtp() {
        if (rtpLink != null)
            rtpLink.stop();
    }

    private Request createRequest(String method, String address) {
        return createRequest(method, address, null, null);
    }

    private Request createRequest(String method, String address, String sdpData) {
        return createRequest(method, address, sdpData, null);
    }

    private Request createRequest(String method, String address, String sdpData, CallIdHeader callIdHeader) {
        Request request = null;

        try {
            Address addressTo = this.addressFactory.createAddress(address);

            ToHeader toHeader = headerFactory.createToHeader(addressTo, null);
            javax.sip.address.URI requestURI = addressTo.getURI();

            ArrayList viaHeaders = new ArrayList();

            ViaHeader viaHeader = this.headerFactory.createViaHeader(
                    this.ip,
                    this.sipPort,
                    "udp",
                    null);

            viaHeaders.add(viaHeader);

            if (callIdHeader == null)
                callIdHeader = sipProvider.getNewCallId();

            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, method);

            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

            FromHeader fromHeader = this.headerFactory.createFromHeader(this.contactAddress,
                    String.valueOf(this.tag));

            request = messageFactory.createRequest(
                    requestURI,
                    method,
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwards
            );

            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            if (this.proxySipAddress != null) {
                RouteHeader routeHeader = headerFactory.createRouteHeader(this.addressFactory.createAddress(this.proxySipAddress));
                request.addHeader(routeHeader);
            }

            if (sdpData != null) {
                ContentTypeHeader contentTypeHeader
                        = headerFactory.createContentTypeHeader("application", "sdp");
                sdpData = createSDPData(rtpPort); //create SDP content
                request.setContent(sdpData, contentTypeHeader);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

        return request;
    }

    private static String uriFromAddress(String address) {
        return "sip:" + address;
    }

    public static String uriFromAddress(String ip, int port) {
        return uriFromAddress(ip + ":" + port);
    }

    public static String uriFromAddress(String ip, String port) {
        return uriFromAddress(ip + ":" + port);
    }


}
