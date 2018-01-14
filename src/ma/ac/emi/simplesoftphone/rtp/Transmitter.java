package ma.ac.emi.simplesoftphone.rtp;

import javax.media.*;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import java.io.IOException;

public class Transmitter {
    private String locator;
    //media formats needed to build RTP stream
    private static final Format[] FORMATS = new Format[] {new AudioFormat(AudioFormat.ULAW_RTP)}; //Ulaw_RTP
    private static final ContentDescriptor CONTENT_DESCRIPTOR =new ContentDescriptor(ContentDescriptor.RAW_RTP);

    private Transmitter(String locator){
        this.locator = locator;
        // media source = microphone
        MediaLocator mediaLocator = new MediaLocator("javasound://0");

        // creating a source that will be used in creating the processor
        DataSource source = null;
        try {
            source = Manager.createDataSource(mediaLocator);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoDataSourceException e) {
            e.printStackTrace();
        }

        //creating the processor form the source and formats that we want (RTP)
        Processor mediaProcessor = null;
        try {
            mediaProcessor = Manager.createRealizedProcessor( new ProcessorModel(source, FORMATS, CONTENT_DESCRIPTOR));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoProcessorException e) {
            e.printStackTrace();
        } catch (CannotRealizeException e) {
            e.printStackTrace();
        }

        // this is the output medialocator : ip, port and data type  //to
        MediaLocator outputMediaLocator = new MediaLocator(this.locator);

        // now , we are creating a datasink from the processor's output datasource and send it to output locator
        DataSink dataSink = null;
        try {
            dataSink = Manager.createDataSink(mediaProcessor.getDataOutput(),outputMediaLocator);
        } catch (NoDataSinkException e) {
            e.printStackTrace();
        }

        // start the processor
        mediaProcessor.start();

        //open connection
        try {
            dataSink.open();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //start streaming the RTP data
        try {
            dataSink.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Transmiting...");
    }

    public static Transmitter to(String locator){
        return new Transmitter(locator);
    }
}
