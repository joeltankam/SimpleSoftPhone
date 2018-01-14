package ma.ac.emi.simplesoftphone.sip;

import ma.ac.emi.simplesoftphone.rtp.RtpLink;
import ma.ac.emi.simplesoftphone.ui.SipBasicPhone;

import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.*;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
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
            ServerTransaction transaction = requestEvent.getServerTransaction();
            if (null == transaction) {
                transaction = sipLink.sipProvider.getNewServerTransaction(request);
            }

            Response response;
            ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);

            if (request.getMethod().equals("INVITE")) {
                byte[] rawContent = request.getRawContent();
                String sdpContent = new String(rawContent, "UTF-8");

                SdpFactory sdpFactory = SdpFactory.getInstance();
                SessionDescription sessionDescription = sdpFactory.createSessionDescription(sdpContent);

                Vector mediaDescs = sessionDescription.getMediaDescriptions(false);

                MediaDescription am = (MediaDescription) mediaDescs.get(0);

                sipLink.remoteRtpPort = am.getMedia().getMediaPort();

                response = sipLink.messageFactory.createResponse(200, request);

                ((ToHeader) response.getHeader("To")).setTag(String.valueOf(sipLink.tag));
                response.addHeader(sipLink.contactHeader);

                ContentTypeHeader contentTypeHeader
                        = sipLink.headerFactory.createContentTypeHeader("application", "sdp");

                String sdpData = sipLink.createSDPData(sipLink.rtpPort, 0); //create SDP content
                response.setContent(sdpData, contentTypeHeader);

                transaction.sendResponse(response);

                String remoteRtpAdress = RtpLink.audioUriFromAddress(viaHeader.getHost(), sipLink.remoteRtpPort);
                sipLink.startRtp(remoteRtpAdress);

                sipLink.ui.addSentMessage(response.toString());

            } else if (request.getMethod().equals("BYE")) {
                response = sipLink.messageFactory.createResponse(200, request);
                ((ToHeader) response.getHeader("To")).setTag(String.valueOf(sipLink.tag));
                response.addHeader(sipLink.contactHeader);
                transaction.sendResponse(response);

                sipLink.ui.addSentMessage(response.toString());
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


                byte[] rawContent = response.getRawContent();
                String sdpContent = new String(rawContent, "UTF-8");

                SdpFactory sdpFactory = SdpFactory.getInstance();
                SessionDescription sessionDescription = sdpFactory.createSessionDescription(sdpContent);

                Vector mediaDescs = sessionDescription.getMediaDescriptions(false);

                MediaDescription am = (MediaDescription) mediaDescs.get(0);

                sipLink.remoteRtpPort = am.getMedia().getMediaPort();

                request = sipLink.dialog.createAck(cseq.getSeqNumber());

                sipLink.contactHeader = sipLink.headerFactory.createContactHeader(sipLink.contactAddress);
                request.addHeader(sipLink.contactHeader);
                sipLink.dialog.sendAck(request);

                sipLink.ui.addSentMessage(request.toString());

                String remoteRtpAdress = RtpLink.audioUriFromAddress(viaHeader.getHost(), sipLink.remoteRtpPort);
                sipLink.startRtp(remoteRtpAdress);
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
