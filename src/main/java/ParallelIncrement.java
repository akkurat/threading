import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParallelIncrement {

    public static final int THREADS = 10;
    public static final int CHUNKS = (int)1e7;

    public static void main(String[] args) throws InterruptedException {
        ParallelIncrement main = new ParallelIncrement();
        Timer timer = new Timer();

        timer.reset();
        List<Integer> seqIdsNaive = main.threading( Incrementor::new, false );
        printResultPartially(seqIdsNaive);
        timer.printTimeAndReset( "Naive List");

        List<Integer> seqIds = main.threading( Incrementor::new, true );
        printResultPartially(seqIds);
        timer.printTimeAndReset( "List Sync");

        List<Integer> seqIdsDoubleSynced = main.threading( SyncedIncrementor::new, true );
        printResultPartially(seqIdsDoubleSynced);
        timer.printTimeAndReset( "List+ReadWrite Sync");

        List<Integer> seqIdsSynced = main.threading( SyncedIncrementor::new, false );
        printResultPartially(seqIdsSynced);
        timer.printTimeAndReset( "ReadWrite Sync");
    }

    private static void printResultPartially(List<Integer> seqIds) {
        System.out.println(seqIds.stream().skip(THREADS*10).limit(100).map(String::valueOf).collect(Collectors.joining(",")) );
    }

    private List<Integer> threading(Function<List, Runnable> runnableFactory, boolean syncList) throws InterruptedException {
        List<Integer> seqIds = new ArrayList<>(THREADS* CHUNKS);
        if( syncList )
            seqIds = Collections.synchronizedList(seqIds);
        ArrayList<Thread> threads = new ArrayList<>();
        for(int i = 0; i< THREADS; i++)
        {
           threads.add(new Thread(runnableFactory.apply(seqIds)));
        }
        threads.forEach( t -> t.start() );

        for( Thread t: threads)
        {
            t.join();
        }
        return seqIds;
    }
}

class Incrementor implements Runnable
{

    private volatile List<Integer> integers;

    public Incrementor(List<Integer> integers) {
        this.integers = integers;
    }

    @Override
    public void run() {
        for(int i = 0; i< ParallelIncrement.CHUNKS; i++)
        {
            int value;

            if( integers.isEmpty() ) {
                value = 0;
            }
            else {
                value = integers.get(integers.size()-1) + 1;
            }
            integers.add( value );
        }

    }
}
class SyncedIncrementor implements Runnable {

    private final List<Integer> integers;

    public SyncedIncrementor(List<Integer> integers) {
        this.integers = integers;
    }

    @Override
    public void run() {
        for (int i = 0; i < ParallelIncrement.CHUNKS; i++) {
            int value;
            synchronized (integers) {
                if (integers.isEmpty()) {
                    value = 0;
                } else {
                    value = integers.get(integers.size()-1) + 1;
                }
                integers.add(value);
            }
        }

    }
}
