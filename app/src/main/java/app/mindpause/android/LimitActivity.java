package app.mindpause.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;

/** A voluntary daily stop. The user can always go home and change the app rule. */
public final class LimitActivity extends BaseActivity {
    private Store store;
    private String pkg;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){
        @Override public void run(){
            if(!store.enabled() || !store.apps().contains(pkg) || !store.limited(pkg)){finish();return;}
            handler.postDelayed(this,1000);
        }
    };
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        store=new Store(this);
        show(getIntent());
        if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
            android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::home);
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);show(intent);}
    private void show(Intent intent){
        pkg=intent.getStringExtra("package");
        if(pkg==null || !store.enabled() || !store.apps().contains(pkg) || !store.limited(pkg)){finish();return;}
        String name=pkg;
        try{name=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}
        catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
        LinearLayout page=Ui.page(this);
        Ui.space(page,40);
        Ui.add(page,getString(R.string.limit_title),30,Ui.INK,true);
        Ui.space(page,16);
        Ui.add(page,getString(R.string.limit_message,name,store.limit(pkg)),18,Ui.MUTED,false);
        Ui.button(page,getString(R.string.go_home),true,this::home);
    }
    @Override protected void onResume(){super.onResume();if(!refreshAppearance())handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    private void home(){
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
    // Android 13+ uses the registered OnBackInvokedCallback. Older versions call this method.
    @android.annotation.SuppressLint("GestureBackNavigation") @Override public void onBackPressed(){home();}
}
