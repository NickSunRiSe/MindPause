package app.mindpause.android;

import android.content.SharedPreferences;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.TreeSet;

/** Lifetime totals are independent of the resettable interval and daily allowance. */
final class Viewing {
    private final SharedPreferences prefs;
    Viewing(SharedPreferences prefs) { this.prefs=prefs; }
    long total(String pkg) { return prefs.getLong("total."+pkg,0); }
    long today(String pkg,String date) {
        return date.equals(prefs.getString("date."+pkg,""))?prefs.getLong("day."+pkg,0):0;
    }
    Set<String> packages() {
        Set<String> result=new TreeSet<>();
        for(String key:prefs.getAll().keySet())if(key.startsWith("total."))result.add(key.substring(6));
        return result;
    }
    void add(String pkg,long delta,ZonedDateTime end) {
        if(delta<=0)return;
        String date=end.toLocalDate().toString();
        long midnight=end.toLocalDate().atStartOfDay(end.getZone()).toInstant().toEpochMilli();
        long todayPart=Math.min(delta,end.toInstant().toEpochMilli()-midnight);
        prefs.edit().putLong("total."+pkg,total(pkg)+delta).putString("date."+pkg,date)
            .putLong("day."+pkg,today(pkg,date)+todayPart).apply();
    }
}
