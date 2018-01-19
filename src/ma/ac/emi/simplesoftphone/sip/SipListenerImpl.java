package ma.ac.emi.simplesoftphone.sip;

import ma.ac.emi.simplesoftphone.rtp.RtpLink;
import ma.ac.emi.simplesoftphone.ui.SipBasicPhone;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.*;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SipListenerImpl implements SipListener {

    SipLink sipLink;

    public SipListenerImpl(SipLink sipLink) {
        this.sipLink = sipLink;
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        sipLink.ui.addReceivedMessage(request.toString());

        try {
            sipLink.transaction = requestEvent.getServerTransaction();
            if (null == sipLink.transaction) {
                sipLink.transaction = sipLink.sipProvider.getNewServerTransaction(request);
            }

            ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
            Response response;

            if (request.getMethod().equals(Request.INVITE)) {

                sipLink.remoteSipAddress = viaHeader.getMAddr();

                byte[] rawContent = request.getRawContent();
                String sdpContent = new String(rawContent, "UTF-8");

                SdpFactory sdpFactory = SdpFactory.getInstance();
                SessionDescription sessionDescription = sdpFactory.createSessionDescription(sdpContent);

                Vector mediaDescs = sessionDescription.getMediaDescriptions(false);

                MediaDescription am = (MediaDescription) mediaDescs.get(0);

                sipLink.remoteRtpPort = am.getMedia().getMediaPort();

                FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);

                response = sipLink.messageFactory.createResponse(Response.RINGING, request);

                ((ToHeader) response.getHeader("To")).setTag(String.valueOf(sipLink.tag));
                response.addHeader(sipLink.contactHeader);

                ContentTypeHeader contentTypeHeader
                        = sipLink.headerFactory.createContentTypeHeader("application", "sdp");

                sipLink.transaction.sendResponse(response);

                sipLink.ui.addSentMessage(response.toString());
                sipLink.ui.incomingCall(fromHeader.toString());
            } else if (request.getMethod().equals(Request.CANCEL) || request.getMethod().equals(Request.BYE)) {
                response = sipLink.messageFactory.createResponse(Response.OK, request);
                ((ToHeader) response.getHeader("To")).setTag(String.valueOf(sipLink.tag));
                response.addHeader(sipLink.contactHeader);
                sipLink.transaction.sendResponse(response);

                sipLink.ui.addSentMessage(response.toString());
                sipLink.ui.cancelCall();
                if (request.getMethod().equals(Request.BYE))
                    sipLink.stopRtp();
            } else if (request.getMethod().equals("ACK")) {

            }
        } catch (Exception ex) {
            Logger.getLogger(SipBasicPhone.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();

        sipLink.ui.addReceivedMessage(response.toString());
        Request request;

        try {
            CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
            ViaHeader viaHeader = (ViaHeader) response.getHeader(ViaHeader.NAME);

            if (cseq.getMethod().equals(Request.INVITE)) {


                if (response.getStatusCode() == Response.OK) {
                    byte[] rawContent = response.getRawContent();
                    String sdpContent = new String(rawContent, "UTF-8");

                    SdpFactory sdpFactory = SdpFactory.getInstance();
                    SessionDescription sessionDescription = sdpFactory.createSessionDescription(sdpContent);

                    Vector mediaDescs = sessionDescription.getMediaDescriptions(false);

                    Connection c = sessionDescription.getConnection();

                    MediaDescription am = (MediaDescription) mediaDescs.get(0);

                    sipLink.remoteRtpPort = am.getMedia().getMediaPort();

                    request = sipLink.dialog.createAck(cseq.getSeqNumber());

                    sipLink.contactHeader = sipLink.headerFactory.createContactHeader(sipLink.contactAddress);
                    request.addHeader(sipLink.contactHeader);
                    sipLink.dialog.sendAck(request);
                    String remoteRtpAddress = RtpLink.audioUriFromAddress(c.getAddress(), sipLink.remoteRtpPort);

                    sipLink.ui.answeredCall();
                    sipLink.ui.addSentMessage(request.toString());
                    sipLink.startRtp(remoteRtpAddress);
                } else if (response.getStatusCode() == Response.RINGING) {
                    sipLink.ui.ringing();
                } else if (response.getStatusCode() == Response.DECLINE) {
                    sipLink.ui.cancelCall();
                }
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
