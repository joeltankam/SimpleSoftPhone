package ma.ac.emi.simplesoftphone.sip;

import ma.ac.emi.simplesoftphone.ui.SipBasicPhone;

import javax.sip.*;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
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

        System.out.println(request.toString() + "\n\n");
        try {
            ServerTransaction transaction = requestEvent.getServerTransaction();
            if (null == transaction) {
                transaction = sipLink.sipProvider.getNewServerTransaction(request);
            }

            Response response;
            if (request.getMethod().equals("INVITE")) {

                response = sipLink.messageFactory.createResponse(200, request);
                ((ToHeader) response.getHeader("To")).setTag(String.valueOf(sipLink.tag));
                response.addHeader(sipLink.contactHeader);
                transaction.sendResponse(response);

            } else if (request.getMethod().equals("BYE")) {

                response = sipLink.messageFactory.createResponse(200, request);
                ((ToHeader) response.getHeader("To")).setTag(String.valueOf(sipLink.tag));
                response.addHeader(sipLink.contactHeader);
                transaction.sendResponse(response);
                System.out.println(response.toString() + "\n\n");
            } else if (request.getMethod().equals("ACK")) {

            }
        } catch (Exception ex) {
            Logger.getLogger(SipBasicPhone.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();

        System.out.println(response.toString() + "\n\n");
        Request request;

        try {
            CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

            if (cseq.getMethod().equals(Request.INVITE)) {
                request = sipLink.dialog.createAck(cseq.getSeqNumber());

                sipLink.contactHeader = sipLink.headerFactory.createContactHeader(sipLink.contactAddress);
                request.addHeader(sipLink.contactHeader);
                sipLink.dialog.sendAck(request);
                System.out.println(request.toString() + "\n\n");
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
