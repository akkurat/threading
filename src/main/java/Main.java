import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Main main = new Main();
        Timer timer = new Timer();
        ConcurrentLinkedDeque<Integer> seqIds = main.threading( Incrementor::new );
        printResultPartially(seqIds);
        timer.printTimeAndReset( "No Sync");

        ConcurrentLinkedDeque<Integer> seqIdsSynced = main.threading( SyncedIncrementor::new );
        printResultPartially(seqIdsSynced);
        timer.printTimeAndReset( "Synced");
    }

    private static void printResultPartially(ConcurrentLinkedDeque<Integer> seqIds) {
        System.out.println(seqIds.stream().skip(20).limit(20).map(String::valueOf).collect(Collectors.joining(",")) );
    }

    private ConcurrentLinkedDeque<Integer> threading(Function<ConcurrentLinkedDeque, Runnable> runnableFactory) throws InterruptedException {
        ConcurrentLinkedDeque<Integer> seqIds = new ConcurrentLinkedDeque<>();
        ArrayList<Thread> threads = new ArrayList<>();
        for(int i=0; i<100; i++)
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

    private transient final Deque<Integer> integers;

    public Incrementor(Deque<Integer> integers) {
        this.integers = integers;
    }

    @Override
    public void run() {
        for( int i=0; i<100000; i++)
        {
            int value;

            if( integers.isEmpty() ) {
                value = 0;
            }
            else {
                value = integers.getLast() + 1;
            }
            integers.add( value );
        }

    }
}
class SyncedIncrementor implements Runnable {

    private transient final Deque<Integer> integers;

    public SyncedIncrementor(Deque<Integer> integers) {
        this.integers = integers;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; i++) {
            int value;
            synchronized (integers) {
                if (integers.isEmpty()) {
                    value = 0;
                } else {
                    value = integers.getLast() + 1;
                }
                integers.add(value);
            }
        }

    }
}
