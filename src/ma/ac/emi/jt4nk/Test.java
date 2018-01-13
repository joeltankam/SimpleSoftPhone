package ma.ac.emi.jt4nk;

import ma.ac.emi.jt4nk.rtp.Receiver;
import ma.ac.emi.jt4nk.rtp.Transmitter;

public class Test {
    public static void main(String args[]){
        String locator = "rtp://192.168.43.45:10000/audio";
        new Transmitter(locator);
        new Receiver(locator);
    }
}
