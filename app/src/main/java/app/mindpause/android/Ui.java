package app.mindpause.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class Ui {
    static final int BG=Color.rgb(245,244,239), INK=Color.rgb(34,42,36), MUTED=Color.rgb(102,111,102), LIME=Color.rgb(214,245,140), WHITE=Color.WHITE;
    static final int ON_ACCENT=Color.rgb(33,42,35);
    static int ink(Context c){return Appearance.dark(c)?Color.rgb(235,240,232):INK;}
    static int muted(Context c){return Appearance.dark(c)?Color.rgb(169,184,172):MUTED;}
    static int surface(Context c){return Appearance.dark(c)?Color.rgb(32,42,36):WHITE;}
    static int background(Context c){return Appearance.dark(c)?Color.rgb(20,26,23):BG;}
    static int error(Context c){return Appearance.dark(c)?Color.rgb(255,171,145):Color.rgb(159,65,42);}
    static int dp(Context c,float n) { return (int)(n*c.getResources().getDisplayMetrics().density+.5f); }
    static GradientDrawable bg(int color,int radius,Context c) { GradientDrawable d=new GradientDrawable(); d.setColor(color==WHITE?surface(c):color); d.setCornerRadius(dp(c,radius)); return d; }
    static LinearLayout column(Context c) { LinearLayout v=new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v; }
    static LinearLayout page(Activity a) {
        ScrollView scroll=new ScrollView(a); scroll.setFillViewport(true); scroll.setBackgroundColor(background(a));
        LinearLayout page=column(a); page.setPadding(dp(a,24),dp(a,20),dp(a,24),dp(a,32));
        scroll.addView(page,new ScrollView.LayoutParams(-1,-2)); a.setContentView(scroll);
        if(Build.VERSION.SDK_INT>=30) a.getWindow().setDecorFitsSystemWindows(false);
        a.getWindow().setStatusBarColor(background(a));a.getWindow().setNavigationBarColor(background(a));
        if(Build.VERSION.SDK_INT>=30){int flags=android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;a.getWindow().getInsetsController().setSystemBarsAppearance(Appearance.dark(a)?0:flags,flags);}
        else {int flags=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;a.getWindow().getDecorView().setSystemUiVisibility(Appearance.dark(a)?0:flags);}
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets s=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                scroll.setPadding(s.left,s.top,s.right,s.bottom);
            } else scroll.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.requestApplyInsets(); return page;
    }
    static TextView text(Context c,String value,int size,int color,boolean bold) {
        TextView t=new TextView(c); t.setText(value); t.setTextSize(size); t.setTextColor(color==INK?ink(c):color==MUTED?muted(c):color);
        t.setLineSpacing(dp(c,3),1f); if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        return t;
    }
    static TextView add(LinearLayout parent,String value,int size,int color,boolean bold) {
        TextView t=text(parent.getContext(),value,size,color,bold); parent.addView(t); return t;
    }
    static void space(LinearLayout parent,int n) { View v=new View(parent.getContext()); parent.addView(v,new LinearLayout.LayoutParams(1,dp(parent.getContext(),n))); }
    static LinearLayout card(LinearLayout parent,int color) {
        Context c=parent.getContext(); LinearLayout v=column(c); v.setPadding(dp(c,20),dp(c,20),dp(c,20),dp(c,20)); v.setBackground(bg(color,24,c));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(c,12); parent.addView(v,lp); return v;
    }
    static Button button(LinearLayout parent,String label,boolean primary,Runnable action) {
        Context c=parent.getContext(); Button b=new Button(c); b.setText(label); b.setTextSize(15); b.setAllCaps(false);
        // A platform Button keeps its elevation animator after a custom background is set.
        // Its shadow then clips against full-width parent bounds, especially on One UI.
        b.setStateListAnimator(null); b.setElevation(0); b.setTranslationZ(0);
        b.setBackgroundTintList(null); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setTextColor(primary?ON_ACCENT:ink(c)); b.setMinHeight(dp(c,56)); b.setMinimumHeight(dp(c,56));
        b.setGravity(Gravity.CENTER); b.setSingleLine(false); b.setMaxLines(Integer.MAX_VALUE);
        b.setPadding(dp(c,18),dp(c,14),dp(c,18),dp(c,14));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(primary?0x18222A24:Appearance.dark(c)?0x30FFFFFF:0x18222A24),
            bg(primary?LIME:Appearance.dark(c)?Color.rgb(45,58,49):Color.rgb(231,234,225),16,c),bg(WHITE,16,c)));
        b.setClipToOutline(true);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(c,10); parent.addView(b,lp);
        b.setOnClickListener(v->action.run()); return b;
    }
}
