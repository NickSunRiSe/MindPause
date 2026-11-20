package app.mindpause.android.core;

import java.time.LocalDateTime;

public final class Schedule {
    private Schedule() {}
    public static boolean includes(LocalDateTime now, int startMinute, int endMinute, int dayMask) {
        int minute = now.getHour() * 60 + now.getMinute();
        // For overnight windows, the after-midnight portion belongs to the starting day.
        LocalDateTime anchor = startMinute > endMinute && minute < endMinute ? now.minusDays(1) : now;
        boolean day = (dayMask & (1 << (anchor.getDayOfWeek().getValue() - 1))) != 0;
        if (!day) return false;
        if (startMinute == endMinute) return true;
        return startMinute < endMinute ? minute >= startMinute && minute < endMinute
                : minute >= startMinute || minute < endMinute;
    }
}
