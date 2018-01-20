package ma.ac.emi.simplesoftphone.sip;

import ma.ac.emi.simplesoftphone.rtp.RtpLink;
import ma.ac.emi.simplesoftphone.ui.SipBasicPhone;

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

    private SipFactory sipFactory; // Pour acceder à l’API SIP.
    private SipStack sipStack; // Le SIP stack.
    SipProvider sipProvider; // Pour envoyer des messages SIP.
    MessageFactory messageFactory; // Pour créer les messages SIP.
    HeaderFactory headerFactory; // Pour créer les entêtes SIP.
    AddressFactory addressFactory; // Pour créer les SIP URIs.
    private ListeningPoint listeningPoint; // SIP listening IP address/port.
    private Properties properties; // autres propriétés.
    // Objets pour stocker la configuration locale.


    String ip; // Adresse IP locale
    int sipPort; // Port Sip local.
    String protocol = "udp"; // Protocole local de transport (UDP).

    String remoteSipAddress;

    int rtpPort; // Port Sip local.

    int remoteRtpPort; // Port Sip local.

    int tag = (new Random()).nextInt(); // Le tag local.
    Address contactAddress; // L’adresse de contact.
    ContactHeader contactHeader; // L’entête contact.
    private static String transport;
    public static SdpFactory sdpFactory; //pour le corps du message SIP (SDP)

    Request lastRequest;
    Dialog dialog; // Dialogue Client
    ServerTransaction transaction;

    SipListenerImpl sipListener;

    RtpLink rtpLink;

    SipBasicPhone ui;

    public SipLink(String ip, int sipPort, int rtpPort, SipBasicPhone ui) throws InvalidArgumentException {
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

        this.sipFactory = SipFactory.getInstance();
        this.sipFactory.setPathName("gov.nist");
        this.properties = new Properties();
        this.properties.setProperty("javax.sip.STACK_NAME", "stack");
        try {
            this.sipStack = this.sipFactory.createSipStack(this.properties);
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
        }
        try {
            this.messageFactory = this.sipFactory.createMessageFactory();
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
        }
        try {
            this.headerFactory = this.sipFactory.createHeaderFactory();
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
        }
        try {
            this.addressFactory = this.sipFactory.createAddressFactory();
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
        }
        try {
            this.listeningPoint = this.sipStack.createListeningPoint(this.ip, this.sipPort, this.protocol);
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

        sipListener = new SipListenerImpl(this);
        try {
            this.sipProvider.addSipListener(sipListener);
        } catch (TooManyListenersException e) {
            e.printStackTrace();
        }


        System.out.println("sip:" + this.ip + ":" + this.sipPort + "\n");
    }

    public String createSDPData(int localBasePort, int remoteBasePort) {
        try {
            sdpFactory = SdpFactory.getInstance();
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
            System.out.println("An SDP exception occurred while generating sdp description");
            e.printStackTrace();
        }
        return "No SDP set";
    }

    public void takeCall() {
        try {
            Request request = transaction.getRequest();
            byte[] rawContent = request.getRawContent();
            String sdpContent = new String(rawContent, "UTF-8");
            SdpFactory sdpFactory = SdpFactory.getInstance();
            SessionDescription sessionDescription = sdpFactory.createSessionDescription(sdpContent);

            Connection c = sessionDescription.getConnection();

            FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);

            Response response;
            response = messageFactory.createResponse(Response.OK, request);

            ((ToHeader) response.getHeader("To")).setTag(String.valueOf(tag));
            response.addHeader(contactHeader);

            ContentTypeHeader contentTypeHeader
                    = headerFactory.createContentTypeHeader("application", "sdp");

            String sdpData = createSDPData(rtpPort, 0); //create SDP content
            response.setContent(sdpData, contentTypeHeader);

            transaction.sendResponse(response);

            ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
            String remoteRtpAddress = RtpLink.audioUriFromAddress(c.getAddress(), remoteRtpPort);

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

            ((ToHeader) response.getHeader("To")).setTag(String.valueOf(tag));
            response.addHeader(contactHeader);

            ContentTypeHeader contentTypeHeader
                    = headerFactory.createContentTypeHeader("application", "sdp");

            String sdpData = createSDPData(rtpPort, 0); //create SDP content
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
            // Créer le To Header
            // Obtenir l’adresse de destination à partir du text field.
            Address addressTo = this.addressFactory.createAddress(address);

            //addressTo.setDisplayName("");

            ToHeader toHeader = headerFactory.createToHeader(addressTo, null);
            // Créer le request URI pour les messages SIP.
            javax.sip.address.URI requestURI = addressTo.getURI();
            // Affecter le type du protocole de Transport TCP ou UDP??
            transport = "udp";
            // Créer les Via Headers
            ArrayList viaHeaders = new ArrayList();
            ViaHeader viaHeader = this.headerFactory.createViaHeader(
                    this.ip,
                    this.sipPort,
                    transport,
                    null);
            // ajouter les via headers
            viaHeaders.add(viaHeader);
            // Créer le ContentTypeHeader
            ContentTypeHeader contentTypeHeader
                    = headerFactory.createContentTypeHeader("application", "sdp");
            // Créer une nouvelle entête CallId
            CallIdHeader callIdHeader = sipProvider.getNewCallId();
            // Créer une nouvelle entête Cseq
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);
            // Créer une nouvelle entête MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
            // Créer le "From" header.
            FromHeader fromHeader = this.headerFactory.createFromHeader(this.contactAddress,
                    String.valueOf(this.tag));

            // Créer la requête Invite.
            Request request = messageFactory.createRequest(
                    requestURI,
                    Request.INVITE,
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwards
            );

            // Ajouter l’adresse de contacte.
            //contactAddress.setDisplayName("Alice Contact");
            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            String sdpData = createSDPData(rtpPort, 0); //create SDP content
            request.setContent(sdpData, contentTypeHeader);

            // Créer la transaction client.
            ClientTransaction inviteTid = this.sipProvider.getNewClientTransaction(request);
            // envoyer la requête
            inviteTid.sendRequest();

            dialog = inviteTid.getDialog();
            ui.addSentMessage(request.toString());
        } catch (Exception e) {
        }
    }

    public void cancelCall() {
        try {
            // Créer le To Header
            // Obtenir l’adresse de destination à partir du text field.
            Address addressTo = this.addressFactory.createAddress(remoteSipAddress);

            //addressTo.setDisplayName("");

            ToHeader toHeader = headerFactory.createToHeader(addressTo, null);
            // Créer le request URI pour les messages SIP.
            javax.sip.address.URI requestURI = addressTo.getURI();
            // Affecter le type du protocole de Transport TCP ou UDP??
            transport = "udp";
            // Créer les Via Headers
            ArrayList viaHeaders = new ArrayList();
            ViaHeader viaHeader = this.headerFactory.createViaHeader(
                    this.ip,
                    this.sipPort,
                    transport,
                    null);
            // ajouter les via headers
            viaHeaders.add(viaHeader);
            // Créer le ContentTypeHeader
            ContentTypeHeader contentTypeHeader
                    = headerFactory.createContentTypeHeader("application", "sdp");
            // Créer une nouvelle entête CallId
            CallIdHeader callIdHeader = dialog.getCallId();

            // Créer une nouvelle entête Cseq
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(dialog.getLocalSeqNumber(), Request.CANCEL);
            // Créer une nouvelle entête MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
            // Créer le "From" header.
            FromHeader fromHeader = this.headerFactory.createFromHeader(this.contactAddress,
                    String.valueOf(this.tag));

            // Créer la requête Invite.
            Request request = messageFactory.createRequest(
                    requestURI,
                    Request.CANCEL,
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwards
            );

            // Ajouter l’adresse de contacte.
            //contactAddress.setDisplayName("Alice Contact");
            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            ClientTransaction endTid = this.sipProvider.getNewClientTransaction(request);
            dialog.sendRequest(endTid);
            ui.addSentMessage(request.toString());
        } catch (Exception e) {
            //Afficher l’erreur en cas de problème.
            System.out.println("Failed: " + e.getMessage() + "\n");
        }
    }

    public void endCall() {
        try {
            // Créer le To Header
            Address addressTo = this.addressFactory.createAddress(remoteSipAddress);
            //addressTo.setDisplayName("");
            ToHeader toHeader = headerFactory.createToHeader(addressTo, null);
            // Créer le request URI pour les messages SIP.
            javax.sip.address.URI requestURI = addressTo.getURI();
            // Affecter le type du protocole de Transport TCP ou UDP??
            transport = "udp";
            // Créer les Via Headers
            ArrayList viaHeaders = new ArrayList();
            ViaHeader viaHeader = this.headerFactory.createViaHeader(
                    this.ip,
                    this.sipPort,
                    transport, null);
            // ajouter les via headers
            viaHeaders.add(viaHeader);
            // Créer le ContentTypeHeader
            ContentTypeHeader contentTypeHeader
                    = headerFactory.createContentTypeHeader("application", "sdp");
            // Créer une nouvelle entête CallId
            CallIdHeader callIdHeader = dialog.getCallId();
            // Créer une nouvelle entête Cseq
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.BYE);
            // Créer une nouvelle entête MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
            // Créer le "From" header.
            FromHeader fromHeader = this.headerFactory.createFromHeader(this.contactAddress,
                    String.valueOf(this.tag));

            // Créer la requête Invite.
            Request request = messageFactory.createRequest(requestURI, Request.BYE,
                    callIdHeader, cSeqHeader, fromHeader,
                    toHeader, viaHeaders, maxForwards);

            // Ajouter l’adresse de contacte.
            //contactAddress.setDisplayName("Alice Contact");
            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // Créer la transaction client.
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

    public void sendRequest() {

    }

    public static String uriFromAddress(String address) {
        return "sip:" + address;
    }

    public static String uriFromAddress(String ip, int port) {
        return uriFromAddress(ip + ":" + port);
    }

    public static String uriFromAddress(String ip, String port) {
        return uriFromAddress(ip + ":" + port);
    }


}
