package app.mindpause.android;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import app.mindpause.android.core.*;
import java.util.*;

final class AppearanceTests {
    private final Instrumentation i;
    AppearanceTests(Instrumentation i){this.i=i;}
    private void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private View find(View root,java.util.function.Predicate<View> f){if(f.test(root))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int n=0;n<group.getChildCount();n++){View v=find(group.getChildAt(n),f);if(v!=null)return v;}}return null;}
    private View button(Activity a,int resource){View b=find(a.getWindow().getDecorView(),v->v instanceof Button && ((Button)v).getText().toString().equals(a.getString(resource)));require(b!=null,"Button "+a.getString(resource));return b;}
    private Activity open(boolean demo,String pkg){Activity a=i.startActivitySync(new Intent(i.getTargetContext(),BreakActivity.class).putExtra("demo",demo).putExtra("package",pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();return a;}
    void run()throws Exception{
        String pkg="app.mindpause.fixture";
        for(String lang:new String[]{"ru","en"})for(String theme:new String[]{"light","dark"}){
            i.runOnMainSync(()->{Appearance.language(i.getTargetContext(),lang);Appearance.theme(i.getTargetContext(),theme);});i.waitForIdleSync();
            for(Subject subject:Subject.values())for(int mode:new int[]{1,2}){
                i.runOnMainSync(()->{Store s=new Store(i.getTargetContext());s.subjects(Collections.singleton(subject.id));s.p.edit().putInt("learning."+subject.id+".mode",mode).commit();});
                Activity a=open(true,pkg);
                i.runOnMainSync(()->{
                    require(Appearance.dark(a)==theme.equals("dark"),"Applied theme "+theme);
                    require(a.getResources().getConfiguration().getLocales().get(0).getLanguage().equals(lang),"Applied language "+lang);
                    Exercise e=new Store(a).problem("demo");require(e.subject==subject,"Subject unchanged");
                    if(lang.equals("en")&&subject!=Subject.RUSSIAN)require(!(e.question+e.hint+e.explanation).matches("(?s).*[А-Яа-яЁё].*"),"English task content");
                    if(subject==Subject.MEMORY)button(a,R.string.ready_recall).performClick();
                    button(a,R.string.reveal).performClick();require(button(a,R.string.review_new_question)!=null,"Review action");
                    require(find(a.getWindow().getDecorView(),v->v instanceof TextView && ((TextView)v).getText().toString().equals(e.answers.get(0)))!=null,"Correct answer visible");
                    button(a,R.string.review_new_question).performClick();require(!new Store(a).problem("demo").id.equals(e.id),"Preview gives a different question");a.finish();
                });i.waitForIdleSync();
            }
        }
        new ReviewTests(i).run();
    }
}
