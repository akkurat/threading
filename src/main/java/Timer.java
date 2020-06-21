public class Timer {
    long start;

    public Timer()
    {
        this.start = System.currentTimeMillis();
    }

    public void printTimeAndReset(String message)
    {
        System.out.println( System.currentTimeMillis()-start + "ms " + message);
        reset();
    }

    public void reset()
    {
        this.start = System.currentTimeMillis();
    }


}
