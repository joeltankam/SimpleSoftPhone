package ma.ac.emi.simplesoftphone.rtp;

public class RtpLink {
    private RtpTransmitter transmitter;
    private RtpReceiver receiver;

    private RtpLink(RtpTransmitter transmitter, RtpReceiver receiver) {
        this.transmitter = transmitter;
        this.receiver = receiver;
    }

    private static String uriFromAddress(String address) {
        return "rtp://" + address;
    }

    public static String audioUriFromAddress(String address){
        return "rtp://" + address + "/audio";
    }

    public static String uriFromAddress(String ip, String port){
        return uriFromAddress(ip + ":" + port);
    }

    public static String uriFromAddress(String ip, int port) {
        return uriFromAddress(ip + ":" + port);
    }

    public static String audioUriFromAddress(String ip, String port){
        return audioUriFromAddress(ip + ":" + port);
    }

    public static String audioUriFromAddress(String ip, int port) {
        return audioUriFromAddress(ip + ":" + port);
    }

    public static RtpLink start(String fromLocator, String toLocator) {
        return new RtpLink(RtpTransmitter.to(toLocator), RtpReceiver.from(fromLocator));
    }

    public void stop() {
        receiver.stop();
        transmitter.stop();
    }
}
