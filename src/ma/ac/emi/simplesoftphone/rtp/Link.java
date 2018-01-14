package ma.ac.emi.simplesoftphone.rtp;

public class Link {
    Transmitter transmitter;
    Receiver receiver;

    private Link(String fromLocator, String toLocator) {
        new Link(Transmitter.to(toLocator), Receiver.from(fromLocator));
    }

    public Link(Transmitter transmitter, Receiver receiver) {
        this.transmitter = transmitter;
        this.receiver = receiver;
    }

    public static String uriFromAddress(String address){
        return "rtp://" + address;
    }

    public static String audioUriFromAddress(String address){
        return "rtp://" + address + "/audio";
    }

    public static String uriFromAddress(String ip, String port){
        return uriFromAddress(ip + ":" + port);
    }

    public static String audioUriFromAddress(String ip, String port){
        return audioUriFromAddress(ip + ":" + port);
    }

    public static Link start(String fromLocator, String toLocator) {
        return new Link(Transmitter.to(toLocator), Receiver.from(fromLocator));
    }

    public void stop() {
        receiver.stop();
        transmitter.stop();
    }
}
