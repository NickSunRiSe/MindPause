package app.mindpause.android;

import android.app.Instrumentation;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import java.util.Collections;
import java.util.*;
import app.mindpause.android.core.*;

/** Local emulator smoke tests, without extra Android runtime dependencies. */
public final class DeviceTests extends Instrumentation {
    private boolean prepareTimer, prepareLimit;
    @Override public void onCreate(Bundle args){super.onCreate(args);prepareTimer="true".equals(args.getString("prepareTimer"));prepareLimit="true".equals(args.getString("prepareLimit"));start();}
    private void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private android.view.View find(android.view.View view,java.util.function.Predicate<android.view.View> match){
        if(match.test(view))return view;
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){android.view.View found=find(group.getChildAt(i),match);if(found!=null)return found;}}
        return null;
    }
    @Override public void onStart(){
        Bundle results=new Bundle();
        try{
            if(prepareTimer || prepareLimit){
                runOnMainSync(()->{Store s=new Store(getTargetContext());s.p.edit().clear().commit();
                    Appearance.language(getTargetContext(),"ru");s=new Store(getTargetContext());
                    s.select(Collections.singleton("app.mindpause.fixture"));s.subjects(Collections.singleton("math"));
                    getTargetContext().getSharedPreferences("viewing",0).edit().clear().commit();
                    getTargetContext().getSharedPreferences("study-archive",0).edit().clear().commit();
                    s.p.edit().putInt("app.mindpause.fixture.minutes",1).putInt("app.mindpause.fixture.skipDelay",0).commit();
                    if(prepareLimit){
                        String pkg="app.mindpause.fixture";s.begin(pkg);s.completeQuestion(pkg,s.problem(pkg).id);s.setGoal(1);
                        s.p.edit().putInt(pkg+".dailyLimit",1).commit();
                    }});
                results.putString("stream","PASS: prepared foreground test\n");finish(Activity.RESULT_OK,results);return;
            }
            runOnMainSync(()->{
                Store s=new Store(getTargetContext());s.p.edit().clear().commit();String pkg="app.mindpause.fixture";
                getTargetContext().getSharedPreferences("study-archive",0).edit().clear().commit();
                Appearance.language(getTargetContext(),"ru");s=new Store(getTargetContext());require(s.subjects().size()==Subject.values().length,"fresh install includes the ready preset");
                s.p.edit().clear().putStringSet("apps",Collections.singleton(pkg)).putInt(pkg+".difficulty",2).putInt(pkg+".minutes",3).commit();
                s=new Store(getTargetContext());require(s.subjects().equals(Collections.singleton("math")),"migration preserves math-only selection");require(s.level(Subject.MATH,pkg)==2 && s.interval(pkg)==3,"migration preserves old app rules");
                s.subjects(new HashSet<>(Arrays.asList("history","math")));
                s.p.edit().putInt("learning.history.level",2).putInt("learning.history.mode",2).putInt("learning.math.level",0).putInt("learning.math.mode",1).commit();
                for(int i=0;i<100;i++){
                    Exercise e=s.newProblem(pkg);require(e.subject==Subject.HISTORY || e.subject==Subject.MATH,"disabled subjects excluded");
                    require(e.level==(e.subject==Subject.HISTORY?2:0),"per-subject level");require(e.choices==(e.subject==Subject.HISTORY),"per-subject mode");
                    Exercise restored=new Store(getTargetContext()).problem(pkg);require(restored.id.equals(e.id)&&restored.options.equals(e.options)&&restored.choices==e.choices,"question and option order persist");
                }
                s.subjects(Collections.singleton("geography"));Exercise geography=s.problem(pkg);require(geography.subject==Subject.GEOGRAPHY,"deselection replaces pending foreign-subject question");
                try{s.subjects(Collections.emptySet());throw new AssertionError("empty selection accepted");}catch(IllegalArgumentException expected){}
                s.subjects(Collections.singleton("math"));s.reset(pkg);
                s.select(Collections.singleton(pkg));s.p.edit().putInt(pkg+".minutes",1).putInt(pkg+".quota",1).putInt(pkg+".skipDelay",10).commit();
                s.begin(pkg);String q=s.problem(pkg).question;require(new Store(getTargetContext()).problem(pkg).question.equals(q),"problem survives Store recreation");
                require(!s.skip(pkg),"delay prevents skip");s.p.edit().putLong(pkg+".waited",10000).commit();require(s.skip(pkg),"eligible skip succeeds");
                s.begin(pkg);s.p.edit().putLong(pkg+".waited",10000).commit();require(!s.skip(pkg),"daily quota enforced");
                s.reset(pkg);require(!s.canSkip(pkg),"reset does not restore quota");
                s.p.edit().putString(pkg+".skipDate","2001-01-01").commit();require(s.canSkip(pkg),"new date resets quota");
                s.increment("solved");require(s.today("solved")==1,"daily statistics");
                s.p.edit().putString("statsDate","2001-01-01").commit();require(s.today("solved")==0,"statistics roll over");
                s.begin(pkg);s.select(Collections.emptySet());require(!s.pending(pkg),"removing app clears break");
                s.select(Collections.singleton(pkg));s.p.edit().putInt(pkg+".quota",3).putInt(pkg+".skipDelay",0).commit();s.reset(pkg);
            });
            Activity a=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();runOnMainSync(a::finish);
            Activity b=startActivitySync(new Intent(getTargetContext(),BreakActivity.class).putExtra("demo",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();runOnMainSync(b::finish);
            for(Subject subject:Subject.values())for(int mode:new int[]{1,2}){
                runOnMainSync(()->{Store s=new Store(getTargetContext());s.subjects(Collections.singleton(subject.id));s.p.edit().putInt("learning."+subject.id+".mode",mode).commit();});
                Activity activity=startActivitySync(new Intent(getTargetContext(),BreakActivity.class).putExtra("demo",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
                runOnMainSync(()->{
                    Exercise e=new Store(getTargetContext()).problem("demo");
                    if(subject==Subject.MEMORY)find(activity.getWindow().getDecorView(),v->v instanceof android.widget.Button && ((android.widget.Button)v).getText().equals(activity.getString(R.string.ready_recall))).performClick();
                    android.view.View root=activity.getWindow().getDecorView();
                    if(mode==1){android.widget.EditText field=(android.widget.EditText)find(root,v->v instanceof android.widget.EditText);require(field!=null,"manual input visible");field.setText(e.answers.get(0));
                        android.view.View check=find(root,v->v instanceof android.widget.Button && ((android.widget.Button)v).getText().toString().equals(activity.getString(R.string.check)));require(check!=null,"check button");check.performClick();
                    }else{
                        require(find(root,v->v instanceof android.widget.EditText)==null,"choice mode has no text field");
                        android.view.View option=find(root,v->v instanceof android.widget.Button && e.accepts(((android.widget.Button)v).getText().toString()));require(option!=null,"correct option visible");option.performClick();
                    }
                    require(find(activity.getWindow().getDecorView(),v->v instanceof android.widget.TextView && ((android.widget.TextView)v).getText().toString().equals(activity.getString(R.string.success_title)))!=null,"correct answer shows success for "+subject+"/"+mode);
                    activity.finish();
                });
            }
            // Check a real two-question break, including restoration after leaving between answers.
            String pkg="app.mindpause.fixture";
            runOnMainSync(()->{Store s=new Store(getTargetContext());s.subjects(Collections.singleton("math"));s.p.edit().putInt(pkg+".count",2).putInt("learning.math.mode",1).commit();s.reset(pkg);s.begin(pkg);});
            for(int number=0;number<2;number++){
                Activity a2=startActivitySync(new Intent(getTargetContext(),BreakActivity.class).putExtra("package",pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();final int index=number;
                runOnMainSync(()->{Store s=new Store(getTargetContext());Exercise e=s.problem(pkg);android.view.View root=a2.getWindow().getDecorView();
                    ((android.widget.EditText)find(root,v->v instanceof android.widget.EditText)).setText(e.answers.get(0));
                    find(root,v->v instanceof android.widget.Button && ((android.widget.Button)v).getText().toString().equals(a2.getString(R.string.check))).performClick();
                    require(s.pending(pkg)==(index==0),"multi-question break progress");a2.finish();
                });
            }
            runOnMainSync(()->{Store s=new Store(getTargetContext());s.subjects(Collections.singleton("math"));s.p.edit().putInt(pkg+".count",1).commit();s.reset(pkg);});
            new AppearanceTests(this).run();
            new ProgressTests(this).run();
            results.putString("stream","PASS: bilingual/dark-theme screens, goose hint, reveal persistence, review requires a new unsolved question or a quota-charged skip, no progress on reveal/replacement, all three policies, quota/delay enforcement, idempotent completion, legacy pending math migration,  migration, defaults, selected subjects, per-subject level/mode, persistence, quota, day rollover, all six subjects in both answer modes, two-question break, recall restoration, independent daily limits and goals, viewing and study rollover, errors and timed results.\n");finish(Activity.RESULT_OK,results);
        }catch(Throwable t){results.putString("stream","FAIL: "+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,results);}
    }
}
