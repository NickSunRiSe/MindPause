package app.mindpause.android;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import app.mindpause.android.core.UsageClock;
import app.mindpause.android.core.ForegroundWindows;
import java.util.ArrayList;
import java.util.List;

public final class FocusService extends AccessibilityService {
    static volatile boolean connected;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final UsageClock clock=new UsageClock();
    private Store store;
    private long lastLaunch;
    private final Runnable tick=new Runnable() {
        @Override public void run() { sample(); handler.postDelayed(this,500); }
    };
    @Override protected void onServiceConnected() {
        super.onServiceConnected(); store=new Store(this); connected=true;
        handler.removeCallbacks(tick); handler.post(tick);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // Sample on the fixed 500 ms tick. Window-change callbacks can arrive while
        // Android is still updating the window list; a burst must not break the clock.
        // Do not inspect event text, descriptions, or child nodes.
    }
    private void sample() {
        long now=SystemClock.elapsedRealtime();
        PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
        KeyguardManager keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        boolean allowed=store.enabled() && power.isInteractive() && !keyguard.isKeyguardLocked();
        String pkg=allowed ? foregroundPackage() : null;
        allowed=allowed && pkg!=null && store.apps().contains(pkg);
        long delta=clock.sample(pkg,allowed,now);
        if(!allowed) return;
        if(delta>0)store.viewing.add(pkg,delta,java.time.ZonedDateTime.now());
        boolean limitReached=store.limited(pkg);
        if(!limitReached && (!store.inSchedule() || store.goalMet()))return;
        if(!limitReached && !store.pending(pkg)) {
            long used=store.used(pkg)+delta;
            if(delta>0) store.used(pkg,used);
            if(used>=store.interval(pkg)*60_000L) store.begin(pkg);
        }
        if((limitReached || store.pending(pkg)) && now-lastLaunch>=1800) {
            lastLaunch=now;
            Intent intent=new Intent(this,limitReached?LimitActivity.class:BreakActivity.class).putExtra("package",pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try { startActivity(intent); }
            catch (RuntimeException e) { android.util.Log.w("Pause","Unable to open break screen",e); }
        }
    }
    private String foregroundPackage() {
        try {
            List<AccessibilityWindowInfo> windows=getWindows();
            List<ForegroundWindows.Window> states=new ArrayList<>();
            for(AccessibilityWindowInfo w:windows) {
                states.add(new ForegroundWindows.Window(w.getType(),w.getLayer(),w.isActive(),w.isFocused(),w.isInPictureInPictureMode()));
            }
            int selected=ForegroundWindows.select(states);
            if(selected<0)return null;
            AccessibilityNodeInfo root=windows.get(selected).getRoot();
            if(root==null) return null;
            CharSequence pkg=root.getPackageName();
            String name=pkg==null ? null : pkg.toString();
            root.recycle();
            return name;
        } catch (RuntimeException ignored) { return null; }
    }
    @Override public void onInterrupt() { clock.sample(null,false,SystemClock.elapsedRealtime()); }
    @Override public boolean onUnbind(Intent intent) { connected=false; handler.removeCallbacksAndMessages(null); return super.onUnbind(intent); }
    @Override public void onDestroy() { connected=false; handler.removeCallbacksAndMessages(null); super.onDestroy(); }
}
