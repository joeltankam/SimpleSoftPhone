package ma.ac.emi.simplesoftphone;

import ma.ac.emi.simplesoftphone.rtp.Receiver;
import ma.ac.emi.simplesoftphone.rtp.Transmitter;

public class RTPTest {
    public static void main(String args[]){
        String locator = "rtp://192.168.43.45:10000/audio";
        Transmitter.to(locator);
        Receiver.from(locator);
    }
}
