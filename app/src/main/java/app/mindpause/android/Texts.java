package app.mindpause.android;
import android.content.Context;
import app.mindpause.android.core.Subject;
final class Texts {
    static String subject(Context c,Subject s){return c.getString(new int[]{R.string.subject_math,R.string.subject_russian,R.string.subject_english,R.string.subject_history,R.string.subject_geography,R.string.subject_memory}[s.ordinal()]);}
    static String duration(Context c,long ms) {
        long seconds=ms/1000;
        if(ms>0 && seconds==0)return c.getString(R.string.less_second);
        if(seconds<60)return c.getString(R.string.duration_s,seconds);
        if(seconds<3600)return c.getString(R.string.duration_ms,seconds/60,seconds%60);
        return c.getString(R.string.duration_hm,seconds/3600,seconds/60%60);
    }
    static String level(Context c,int level){return c.getString(new int[]{R.string.basic,R.string.medium,R.string.hard}[level]);}
}
