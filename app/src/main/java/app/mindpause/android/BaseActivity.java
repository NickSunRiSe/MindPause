package app.mindpause.android;

import android.app.Activity;
import android.content.Context;

public abstract class BaseActivity extends Activity {
    private String appearanceKey;
    @Override protected void attachBaseContext(Context base){appearanceKey=Appearance.key(base);super.attachBaseContext(Appearance.wrap(base));}
    protected boolean refreshAppearance(){if(!Appearance.key(this).equals(appearanceKey)){recreate();return true;}return false;}
}
