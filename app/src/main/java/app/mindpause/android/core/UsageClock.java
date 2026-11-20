package app.mindpause.android.core;

/** Monotonic, conservative foreground accounting. Never counts sleep or missed polls. */
public final class UsageClock {
    private String previous;
    private long last = -1;
    public long sample(String foreground, boolean allowed, long elapsedRealtime) {
        long delta = last < 0 ? 0 : elapsedRealtime - last;
        boolean continuous = allowed && foreground != null && foreground.equals(previous);
        previous = allowed ? foreground : null;
        last = elapsedRealtime;
        return continuous && delta > 0 && delta <= 2500 ? delta : 0;
    }
}
