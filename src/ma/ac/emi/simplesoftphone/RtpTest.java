package ma.ac.emi.simplesoftphone;

import ma.ac.emi.simplesoftphone.rtp.RtpReceiver;
import ma.ac.emi.simplesoftphone.rtp.RtpTransmitter;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class RtpTest {
    public static void main(String args[]) {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            String locator = "rtp://" + ip + ":10000/audio";
            RtpTransmitter.to(locator);
            RtpReceiver.from(locator);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
