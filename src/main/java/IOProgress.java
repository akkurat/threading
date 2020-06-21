import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;

public class IOProgress {
    public static void main(String[] args) throws IOException {
        IOProgress ioProgress = new IOProgress();
        ioProgress.start();
    }

    private void start() throws IOException {
        Random random = new Random();

        long mb = 1024L * 1024;

        LinkedBlockingQueue<ProgressInfo> messageQueue = new LinkedBlockingQueue<>();


        List<FileCopyRunner> copyRunners = ImmutableList.of(
                new FileCopyRunner(messageQueue, 230*mb, 1),
        new FileCopyRunner(messageQueue, 2342*mb, 2),
        new FileCopyRunner(messageQueue, 4500*mb, 3)
        );



        MessageOutputer messageOutputer = new MessageOutputer(messageQueue, ImmutableList.of(1,2,3));
        Thread messageThread = new Thread(messageOutputer);

        List<Thread> copyThreads = copyRunners.stream().map(r -> new Thread(r)).collect(Collectors.toList());
        copyThreads.forEach( Thread::start );
        messageThread.start();

        try {
            // Wait for copy thread to finish
            copyThreads.forEach(thread -> {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            messageOutputer.stop();
            messageThread.join();

            System.out.println();
            copyRunners.forEach( f -> System.out.println( f.file.getAbsolutePath() + f.file.length() / 1024 / 1024 + "MB" ) );
        }
        catch (InterruptedException e) {
            System.exit(1);
        }

        System.exit(0);

    }

}

class FileCopyRunner implements Runnable {

    private final InputStream is;
    public final File file;

    public FileCopyRunner(LinkedBlockingQueue<ProgressInfo> messageQueue, long fs_gb, int virtualFileId) throws IOException {

        InputStream is = new RandomInputStream(messageQueue, fs_gb, virtualFileId);


        File file = File.createTempFile("test", "id"+virtualFileId );
        file.deleteOnExit();

        System.out.println( file.toPath().toAbsolutePath());

        this.is = is;
        this.file = file;
    }


    @Override
    public void run() {
        try {
            Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
class MessageOutputer implements Runnable
{
    private final LinkedBlockingQueue<ProgressInfo> queue;
    private final Map<Integer, String> fileIds;

    public void stop() {
        this.done = true;
    }

    private boolean done;

    public MessageOutputer(LinkedBlockingQueue<ProgressInfo> queue, List<Integer> fileIds ) {
        this.queue = queue;
        this.done = false;
        this.fileIds = IntStream.range(0, fileIds.size()).boxed()
                .collect(toMap(fileIds::get, i-> "", (s1,s2) -> s2, TreeMap::new));

    }

    @Override
    public void run() {

        while( !done )
        {
            List<ProgressInfo> buffer = new ArrayList<>();
            while( !queue.isEmpty() )
            {
                buffer.add( queue.poll() );
            }

            // Cursor Position is assumed to be in bottom
            for( ProgressInfo b: buffer)
            {
                fileIds.put(b.id, b.id+": "+b.asBar(34) );
            }

            System.out.print("\033[H\033[2J");

            System.out.print( String.join("\n", fileIds.values()));

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
    private final LinkedBlockingQueue<ProgressInfo> messageQueue;
    private final long virtualFileSize;
    private final int virtualFileId;
    private double lastFraction;
    private long read;
    private Random generator = new Random();
    private boolean closed = false;

    public RandomInputStream(LinkedBlockingQueue<ProgressInfo> consumer, long virtualFileSize, int virtualFileId ) {
        this.messageQueue = consumer;
        this.virtualFileSize = virtualFileSize;
        this.virtualFileId = virtualFileId;

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
            messageQueue.add(new ProgressInfo( virtualFileId, 
                    toMB(read), toMB(virtualFileSize), "MB") );
        }
    }

    private double toMB(long virtualFileSize) {
        return virtualFileSize/1024.0/1024;
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

class ProgressInfo {
    private final double value;
    private final double totalValue;
    private final String unit;
    public final int id;

    public ProgressInfo(int id, double value, double totalValue, String unit) {
        this.id = id;
        this.value = value;
        this.totalValue = totalValue;
        this.unit = unit;
    }

    public String asBar( int PGR_BARS ) {
        double lastFraction = value / totalValue;
        String pgrsbar = IntStream.rangeClosed(1, PGR_BARS)
                .mapToObj(i -> lastFraction * PGR_BARS >= i ? "#" : "_")
                .collect(Collectors.joining());
        return (String.format("%s %.1f%% / %.1f %s", pgrsbar, (lastFraction * 100), value, unit ) );
    }
}
