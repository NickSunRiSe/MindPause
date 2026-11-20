package app.mindpause.android;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import app.mindpause.android.core.Exercise;
import app.mindpause.android.core.MemoryExercise;
import app.mindpause.android.core.Subject;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.function.Predicate;

final class ProgressTests {
    private final Instrumentation runner;
    private final Context context;
    private static final String PKG="app.mindpause.fixture";
    ProgressTests(Instrumentation runner){this.runner=runner;context=runner.getTargetContext();}

    private static void require(boolean state,String message){if(!state)throw new AssertionError(message);}
    private SharedPreferences clean(String name){
        SharedPreferences prefs=context.getSharedPreferences(name,Context.MODE_PRIVATE);
        prefs.edit().clear().commit();return prefs;
    }
    private View find(View view,Predicate<View> match){
        if(match.test(view))return view;
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int n=0;n<group.getChildCount();n++){View result=find(group.getChildAt(n),match);if(result!=null)return result;}
        }
        return null;
    }
    private View text(Activity a,String label){
        return find(a.getWindow().getDecorView(),v->v instanceof TextView && ((TextView)v).getText().toString().equals(label));
    }
    private Activity open(Class<? extends Activity> type){
        Activity a=runner.startActivitySync(new Intent(context,type).putExtra("package",PKG).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        runner.waitForIdleSync();return a;
    }
    void run(){
        runner.runOnMainSync(this::midnightAndMigration);
        runner.runOnMainSync(()->{
            clean("pause");clean("viewing");clean("study-archive");
            Store s=new Store(context);
            require(s.limit(PKG)==0 && s.goal()==0,"limits opt in");
            s.select(new HashSet<>(Arrays.asList(PKG,"other.test")));
            s.subjects(Collections.singleton("memory"));
            s.p.edit().putInt("learning.memory.mode",1).putInt("learning.memory.level",1)
                .putInt(PKG+".count",3).putInt(PKG+".dailyLimit",1).commit();
            s.begin(PKG);s.begin("other.test");s.setGoal(1);
        });
        Activity first=open(BreakActivity.class);
        runner.runOnMainSync(()->{
            Exercise e=new Store(first).problem(PKG);
            require(text(first,e.answers.get(0))!=null,"digits visible before recall");
            text(first,first.getString(R.string.ready_recall)).performClick();
            require(text(first,e.answers.get(0))==null,"digits hidden in manual recall");
            first.finish();
        });
        runner.waitForIdleSync();
        Activity recall=open(BreakActivity.class);
        SystemClock.sleep(1200);
        runner.runOnMainSync(()->{
            Store s=new Store(recall);Exercise e=s.problem(PKG);
            require(text(recall,recall.getString(R.string.ready_recall))==null,"reopening cannot reveal memorized digits");
            EditText answer=(EditText)find(recall.getWindow().getDecorView(),v->v instanceof EditText);
            View submit=text(recall,recall.getString(R.string.check));
            submit.performClick();
            answer.setText("00");submit.performClick();
            answer.setText(e.answers.get(0));submit.performClick();
            StudyHistory.Day day=s.history.day(LocalDate.now().toString());
            require(day.total("wrong")==1 && day.total("solved")==1,"one wrong answer and one independent solve");
            require(day.group(Subject.MEMORY,1).optLong("ms")>=500,"active solve time recorded for level");
            require(s.goalMet()&&!s.pending(PKG)&&!s.pending("other.test"),"goal ends multi-question break across apps");
            require(!s.completeQuestion(PKG,e.id),"cannot count completion twice");
            s.viewing.add(PKG,60_000,ZonedDateTime.now().withHour(12));
            require(s.limited(PKG)&&!s.limited("other.test")&&s.goalMet(),"goal and per-app limit independent");
            s.reset(PKG);require(s.limited(PKG),"interval reset retains daily viewing");
            recall.finish();
        });
        runner.waitForIdleSync();
        Activity stats=open(StatsActivity.class);
        runner.runOnMainSync(()->{
            require(text(stats,Texts.subject(stats,Subject.MEMORY))!=null,"daily subject breakdown visible");
            require(text(stats,stats.getString(R.string.study_counts,1,0,0,1))!=null,"daily stats shown");
            text(stats,stats.getString(R.string.all_study_days)).performClick();
            require(text(stats,stats.getString(R.string.study_counts,1,0,0,1))!=null,"all-day summary shown");
            stats.finish();
        });
        runner.waitForIdleSync();
        Activity cap=open(LimitActivity.class);
        runner.runOnMainSync(()->{
            require(text(cap,cap.getString(R.string.go_home))!=null,"limit has exit to home");
            cap.finish();
        });
    }
    private void midnightAndMigration(){
        SharedPreferences live=clean("test.study.live");
        SharedPreferences archive=clean("test.study.archive");
        live.edit().putString("statsDate","2024-04-01").putInt("stats.solved",3).commit();
        StudyHistory h=new StudyHistory(live,archive);
        Exercise e=MemoryExercise.generate(new Random(35),2);
        SharedPreferences.Editor next=live.edit();
        h.record(next,"solved",e,3800,"2024-04-02");next.commit();
        next=live.edit();h.record(next,"wrong",e,0,"2024-04-03");next.commit();
        h=new StudyHistory(live,archive);
        require(h.day("2024-04-01").total("solved")==3 && h.day("2024-04-01").total("timed")==0,"legacy totals preserved without invented durations");
        require(h.day("2024-04-02").group(Subject.MEMORY,2).optLong("ms")==3800 && h.day("2024-04-03").total("solved")==0,"day archives are independent");
        Viewing viewing=new Viewing(clean("test.viewing"));
        ZonedDateTime end=ZonedDateTime.parse("2024-04-02T00:00:00.400+04:00");
        viewing.add(PKG,1000,end);viewing.add("other",200,end);
        require(viewing.total(PKG)==1000 && viewing.today(PKG,"2024-04-02")==400,"viewing split at midnight");
        require(viewing.today(PKG,"2024-04-03")==0 && viewing.packages().size()==2,"daily reset and lifetime packages");
    }
}
