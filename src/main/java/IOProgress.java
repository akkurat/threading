import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IOProgress {
    public static void main(String[] args) throws IOException {
        IOProgress ioProgress = new IOProgress();
        ioProgress.start();
    }

    private void start() throws IOException {
        Random random = new Random();

        long fs_gb = 1024L * 1024 * 2342;

        LinkedBlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
        InputStream is = new RandomInputStream(messageQueue, fs_gb );


        File file = File.createTempFile("test", "bas" );
        file.deleteOnExit();

        System.out.println( file.toPath().toAbsolutePath());


        Thread copyThread = new Thread(() -> {
            try {
                Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        MessageOutputer messageOutputer = new MessageOutputer(messageQueue);
        Thread messageThread = new Thread(messageOutputer);

        copyThread.start();
        messageThread.start();

        try {
            // Wait for copy thread to finish
            copyThread.join();

            messageOutputer.stop();
            messageThread.join();

            System.out.println();
            System.out.println( file.length() / 1024 / 1024 + "MB" );
        }
        catch (InterruptedException e) {
            System.exit(1);
        }

        System.exit(0);

    }

}

class MessageOutputer implements Runnable
{
    private final Queue<String> queue;

    public void stop() {
        this.done = true;
    }

    private boolean done;

    public MessageOutputer(Queue<String> queue ) {
        this.queue = queue;
        this.done = false;
    }



    @Override
    public void run() {

        while( !done )
        {
            List<String> buffer = new ArrayList <>();
            while( !queue.isEmpty() )
            {
                buffer.add( queue.poll() );
            }
//            System.out.println(String.join(" | ", buffer));
            if( !buffer.isEmpty() ) {
                String s = buffer.get(buffer.size() - 1);
                System.out.print("\r"+s);
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}

class RandomInputStream extends InputStream {

    public static final double PROGRESS_MSG = 0.001;
    public static final int PGR_BARS = 35;
    private final LinkedBlockingQueue<String> messageQueue;
    private final long virtualFileSize;
    private double lastFraction;
    private long read;
    private Random generator = new Random();
    private boolean closed = false;

    public RandomInputStream(LinkedBlockingQueue<String> consumer, long virtualFileSize ) {
        this.messageQueue = consumer;
        this.virtualFileSize = virtualFileSize;

        this.lastFraction = 0.0;
        this.read = 0L;
    }

    @Override
    public int read() throws IOException {
        if(completeRead())
        {
            return -1;
        }
        checkOpen();
        int result = generator.nextInt() % 256;
        if (result < 0) {
            result = -result;
        }
        // Reads only one Byte
        increaseRead(1);
        return result;
    }

    private void increaseRead(int numBytes) {
        read += numBytes;
        double newPercent = (double) read / virtualFileSize;
        if( newPercent- lastFraction > PROGRESS_MSG || newPercent > 0.999 ) {
            lastFraction = newPercent;
            String pgrsbar = IntStream.rangeClosed(1, PGR_BARS)
                    .mapToObj(i -> lastFraction * PGR_BARS >= i ? "#" : "_")
                    .collect(Collectors.joining());
            messageQueue.add( String.format("%s  %.1f%% / %.1f MB",pgrsbar, (lastFraction *100), read/1024.0/1024) );
        }
    }


    @Override
    public int read(byte[] data, int offset, int length) throws IOException {
        if(completeRead())
        {
            return -1;
        }
        checkOpen();
        byte[] temp = new byte[length];
        getData(temp);
        System.arraycopy(temp, 0, data, offset, length);
        increaseRead( length );
        return length;

    }

    @Override
    public int read(byte[] data) throws IOException {
        if(completeRead())
        {
            return -1;
        }
        checkOpen();
        getData(data);
        increaseRead( data.length );
        return data.length;

    }


    private void getData(byte[] data) {
        generator.nextBytes(data);
    }

    @Override
    public long skip(long bytesToSkip) throws IOException {
        checkOpen();
        // It's all random so skipping has no effect.
        return bytesToSkip;
    }

    @Override
    public void close() {
        this.closed = true;
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("Input stream closed");
        }
    }

    @Override
    public int available() {
        // Limited only by available memory and the size of an array.
        return 1_024 * 1_024;
    }

    private boolean completeRead() {
        return read>= virtualFileSize;
    }
}
