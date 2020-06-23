import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FindMax {
    private static final int THREADS = 2;

    public static void main(String[] args) throws InterruptedException {

        double[] randArray = new Random().doubles(200_000_000).toArray();


        Timer timer = new Timer();
        List<Maximizer> maximizerList = new ArrayList<>();
        int inc = randArray.length / THREADS;
        for(int i=0; i< THREADS; i++ )
        {
            maximizerList.add( new Maximizer( randArray, i*inc,
                    Math.min((i+1)*inc,randArray.length) ) );

        }

        List<Thread> threads = maximizerList.stream().map(Thread::new).collect(Collectors.toList());

        threads.forEach( Thread::start );

        for (Thread thread : threads) {
            thread.join();
        }




        Optional<Maximizer> max = maximizerList.stream().max(Comparator.comparing(maximizer -> randArray[maximizer.getMaxIdx()]));
        Optional<Maximizer> min = maximizerList.stream().min(Comparator.comparing(maximizer -> randArray[maximizer.getMinIdx()]));

        timer.printTimeAndReset(String.format("parallel min:%e max:%e",
                randArray[min.get().getMinIdx()], randArray[max.get().getMaxIdx()]) );

        Maximizer single = new Maximizer(randArray, 0, randArray.length);
        single.run();

        timer.printTimeAndReset( String.format("single  min:%e, max: %e ",
                randArray[single.getMinIdx()], randArray[single.getMaxIdx()]  ) );

        OptionalDouble smax = Arrays.stream(randArray).max();
        timer.printTimeAndReset( String.format("streams max %e",  smax.getAsDouble() ) );
        OptionalDouble smin = Arrays.stream(randArray).min();
        timer.printTimeAndReset( String.format("streams min %e",  smin.getAsDouble() ) );
        DoubleSummaryStatistics stats = Arrays.stream(randArray).summaryStatistics();
        timer.printTimeAndReset( String.format("streams min %s",  stats ) );
        System.exit(0);
    }


}

class Maximizer implements Runnable {

    private final double[] input;
    private final int startIdx;
    private final int nextStartIdx;
    private int maxIndex;
    private int minIndex;

    public Maximizer(double[] input, int startIdx, int nextStartIdx ) {
        this.input = input;
        this.startIdx = startIdx;
        this.nextStartIdx = nextStartIdx;
    }

    @Override
    public void run() {

        this.maxIndex = startIdx; this.minIndex = startIdx;

        for( int i=startIdx; i< nextStartIdx; i++ )
        {
            // The first value is kept
            if( input[i] > input[maxIndex] )
                maxIndex = i;
            if( input[i] < input[minIndex] )
                minIndex = i;
        }

    }

    public int getMaxIdx() {
        return maxIndex;
    }

    public int getMinIdx() {
        return minIndex;
    }


}






