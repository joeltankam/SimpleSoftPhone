package ma.ac.emi.simplesoftphone.sip;

import ma.ac.emi.simplesoftphone.ui.SipServer;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.swing.*;
import java.net.InetAddress;
import java.util.Properties;
import java.util.Random;

public class SipListenerServer implements SipListener {

    private SipFactory sipFactory;
    private SipStack sipStack;
    private SipProvider sipProvider;
    private MessageFactory messageFactory;
    private HeaderFactory headerFactory;
    private AddressFactory addressFactory;
    private ListeningPoint listeningPoint;
    private Properties properties;

    private String ip;
    private int port = 5060;
    private String protocol = "udp";
    private int tag = (new Random()).nextInt();
    private Address contactAddress;
    private ContactHeader contactHeader;


    private SipServer ui;

    public SipListenerServer(SipServer ui) {
        this.ui = ui;
        init();
    }

    public void init() {
        try {
            this.ip = InetAddress.getLocalHost().getHostAddress();
            this.sipFactory = SipFactory.getInstance();
            this.sipFactory.setPathName("gov.nist");
            this.properties = new Properties();
            this.properties.setProperty("javax.sip.STACK_NAME", "stack");
            this.sipStack = this.sipFactory.createSipStack(this.properties);
            this.messageFactory = this.sipFactory.createMessageFactory();
            this.headerFactory = this.sipFactory.createHeaderFactory();
            this.addressFactory = this.sipFactory.createAddressFactory();
            this.listeningPoint = this.sipStack.createListeningPoint(this.ip, this.port, this.protocol);
            this.sipProvider = this.sipStack.createSipProvider(this.listeningPoint);
            this.sipProvider.addSipListener(this);

            this.contactAddress = this.addressFactory.createAddress("sip:" + this.ip + ":" + this.port);
            this.contactHeader = this.headerFactory.createContactHeader(contactAddress);

            ui.jTextArea.append("Local address: " + this.ip + ":" + this.port + "\n");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ui, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void forward(Request request) {
        Request newRequest = (Request) request.clone();

        try {
            sipProvider.sendRequest(newRequest);
            System.out.println("newRequest = " + newRequest);
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        // Get the request.
        Request request = requestEvent.getRequest();

        ui.jTextArea.append("\nRECV " + request.getMethod() + " " + request.getRequestURI().toString());

        try {
            // Get or create the server transaction.
            ServerTransaction transaction = requestEvent.getServerTransaction();
            if (null == transaction) {
                if (!request.getMethod().equals(Request.ACK))
                    transaction = this.sipProvider.getNewServerTransaction(request);
            }

            // Update the SIP message table.
            if (!request.getMethod().equals(Request.ACK))
                ui.updateTable(requestEvent, request, transaction);

            ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
            if (!toHeader.getAddress().equals(contactAddress)) {
                forward(request);
            } else {
                // Process the request and send a response.
                Response response;
                if (request.getMethod().equals("REGISTER")) {
                    // If the request is a REGISTER.
                    response = this.messageFactory.createResponse(Response.OK, request);
                    ((ToHeader) response.getHeader("To")).setTag(String.valueOf(this.tag));
                    response.addHeader(this.contactHeader);
                    transaction.sendResponse(response);
                    ui.jTextArea.append(" / SENT " + response.getStatusCode() + " " + response.getReasonPhrase());
                }
            }
        } catch (SipException e) {
            e.printStackTrace();
            ui.jTextArea.append("\nERROR (SIP): " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            ui.jTextArea.append("\nERROR: " + e.getMessage());
        }
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
