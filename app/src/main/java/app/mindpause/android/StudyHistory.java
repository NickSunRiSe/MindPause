package app.mindpause.android;

import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.TreeSet;

import app.mindpause.android.core.Exercise;
import app.mindpause.android.core.Subject;

/** Today's outcomes are committed with the pending question. Only finished days are archived. */
final class StudyHistory {
    static final String SOLVED="solved", REVIEWED="reviewed", SKIPS="skips", WRONG="wrong", TIME="ms";
    private final SharedPreferences live, archive;

    StudyHistory(SharedPreferences live, SharedPreferences archive) {
        this.live=live;
        this.archive=archive;
    }

    static final class Day {
        final JSONObject data;
        Day(String json) {
            try { data=new JSONObject(json); }
            catch(JSONException e) { throw new IllegalStateException("Invalid study history",e); }
        }
        long total(String metric) { return data.optLong(metric); }
        JSONObject group(Subject subject,int level) { return data.optJSONObject(subject.id+"."+level); }
        void add(JSONObject target,String metric,long amount) {
            try { target.put(metric,target.optLong(metric)+amount); }
            catch(JSONException e) { throw new IllegalStateException(e); }
        }
        void record(String metric,Exercise exercise,long duration) {
            add(data,metric,1);
            if(exercise==null)return;
            String key=exercise.subject.id+"."+exercise.level;
            JSONObject group=group(exercise.subject,exercise.level);
            if(group==null){
                group=new JSONObject();
                try { data.put(key,group); } catch(JSONException e){throw new IllegalStateException(e);}
            }
            add(group,metric,1);
            if(SOLVED.equals(metric)){
                add(group,TIME,duration);
                add(data,TIME,duration);
                add(data,"timed",1);
            }
        }
    }

    Day day(String date) {
        if(!date.equals(live.getString("statsDate","")))return new Day(archive.getString(date,"{}"));
        if(live.contains("stats.details"))return new Day(live.getString("stats.details","{}"));
        // The old format saved the last day's totals without subjects or durations.
        Day legacy=new Day("{}");
        for(String key:new String[]{SOLVED,REVIEWED,SKIPS})legacy.add(legacy.data,key,live.getInt("stats."+key,0));
        return legacy;
    }

    TreeSet<String> dates() {
        TreeSet<String> dates=new TreeSet<>(archive.getAll().keySet());
        String active=live.getString("statsDate","");
        if(!active.isEmpty())dates.add(active);
        dates.add(LocalDate.now().toString());
        return dates;
    }

    int record(SharedPreferences.Editor transaction,String metric,Exercise exercise,long duration,String date) {
        String old=live.getString("statsDate","");
        if(!old.isEmpty()&&!old.equals(date)) {
            // Write the old snapshot first. Repeating this after a restart is harmless.
            if(!archive.edit().putString(old,day(old).data.toString()).commit())
                throw new IllegalStateException("Could not archive study day");
        }
        Day next=day(date);
        next.record(metric,exercise,duration);
        transaction.putString("statsDate",date).putString("stats.details",next.data.toString());
        for(String key:new String[]{SOLVED,REVIEWED,SKIPS})transaction.putInt("stats."+key,(int)next.total(key));
        return (int)next.total(SOLVED);
    }
}
