package ma.ac.emi.simplesoftphone.sip;

import ma.ac.emi.simplesoftphone.ui.SipBasicPhon;

import javax.sdp.SdpFactory;
import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.TooManyListenersException;

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


    private String ip; // Adresse IP locale
    private int sipPort = 6000; // Port local.
    private String protocol = "udp"; // Protocole local de transport (UDP).


    int tag = (new Random()).nextInt(); // Le tag local.
    Address contactAddress; // L’adresse de contact.
    ContactHeader contactHeader; // L’entête contact.
    private static String transport;
    public static SdpFactory sdpFactory; //pour le corps du message SIP (SDP)

    Dialog dialog; // Dialogue Client

    SipListenerImpl sipListener;

    SipBasicPhon ui;

    public SipLink(String ip, int sipPort, int rtpPort, SipBasicPhon ui) throws InvalidArgumentException {
        this.sipPort = sipPort;
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

    public void call(String address) {
        try {
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
            ViaHeader viaHeader = this.headerFactory.createViaHeader(this.ip, this.sipPort,
                    transport, null);
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
            Request request = messageFactory.createRequest(requestURI, Request.INVITE,
                    callIdHeader, cSeqHeader, fromHeader,
                    toHeader, viaHeaders, maxForwards);

            // Ajouter l’adresse de contacte.
            //contactAddress.setDisplayName("Alice Contact");
            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // Créer la transaction client.
            ClientTransaction inviteTid = this.sipProvider.getNewClientTransaction(request);
            // envoyer la requête
            inviteTid.sendRequest();

            dialog = inviteTid.getDialog();
            System.out.println(request.toString() + "\n\n");
        } catch (Exception e) {
        }
    }

    public void end(String address) {
        try {
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
            ViaHeader viaHeader = this.headerFactory.createViaHeader(this.ip, this.sipPort,
                    transport, null);
            // ajouter les via headers
            viaHeaders.add(viaHeader);
            // Créer le ContentTypeHeader
            ContentTypeHeader contentTypeHeader
                    = headerFactory.createContentTypeHeader("application", "sdp");
            // Créer une nouvelle entête CallId
            CallIdHeader callIdHeader = sipProvider.getNewCallId();
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
            ClientTransaction inviteTid = this.sipProvider.getNewClientTransaction(request);
            // envoyer la requête
            inviteTid.sendRequest();

            dialog = inviteTid.getDialog();
            // Afficher le message dans le text area.
            System.out.println(request.toString() + "\n\n");
        } catch (Exception e) {
            //Afficher l’erreur en cas de problème.
            System.out.println("Failed: " + e.getMessage() + "\n");
        }
    }
}
