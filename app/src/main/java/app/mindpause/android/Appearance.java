package app.mindpause.android;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import java.util.Locale;

final class Appearance {
    static String language(Context c) {
        if(Build.VERSION.SDK_INT>=33){
            LocaleList locales=c.getSystemService(android.app.LocaleManager.class).getApplicationLocales();
            return locales.isEmpty()?"system":locales.get(0).getLanguage();
        }
        return c.getSharedPreferences("pause",0).getString("ui.language","system");
    }
    static String theme(Context c){return c.getSharedPreferences("pause",0).getString("ui.theme","system");}
    static boolean english(Context c){
        String lang=language(c);
        if(lang.equals("system"))lang=Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();
        return !lang.equals("ru");
    }
    static boolean dark(Context c){return (c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;}
    static String key(Context c){return language(c)+"/"+theme(c);}
    static void language(Context c,String choice){
        c.getSharedPreferences("pause",0).edit().putString("ui.language",choice).apply();
        if(Build.VERSION.SDK_INT>=33)c.getSystemService(android.app.LocaleManager.class).setApplicationLocales(choice.equals("system")?LocaleList.getEmptyLocaleList():LocaleList.forLanguageTags(choice));
    }
    static void theme(Context c,String choice){c.getSharedPreferences("pause",0).edit().putString("ui.theme",choice).apply();}
    static Context wrap(Context base){
        Configuration config=new Configuration(base.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(english(base)?"en":"ru"));
        String choice=theme(base);
        if(!choice.equals("system"))config.uiMode=(config.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|(choice.equals("dark")?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO);
        return base.createConfigurationContext(config);
    }
}
