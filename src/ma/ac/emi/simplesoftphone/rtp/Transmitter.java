package ma.ac.emi.simplesoftphone.rtp;

import javax.media.*;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import java.io.IOException;

/**
 * Permet de transmettre un stream audio pour lire un stream audio à partir d'une uri
 */
public class Transmitter {
    /**
     * Uri à laquelle on transmet le stream audio
     */
    private String locator;

    MediaLocator mediaLocator;

    DataSource source;

    Processor mediaProcessor;

    MediaLocator outputMediaLocator;
    DataSink dataSink;

    /**
     * Formats de transmission
     */
    private static final Format[] FORMATS = new Format[]{new AudioFormat(AudioFormat.ULAW_RTP)};
    private static final ContentDescriptor CONTENT_DESCRIPTOR = new ContentDescriptor(ContentDescriptor.RAW_RTP);

    private Transmitter(String locator) {
        this.locator = locator;
        mediaLocator = new MediaLocator("javasound://0");

        source = null;
        try {
            source = Manager.createDataSource(mediaLocator);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoDataSourceException e) {
            e.printStackTrace();
        }

        mediaProcessor = null;
        try {
            mediaProcessor = Manager.createRealizedProcessor(new ProcessorModel(source, FORMATS, CONTENT_DESCRIPTOR));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoProcessorException e) {
            e.printStackTrace();
        } catch (CannotRealizeException e) {
            e.printStackTrace();
        }

        outputMediaLocator = new MediaLocator(locator);
        dataSink = null;
        try {
            dataSink = Manager.createDataSink(mediaProcessor.getDataOutput(), outputMediaLocator);
        } catch (NoDataSinkException e) {
            e.printStackTrace();
        }

        mediaProcessor.start();

        try {
            dataSink.open();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            dataSink.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Transmiting...");
    }

    /**
     * Crée une transmission vers une uri à partir du microphone
     *
     * @param locator Uri à laquelle on transmet le stream audio
     * @return l'objet Transmitter
     */
    public static Transmitter to(String locator) {
        return new Transmitter(locator);
    }

    public void stop() {
        try {
            dataSink.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaProcessor.stop();
    }
}
