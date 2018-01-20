package ma.ac.emi.simplesoftphone.sip;

import ma.ac.emi.simplesoftphone.rtp.RtpLink;
import ma.ac.emi.simplesoftphone.ui.SipBasicPhone;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SipListenerClient implements SipListener {

    private SipLink sipLink;

    SipListenerClient(SipLink sipLink) {
        this.sipLink = sipLink;
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction transaction = requestEvent.getServerTransaction();
        Dialog dialog = requestEvent.getDialog();
        sipLink.dialog = dialog;

        sipLink.ui.addReceivedMessage(request.toString());
        try {
            if (transaction == null) {
                transaction = sipLink.sipProvider.getNewServerTransaction(request);
            }
            sipLink.transaction = transaction;

            ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);

            Response response;

            switch (request.getMethod()) {
                case Request.INVITE:
                    if (sipLink.divertSipAddress != null) {
                        Address addressTo = sipLink.addressFactory.createAddress(sipLink.divertSipAddress);
                        ToHeader toHeader = sipLink.headerFactory.createToHeader(addressTo, null);
                        request.setHeader(toHeader);
                        request.setRequestURI(addressTo.getURI());
                        sipLink.sipProvider.sendRequest(request);
                        break;
                    }
                    sipLink.remoteSipAddress = viaHeader.getMAddr();

                    SessionDescription sessionDescription = sipLink.getSDPData(request);

                    Vector mediaDescs = sessionDescription.getMediaDescriptions(false);

                    MediaDescription am = (MediaDescription) mediaDescs.get(0);

                    sipLink.remoteRtpPort = am.getMedia().getMediaPort();

                    FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);

                    response = sipLink.messageFactory.createResponse(Response.RINGING, request);

                    ((ToHeader) response.getHeader("To")).setTag(String.valueOf(sipLink.tag));
                    response.addHeader(sipLink.contactHeader);

                    transaction.sendResponse(response);

                    sipLink.ui.addSentMessage(response.toString());
                    sipLink.ui.incomingCall(fromHeader.toString());
                    break;
                case Request.CANCEL:
                case Request.BYE:
                    response = sipLink.messageFactory.createResponse(Response.OK, request);
                    ((ToHeader) response.getHeader("To")).setTag(String.valueOf(sipLink.tag));
                    response.addHeader(sipLink.contactHeader);
                    transaction.sendResponse(response);

                    sipLink.ui.addSentMessage(response.toString());
                    sipLink.ui.cancelCall();
                    if (request.getMethod().equals(Request.BYE))
                        sipLink.stopRtp();
                    break;
                case "ACK":

                    break;
            }
        } catch (Exception ex) {
            Logger.getLogger(SipBasicPhone.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        ClientTransaction transaction = responseEvent.getClientTransaction();
        Dialog dialog = responseEvent.getDialog();

        sipLink.dialog = dialog;
        sipLink.ui.addReceivedMessage(response.toString());
        Request request;

        try {
            CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

            if (cseq.getMethod().equals(Request.INVITE)) {

                if (response.getStatusCode() == Response.OK) {
                    ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
                    sipLink.remoteSipAddress = toHeader.getAddress().getURI().toString();

                    byte[] rawContent = response.getRawContent();
                    String sdpContent = new String(rawContent, "UTF-8");

                    SdpFactory sdpFactory = SdpFactory.getInstance();
                    SessionDescription sessionDescription = sdpFactory.createSessionDescription(sdpContent);

                    Vector mediaDescs = sessionDescription.getMediaDescriptions(false);

                    Connection c = sessionDescription.getConnection();

                    MediaDescription am = (MediaDescription) mediaDescs.get(0);

                    sipLink.remoteRtpPort = am.getMedia().getMediaPort();

                    request = dialog.createAck(cseq.getSeqNumber());

                    if (sipLink.proxySipAddress != null) {
                        RouteHeader routeHeader = sipLink.headerFactory.createRouteHeader(sipLink.addressFactory.createAddress(sipLink.proxySipAddress));
                        request.addHeader(routeHeader);
                    }
                    sipLink.contactHeader = sipLink.headerFactory.createContactHeader(sipLink.contactAddress);
                    request.addHeader(sipLink.contactHeader);
                    dialog.sendAck(request);
                    String remoteRtpAddress = RtpLink.audioUriFromAddress(c.getAddress(), sipLink.remoteRtpPort);

                    sipLink.ui.answeredCall();
                    sipLink.ui.addSentMessage(request.toString());
                    sipLink.startRtp(remoteRtpAddress);
                } else if (response.getStatusCode() == Response.RINGING) {
                    sipLink.ui.ringing();
                } else if (response.getStatusCode() == Response.DECLINE) {
                    sipLink.ui.cancelCall();
                }
            } else if (cseq.getMethod().equals(Request.REGISTER)) {
                ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
                sipLink.proxySipAddress = toHeader.getAddress().getURI().toString();
                int i = 0;
            }

        } catch (Exception ex) {
            Logger.getLogger(SipBasicPhone.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {

    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
