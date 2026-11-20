package app.mindpause.android.core;
import java.time.LocalDateTime;
import org.junit.Test;
import static org.junit.Assert.*;
public class ScheduleTest {
    @Test public void includesStartExcludesEnd(){
        assertTrue(Schedule.includes(LocalDateTime.parse("2026-10-26T09:00"),540,1380,127));
        assertFalse(Schedule.includes(LocalDateTime.parse("2026-10-26T23:00"),540,1380,127));
        assertFalse(Schedule.includes(LocalDateTime.parse("2026-10-26T08:59"),540,1380,127));
    }
    @Test public void overnightBelongsToStartDay(){
        assertTrue(Schedule.includes(LocalDateTime.parse("2026-10-26T23:30"),1320,120,1));
        assertTrue(Schedule.includes(LocalDateTime.parse("2026-10-27T01:59"),1320,120,1));
        assertFalse(Schedule.includes(LocalDateTime.parse("2026-10-27T02:00"),1320,120,1));
        assertFalse(Schedule.includes(LocalDateTime.parse("2026-10-27T23:00"),1320,120,1));
    }
    @Test public void allDayStillHonorsWeekday(){
        assertTrue(Schedule.includes(LocalDateTime.parse("2026-10-26T00:00"),0,0,1));
        assertFalse(Schedule.includes(LocalDateTime.parse("2026-10-27T12:00"),0,0,1));
        assertFalse(Schedule.includes(LocalDateTime.parse("2026-10-26T12:00"),0,0,0));
    }
}
