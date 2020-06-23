import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

public class ParallelExcecution {

    public static final int NUM = 320;
    public static final int THREADS = Runtime.getRuntime().availableProcessors()-1;

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        long[] randArray = new Random().longs(NUM, (long)1e9, (long)1e16 )
                .toArray();

        Timer timer = new Timer();
        ArrayList<Long>[] results = new ArrayList[NUM];

        List<Primer> primers = new ArrayList<>();
        int inc = randArray.length / THREADS;
        for (int i = 0; i < THREADS; i++) {
            primers.add(new Primer(randArray, results,
                    i * inc, Math.min((i + 1) * inc, randArray.length)));
        }

        List<Thread> threads = primers.stream().map(Thread::new).collect(Collectors.toList());

        threads.forEach(Thread::start);

        for (Thread thread : threads) {
            thread.join();
        }

        timer.printTimeAndReset("parallel");
        printPartial(results);

        timer.reset();

        ArrayList<Long>[] expected = Arrays.stream(randArray)
                .mapToObj(ParallelExcecution::primeFactors)
                .toArray(ArrayList[]::new);

        timer.printTimeAndReset("Stream");
        printPartial(expected);

        timer.reset();
        ForkJoinPool pool = new ForkJoinPool(THREADS);

        ArrayList < Long >[] expectedpar = pool.submit(() ->  Arrays.stream(randArray)
                .parallel()
                .mapToObj(ParallelExcecution::primeFactors)
                .toArray(ArrayList[]::new) ).get();

        timer.printTimeAndReset("Stream Parallel");
        printPartial(expectedpar);

    }

    private static void printPartial(ArrayList<Long>[] result ) {
        ArrayList<Long>[] toPrint = Arrays.copyOfRange(result, result.length - 20, result.length);
        System.out.println(Arrays.toString( toPrint ));
    }


    // https://www.geeksforgeeks.org/java-program-for-efficiently-print-all-prime-factors-of-a-given-number/
    public static ArrayList<Long> primeFactors(long n) {

        ArrayList<Long> out = new ArrayList<>();
        // Print the number of 2s that divide n
        while (n % 2 == 0) {
//            System.out.print(2 + " ");
            out.add(2L);
            n /= 2;
        }

        // n must be odd at this point.  So we can
        // skip one element (Note i = i +2)
        for (long i = 3; i <= Math.sqrt(n); i += 2) {
            // While i divides n, print i and divide n
            while (n % i == 0) {
//                System.out.print(i + " ");
                out.add(i);
                n /= i;
            }
        }

        // This condition is to handle the case whien
        // n is a prime number greater than 2
        if (n > 2)
//            System.out.print(n);
            out.add(n);

        return out;
    }

}


class Primer implements Runnable {

    private final long[] input;
    private final ArrayList<Long>[] output;
    private final int startIdx;
    private final int nextStartIdx;

    public Primer(long[] input, ArrayList<Long>[] output, int startIdx, int nextStartIdx) {
        this.input = input;
        this.output = output;
        this.startIdx = startIdx;
        this.nextStartIdx = nextStartIdx;
    }

    @Override
    public void run() {

        for (int i = startIdx; i < nextStartIdx; i++) {
            output[i] = ParallelExcecution.primeFactors(input[i]);
        }
    }
}

