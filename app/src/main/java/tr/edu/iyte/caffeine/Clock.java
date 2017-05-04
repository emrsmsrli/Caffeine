package tr.edu.iyte.caffeine;

public class Clock {
    private int min;
    private int sec;

    public void set(int min, int sec) {
        this.min = min;
        this.sec = sec;
    }

    public int getMin() {
        return min;
    }

    public int getSec() {
        return sec;
    }
}
