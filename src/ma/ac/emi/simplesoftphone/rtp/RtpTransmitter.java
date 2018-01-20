package ma.ac.emi.simplesoftphone.rtp;

import javax.media.*;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import java.io.IOException;

/**
 * Permet de transmettre un stream audio pour lire un stream audio à partir d'une uri
 */
public class RtpTransmitter {
    private Processor mediaProcessor;
    private DataSink dataSink;

    /**
     * Formats de transmission
     */
    private static final Format[] FORMATS = new Format[]{new AudioFormat(AudioFormat.ULAW_RTP)};
    private static final ContentDescriptor CONTENT_DESCRIPTOR = new ContentDescriptor(ContentDescriptor.RAW_RTP);

    private RtpTransmitter(String locator) {
        MediaLocator mediaLocator = new MediaLocator("javasound://0");

        DataSource source = null;
        try {
            source = Manager.createDataSource(mediaLocator);
        } catch (IOException | NoDataSourceException e) {
            e.printStackTrace();
        }

        mediaProcessor = null;
        try {
            mediaProcessor = Manager.createRealizedProcessor(new ProcessorModel(source, FORMATS, CONTENT_DESCRIPTOR));
        } catch (IOException | CannotRealizeException | NoProcessorException e) {
            e.printStackTrace();
        }

        MediaLocator outputMediaLocator = new MediaLocator(locator);
        dataSink = null;
        try {
            dataSink = Manager.createDataSink(mediaProcessor.getDataOutput(), outputMediaLocator);
        } catch (NoDataSinkException e) {
            e.printStackTrace();
        }

        mediaProcessor.start();

        try {
            dataSink.open();
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
     * @return l'objet RtpTransmitter
     */
    public static RtpTransmitter to(String locator) {
        return new RtpTransmitter(locator);
    }

    public void stop() {
        try {
            dataSink.stop();
            dataSink.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaProcessor.stop();
    }
}
