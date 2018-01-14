package ma.ac.emi.simplesoftphone.rtp;

import javax.media.*;
import java.io.IOException;

/**
 * Permet lire un stream audio à partir d'une uri
 */
public class RtpReceiver {
    /**
     * Uri à partir de laquelle on lit le stream audio
     */
    private String locator;
    /**
     * Medialocator correspondant à l'uri
     */
    private MediaLocator mediaLocator;
    /**
     * Lecteur audio
     */
    private Player player;

    private RtpReceiver(String locator) {
        this.locator = locator;
        this.mediaLocator = new MediaLocator(locator);
        try {
            player = Manager.createRealizedPlayer(mediaLocator);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoPlayerException e) {
            e.printStackTrace();
        } catch (CannotRealizeException e) {
            e.printStackTrace();
        }
        player.start();
    }

    /**
     * Crée un recepteur à partir d'une Uri
     * @param locator Uri à partir de laquelle on lit le stream audio
     * @return l'objet RtpReceiver
     */
    public static RtpReceiver from(String locator) {
        return new RtpReceiver(locator);
    }

    public void stop() {
        player.stop();
    }
}