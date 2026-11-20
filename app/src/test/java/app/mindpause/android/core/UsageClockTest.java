package app.mindpause.android.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class UsageClockTest {
    @Test public void countsOnlyConsecutiveForegroundSamples() {
        UsageClock clock=new UsageClock();
        assertEquals(0,clock.sample("tiktok",true,1000));
        assertEquals(500,clock.sample("tiktok",true,1500));
        assertEquals(0,clock.sample("other",true,2000));
        assertEquals(0,clock.sample("tiktok",true,2500));
        assertEquals(500,clock.sample("tiktok",true,3000));
    }
    @Test public void screenLockAndDisabledScheduleBreakContinuity() {
        UsageClock clock=new UsageClock();clock.sample("a",true,0);
        assertEquals(0,clock.sample("a",false,500));
        assertEquals(0,clock.sample("a",true,1000));
        assertEquals(500,clock.sample("a",true,1500));
        assertEquals(0,clock.sample(null,true,2000));
    }
    @Test public void missedPollsAndClockReversalDoNotCount() {
        UsageClock clock=new UsageClock();clock.sample("a",true,1000);
        assertEquals(0,clock.sample("a",true,60000));
        assertEquals(500,clock.sample("a",true,60500));
        assertEquals(0,clock.sample("a",true,60000));
    }
}
