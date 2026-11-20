package app.mindpause.android.core;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static app.mindpause.android.core.ForegroundWindows.*;

public class ForegroundWindowsTest {
    private Window app(boolean active,boolean focused){return new Window(APPLICATION,1,active,focused,false);}
    private Window system(boolean focused){return new Window(SYSTEM,10,true,focused,false);}
    @Test public void unfocusedBannerDoesNotHideFocusedApp(){
        assertEquals(1,select(Arrays.asList(system(false),app(false,true))));
        assertEquals(0,select(Arrays.asList(app(true,true),system(false))));
    }
    @Test public void focusedShadeOrSystemDialogPausesCounting(){
        assertEquals(-1,select(Arrays.asList(app(true,true),system(true))));
        assertEquals(-1,select(Arrays.asList(system(true),app(true,false))));
    }
    @Test public void unknownWindowsNeverReuseLastApp(){
        assertEquals(-1,select(Collections.emptyList()));
        assertEquals(-1,select(Arrays.asList(app(false,false),system(false))));
    }
    @Test public void touchedSplitScreenAppWinsAndPictureInPictureIsIgnored(){
        assertEquals(1,select(Arrays.asList(app(false,true),app(true,false))));
        assertEquals(-1,select(Collections.singletonList(new Window(APPLICATION,10,true,true,true))));
    }
    @Test public void repeatedBannersDoNotExtendOneMinute(){
        UsageClock clock=new UsageClock();long used=0;
        for(long now=0;now<=60000;now+=500){
            List<Window> windows=Arrays.asList(app(false,true),new Window(SYSTEM,10,now%5000<3500,false,false));
            int selected=select(windows);
            used+=clock.sample(selected==0?"feed":null,selected==0,now);
        }
        assertEquals(60000,used);
    }
    @Test public void openingShadePausesWithoutLosingAccumulatedTime(){
        UsageClock clock=new UsageClock();long used=0;
        for(long now=0;now<=20000;now+=500)used+=clock.sample("feed",true,now);
        for(long now=20500;now<=30000;now+=500){int i=select(Arrays.asList(app(false,true),system(true)));used+=clock.sample(null,i>=0,now);}
        assertEquals(20000,used);
        for(long now=30500;now<=70500;now+=500)used+=clock.sample("feed",true,now);
        assertEquals(60000,used);
    }
}
