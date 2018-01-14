package ma.ac.emi.simplesoftphone;

import ma.ac.emi.simplesoftphone.rtp.Receiver;
import ma.ac.emi.simplesoftphone.rtp.Transmitter;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class RTPTest {
    public static void main(String args[]) {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            String locator = "rtp://" + ip + ":10000/audio";
            Transmitter.to(locator);
            Receiver.from(locator);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
