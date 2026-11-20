package app.mindpause.android;

import android.content.Context;
import android.content.SharedPreferences;
import app.mindpause.android.core.Problem;
import app.mindpause.android.core.Schedule;
import app.mindpause.android.core.Exercise;
import app.mindpause.android.core.Subject;
import app.mindpause.android.core.QuestionBank;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

final class Store {
    final SharedPreferences p;
    private final Context context;
    final StudyHistory history;
    final Viewing viewing;
    private static QuestionBank bank, englishBank;
    Store(Context c) {
        context=c;p = c.getSharedPreferences("pause", Context.MODE_PRIVATE);
        history=new StudyHistory(p,c.getSharedPreferences("study-archive",Context.MODE_PRIVATE));
        viewing=new Viewing(c.getSharedPreferences("viewing",Context.MODE_PRIVATE));
        if(!p.contains("learning.schema")) {
            boolean upgrading=p.contains("apps");Set<String> selected=new HashSet<>();
            for(Subject s:Subject.values())if(!upgrading || s==Subject.MATH)selected.add(s.id);
            p.edit().putStringSet("learning.subjects",selected).putInt("learning.schema",1).apply();
        }
    }
    Set<String> subjects() {
        Set<String> defaults=new HashSet<>();for(Subject s:Subject.values())defaults.add(s.id);
        Set<String> selected=new HashSet<>(p.getStringSet("learning.subjects",defaults));selected.retainAll(defaults);
        if(selected.isEmpty())selected.add(Subject.MATH.id);return selected;
    }
    void subjects(Set<String> selected) {
        if(selected.isEmpty())throw new IllegalArgumentException("Select at least one subject");
        for(String id:selected)Subject.byId(id);
        p.edit().putStringSet("learning.subjects",new HashSet<>(selected)).apply();
    }
    int level(Subject subject,String pkg) {return p.getInt("learning."+subject.id+".level",subject==Subject.MATH?p.getInt(pkg+".difficulty",0):0);}
    int mode(Subject subject) {return p.getInt("learning."+subject.id+".mode",subject==Subject.MATH?1:2);}
    int reviewAction() {return Math.max(0,Math.min(2,p.getInt("learning.reviewAction",0)));}
    void preset() {
        Set<String> all=new HashSet<>();SharedPreferences.Editor e=p.edit();
        for(Subject s:Subject.values()){all.add(s.id);e.putInt("learning."+s.id+".level",0).putInt("learning."+s.id+".mode",s==Subject.MATH?1:2);}
        e.putStringSet("learning.subjects",all).apply();
    }
    private QuestionBank bank() {
        if(bank==null)try(InputStream in=context.getAssets().open("questions.tsv")){bank=new QuestionBank(new InputStreamReader(in,StandardCharsets.UTF_8));}
        catch(IOException e){throw new IllegalStateException("Invalid bundled question bank",e);}
        return bank;
    }
    int bankSize(Subject subject,int level) {return bank().pool(subject,level).size();}
    private QuestionBank englishBank(){
        if(englishBank==null)try(InputStream in=context.getAssets().open("questions-en.tsv")){englishBank=new QuestionBank(new InputStreamReader(in,StandardCharsets.UTF_8));}
        catch(IOException e){throw new IllegalStateException("Invalid English question bank",e);}
        return englishBank;
    }
    Exercise localize(Exercise original) {
        if(original.subject==Subject.MEMORY)return app.mindpause.android.core.MemoryExercise.localize(original,Appearance.english(context));
        if(original.subject==Subject.MATH)return app.mindpause.android.core.ExerciseLanguage.math(original,Appearance.english(context));
        Exercise canonical=bank().byId(original.id);
        if(canonical==null)return original;
        Exercise translated=canonical;
        if(Appearance.english(context)) {
            translated=englishBank().byId(original.id);
            if(translated==null)throw new IllegalStateException("Missing translation: "+original.id);
        }
        Exercise source=canonical;
        Exercise english=englishBank().byId(original.id);
        if(english!=null && original.question.equals(english.question))source=english;
        return app.mindpause.android.core.ExerciseLanguage.project(original,source,translated);
    }
    boolean enabled() { return p.getBoolean("enabled", true); }
    boolean pauseMedia() { return p.getBoolean("pauseMedia",false); }
    Set<String> apps() { return new HashSet<>(p.getStringSet("apps", new HashSet<>())); }
    void select(Set<String> apps) {
        Set<String> old = apps();
        for (String pkg : old) if (!apps.contains(pkg)) reset(pkg);
        p.edit().putStringSet("apps", new HashSet<>(apps)).apply();
    }
    int interval(String pkg) { return p.getInt(pkg+".minutes", 5); }
    int limit(String pkg) { return p.getInt(pkg+".dailyLimit",0); }
    boolean limited(String pkg) {
        int minutes=limit(pkg);
        return minutes>0 && viewing.today(pkg,LocalDate.now().toString())>=minutes*60_000L;
    }
    int goal() { return p.getInt("dailyGoal",0); }
    boolean goalMet() { return goal()>0 && today("solved")>=goal(); }
    void setGoal(int goal) {
        SharedPreferences.Editor edit=p.edit().putInt("dailyGoal",goal);
        if(goal>0 && today("solved")>=goal)for(String selected:apps())clearInterval(edit,selected);
        edit.apply();
    }
    int difficulty(String pkg) { return p.getInt(pkg+".difficulty", 1); }
    int count(String pkg) { return p.getInt(pkg+".count", 1); }
    int quota(String pkg) { return p.getInt(pkg+".quota", 3); }
    int skipDelay(String pkg) { return p.getInt(pkg+".skipDelay", 10); }
    long used(String pkg) { return p.getLong(pkg+".used", 0); }
    void used(String pkg, long ms) { p.edit().putLong(pkg+".used", ms).apply(); }
    void reset(String pkg) {
        SharedPreferences.Editor edit=p.edit();
        clearInterval(edit,pkg);
        edit.apply();
    }
    private void clearInterval(SharedPreferences.Editor edit,String pkg) {
        edit.putLong(pkg+".used",0).remove(pkg+".pending").remove(pkg+".problem").remove(pkg+".done")
            .remove(pkg+".waited").remove(pkg+".revealed").remove(pkg+".solveMs").remove(pkg+".recall");
    }
    void answerError(String pkg,Exercise exercise) {
        if(!pending(pkg)||!subjects().contains(exercise.subject.id)||!current(pkg,exercise.id)||revealed(pkg,exercise.id))return;
        SharedPreferences.Editor edit=p.edit();
        history.record(edit,StudyHistory.WRONG,exercise,0,LocalDate.now().toString());
        edit.apply();
    }
    boolean revealed(String pkg,String id){return id.equals(p.getString(pkg+".revealed",""));}
    private boolean current(String pkg,String id){
        try{return id.equals(new JSONObject(p.getString(pkg+".problem","{}")).optString("id"));}catch(Exception e){return false;}
    }
    /** Seeing an answer is recorded once, but never advances the break. */
    boolean reveal(String pkg,String id){
        if((!pending(pkg)&&!pkg.equals("demo")) || !current(pkg,id))return false;
        if(revealed(pkg,id))return true;
        SharedPreferences.Editor e=p.edit().putString(pkg+".revealed",id);
        if(!pkg.equals("demo")){
            Exercise exercise=problem(pkg);
            if(!id.equals(exercise.id))return false;
            history.record(e,StudyHistory.REVIEWED,exercise,0,LocalDate.now().toString());
        }
        e.apply();return true;
    }
    Exercise replaceRevealed(String pkg,String id){
        if((!pending(pkg)&&!pkg.equals("demo")) || !current(pkg,id) || !revealed(pkg,id))return null;
        return newProblem(pkg);
    }
    boolean skipRevealed(String pkg,String id){return current(pkg,id)&&revealed(pkg,id)&&skip(pkg);}
    /** Only an unexposed, current question can advance the break, once. */
    boolean completeQuestion(String pkg,String id){
        if(!pending(pkg) || !current(pkg,id) || revealed(pkg,id))return false;
        int done=p.getInt(pkg+".done",0)+1;
        SharedPreferences.Editor e=p.edit();
        Exercise exercise=problem(pkg);
        if(!id.equals(exercise.id))return false;
        int total=history.record(e,StudyHistory.SOLVED,exercise,p.getLong(pkg+".solveMs",0),LocalDate.now().toString());
        e.putInt(pkg+".done",done).remove(pkg+".problem").remove(pkg+".revealed").remove(pkg+".solveMs");
        if(goal()>0 && total>=goal())for(String selected:apps())clearInterval(e,selected);
        else if(done>=count(pkg))clearInterval(e,pkg);
        e.apply();return true;
    }
    boolean pending(String pkg) { return p.getBoolean(pkg+".pending", false); }
    void begin(String pkg) { p.edit().putBoolean(pkg+".pending", true).putInt(pkg+".done", 0).putLong(pkg+".waited",0).apply(); problem(pkg); }
    boolean inSchedule() { return !p.getBoolean("schedule", false) || Schedule.includes(LocalDateTime.now(), p.getInt("start", 0), p.getInt("end", 0), p.getInt("days",127)); }
    int skipped(String pkg) { return LocalDate.now().toString().equals(p.getString(pkg+".skipDate", "")) ? p.getInt(pkg+".skipped",0) : 0; }
    boolean canSkip(String pkg) { return quota(pkg) == -1 || skipped(pkg) < quota(pkg); }
    boolean skip(String pkg) {
        if (!pending(pkg) || !canSkip(pkg) || p.getLong(pkg+".waited",0) < skipDelay(pkg)*1000L) return false;
        int n = skipped(pkg)+1;
        String date=LocalDate.now().toString();
        SharedPreferences.Editor edit=p.edit().putString(pkg+".skipDate",date).putInt(pkg+".skipped",n);
        history.record(edit,StudyHistory.SKIPS,problem(pkg),0,date);
        clearInterval(edit,pkg);
        edit.apply();
        return true;
    }
    int today(String key) {
        String date=LocalDate.now().toString();
        if(date.equals(p.getString("statsDate","")) && p.contains("stats."+key))return p.getInt("stats."+key,0);
        return (int)history.day(date).total(key);
    }
    void increment(String key) {
        SharedPreferences.Editor edit=p.edit();
        history.record(edit,key,null,0,LocalDate.now().toString());
        edit.apply();
    }
    Exercise problem(String pkg) {
        try {
            JSONObject j=new JSONObject(p.getString(pkg+".problem", ""));
            if(!j.has("subject")) {
                if(!subjects().contains("math"))return newProblem(pkg);
                Exercise migrated=Exercise.math(new Problem(j.getString("t"),j.getString("q"),j.getInt("a"),j.getString("h"),j.getString("e")),difficulty(pkg),new Random());
                saveProblem(pkg,migrated);return localize(migrated);
            }
            Subject subject=Subject.byId(j.getString("subject"));
            if(!subjects().contains(subject.id))return newProblem(pkg);
            return localize(new Exercise(j.getString("id"),subject,j.getInt("level"),j.getString("t"),j.getString("q"),
                list(j.getJSONArray("answers")),list(j.getJSONArray("options")),j.getString("h"),j.getString("e"),j.getString("source"),
                j.getBoolean("strict"),j.getBoolean("numeric"),j.getBoolean("choices")));
        } catch (Exception ignored) { return newProblem(pkg); }
    }
    private List<String> list(JSONArray a) {List<String> list=new ArrayList<>();for(int i=0;i<a.length();i++)list.add(a.optString(i));return list;}
    private void saveProblem(String pkg,Exercise e) throws org.json.JSONException {
        JSONObject j=new JSONObject();j.put("t",e.title).put("q",e.question).put("a",e.answers.get(0)).put("h",e.hint).put("e",e.explanation)
            .put("id",e.id).put("subject",e.subject.id).put("level",e.level).put("answers",new JSONArray(e.answers)).put("options",new JSONArray(e.options))
            .put("source",e.source).put("strict",e.strictSpelling).put("numeric",e.numeric).put("choices",e.choices);
        p.edit().putString(pkg+".problem",j.toString()).apply();
    }
    Exercise newProblem(String pkg) {
        Random r=new Random();
        List<String> selected=new ArrayList<>(subjects());Collections.sort(selected);
        Subject subject=Subject.byId(selected.get(r.nextInt(selected.size())));int level=level(subject,pkg);
        String historyKey="history."+subject.id+"."+level;
        JSONArray history;
        try { history=new JSONArray(p.getString(historyKey,"[]")); } catch (Exception ignored) { history=new JSONArray(); }
        List<String> recent=list(history);Exercise exercise;int limit=200;
        // Include the current ID even when migrating an older pending question whose
        // history is missing. Replacement must never give the revealed question back.
        try{String currentId=new JSONObject(p.getString(pkg+".problem","{}")).optString("id");if(!currentId.isEmpty()&&!recent.contains(currentId))recent.add(currentId);}catch(Exception ignored){}
        if(subject==Subject.MEMORY) {
            do { exercise=app.mindpause.android.core.MemoryExercise.generate(r,level); } while(recent.contains(exercise.id));
        } else if(subject==Subject.MATH) {
            Problem problem;do {problem=Problem.generate(r,level);}while(recent.contains(problem.question));
            exercise=Exercise.math(problem,level,r);
        } else {
            List<Exercise> pool=bank().pool(subject,level);exercise=QuestionBank.choose(pool,recent,r);limit=Math.max(1,pool.size()-1);
        }
        exercise=exercise.withMode(mode(subject),r);
        JSONArray next=new JSONArray();
        for(int i=Math.max(0,history.length()-(limit-1));i<history.length();i++) next.put(history.optString(i));
        next.put(exercise.id);
        try {
            JSONObject j=new JSONObject();
            j.put("t",exercise.title).put("q",exercise.question).put("a",exercise.answers.get(0)).put("h",exercise.hint).put("e",exercise.explanation)
                .put("id",exercise.id).put("subject",subject.id).put("level",level).put("answers",new JSONArray(exercise.answers))
                .put("options",new JSONArray(exercise.options)).put("source",exercise.source).put("strict",exercise.strictSpelling)
                .put("numeric",exercise.numeric).put("choices",exercise.choices);
            p.edit().putString(pkg+".problem",j.toString()).remove(pkg+".revealed").remove(pkg+".recall").remove(pkg+".solveMs").putString(historyKey,next.toString()).apply();
        } catch (Exception e) { throw new IllegalStateException(e); }
        return localize(exercise);
    }
}
