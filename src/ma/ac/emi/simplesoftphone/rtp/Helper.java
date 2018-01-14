package ma.ac.emi.simplesoftphone.rtp;

public class Helper {
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

    public static void link(String fromLocator, String toLocator){
        Transmitter.to(toLocator);
        Receiver.from(fromLocator);
    }
}
