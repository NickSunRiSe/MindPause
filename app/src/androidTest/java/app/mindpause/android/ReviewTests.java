package app.mindpause.android;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import app.mindpause.android.core.*;
import java.util.*;

/** Exercises the persistence boundary, not just the presence of a button. */
final class ReviewTests {
    private final Instrumentation i;
    private final String pkg="app.mindpause.fixture";
    ReviewTests(Instrumentation i){this.i=i;}
    private void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private View find(View root,java.util.function.Predicate<View> f){if(f.test(root))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int n=0;n<group.getChildCount();n++){View v=find(group.getChildAt(n),f);if(v!=null)return v;}}return null;}
    private View button(Activity a,int resource){View b=find(a.getWindow().getDecorView(),v->v instanceof Button && ((Button)v).getText().toString().equals(a.getString(resource)));require(b!=null,"Button "+a.getString(resource));return b;}
    private Activity open(){Activity a=i.startActivitySync(new Intent(i.getTargetContext(),BreakActivity.class).putExtra("package",pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));i.waitForIdleSync();return a;}
    void run()throws Exception{
        i.runOnMainSync(()->{
            Appearance.language(i.getTargetContext(),"en");Store s=new Store(i.getTargetContext());s.subjects(Collections.singleton("english"));s.select(Collections.singleton(pkg));
            s.p.edit().putInt("learning.english.level",1).putInt("learning.english.mode",1).putInt("learning.reviewAction",2).putInt(pkg+".count",2).putInt(pkg+".quota",0).putInt(pkg+".skipDelay",0).commit();s.reset(pkg);s.begin(pkg);
            for(int n=0;!s.problem(pkg).question.contains("goose")&&n<110;n++)s.newProblem(pkg);require(s.problem(pkg).question.contains("goose"),"Goose fixture");
        });
        Activity a=open();
        i.runOnMainSync(()->{
            Store s=new Store(a);int reviewed=s.today("reviewed");
            button(a,R.string.hint).performClick();require(find(a.getWindow().getDecorView(),v->v instanceof TextView&&((TextView)v).getText().toString().contains("vowel inside the root"))!=null,"Rule hint visible");
            button(a,R.string.reveal).performClick();require(s.today("reviewed")==reviewed+1,"Reveal counted once");require(s.pending(pkg)&&s.p.getInt(pkg+".done",0)==0,"Reveal never advances break");
            require(!button(a,R.string.skips_used).isEnabled(),"Zero quota disables skip");
            require(find(a.getWindow().getDecorView(),v->v instanceof EditText)==null,"No input for exposed answer");a.finish();
        });i.waitForIdleSync();
        Activity b=open();
        i.runOnMainSync(()->{
            Store s=new Store(b);Exercise seen=s.problem(pkg);int reviewed=s.today("reviewed"),solved=s.today("solved"),skips=s.today("skips");
            require(s.revealed(pkg,seen.id),"Reveal survives closing Activity");require(s.reveal(pkg,seen.id)&&s.today("reviewed")==reviewed,"Repeated reveal is idempotent");require(!s.completeQuestion(pkg,seen.id),"Exposed answer cannot be submitted as solved");
            button(b,R.string.review_new_question).performClick();Exercise next=s.problem(pkg);require(!next.id.equals(seen.id),"Replacement is different");require(s.pending(pkg)&&s.p.getInt(pkg+".done",0)==0,"Replacement keeps same step");require(s.today("solved")==solved&&s.today("skips")==skips,"Replacement gives no credit and consumes no skip");
            require(s.replaceRevealed(pkg,seen.id)==null&&!s.skipRevealed(pkg,seen.id),"Stale review actions rejected");
            ((EditText)find(b.getWindow().getDecorView(),v->v instanceof EditText)).setText(next.answers.get(0));button(b,R.string.check).performClick();require(s.pending(pkg)&&s.p.getInt(pkg+".done",0)==1,"Only solving replacement advances step");
            button(b,R.string.next_question).performClick();button(b,R.string.reveal).performClick();button(b,R.string.review_new_question).performClick();require(s.p.getInt(pkg+".done",0)==1,"Review at second step preserves first solved step");
            Exercise finalQuestion=s.problem(pkg);((EditText)find(b.getWindow().getDecorView(),v->v instanceof EditText)).setText(finalQuestion.answers.get(0));button(b,R.string.check).performClick();require(!s.pending(pkg)&&s.today("solved")==solved+2,"Two actual solves required");b.finish();
        });i.waitForIdleSync();
        // All three policies, and quota/delay behavior, are enforced in Store as well as UI.
        for(int policy=0;policy<3;policy++){
            final int choice=policy;
            i.runOnMainSync(()->{Store s=new Store(i.getTargetContext());s.p.edit().putInt("learning.reviewAction",choice).putInt(pkg+".quota",3).putInt(pkg+".skipped",0).putInt(pkg+".skipDelay",10).putInt(pkg+".count",1).commit();s.reset(pkg);s.begin(pkg);});
            Activity c=open();
            i.runOnMainSync(()->{
                Store s=new Store(c);String id=s.problem(pkg).id;int before=s.today("skips"),solved=s.today("solved");button(c,R.string.reveal).performClick();
                require(new Store(c).reviewAction()==choice,"Review policy persists");require(!s.skipRevealed(pkg,id),"Review cannot bypass skip delay");
                View skip=find(c.getWindow().getDecorView(),v->v instanceof Button&&((Button)v).getText().toString().startsWith("Skip in"));require((skip!=null)==(choice!=0),"Only configured policies offer skip");if(skip!=null)require(!skip.isEnabled(),"Skip disabled during wait");
                s.p.edit().putLong(pkg+".waited",10000).commit();
                if(choice==0){button(c,R.string.review_new_question).performClick();require(s.pending(pkg),"New-question policy cannot finish break");}
                else{require(s.skipRevealed(pkg,id),"Eligible reviewed skip succeeds");require(!s.pending(pkg)&&s.skipped(pkg)==1&&s.today("skips")==before+1,"Reviewed skip consumes allowance and ends break");require(s.today("solved")==solved,"Skip is not a solution");require(!s.skipRevealed(pkg,id),"Skip cannot be charged twice");}
                c.finish();
            });i.waitForIdleSync();
        }
        i.runOnMainSync(()->{
            Store s=new Store(i.getTargetContext());s.reset(pkg);s.begin(pkg);String id=s.problem(pkg).id;s.reveal(pkg,id);s.p.edit().putInt(pkg+".quota",0).putLong(pkg+".waited",60000).commit();require(!s.skipRevealed(pkg,id),"No skip when quota exhausted");require(s.replaceRevealed(pkg,id)!=null&&s.pending(pkg),"New question remains available without skips");
            s.p.edit().putInt(pkg+".quota",-1).putInt(pkg+".skipDelay",0).commit();id=s.problem(pkg).id;s.reveal(pkg,id);require(s.skipRevealed(pkg,id),"Unlimited skips still available");
            s.subjects(Collections.singleton("math"));s.reset(pkg);s.begin(pkg);s.p.edit().putString(pkg+".problem","{\"t\":\"Разогрев\",\"q\":\"20 + 10 − 3 = ?\",\"a\":27,\"h\":\"подсказка\",\"e\":\"27\"}").commit();Exercise legacy=s.problem(pkg);require(s.completeQuestion(pkg,legacy.id),"Legacy pending math remains solvable");require(!s.completeQuestion(pkg,legacy.id),"Repeated completion rejected");
            s.p.edit().putString("statsDate","2001-01-01").commit();s.increment("solved");require(s.today("reviewed")==0,"Daily review counter resets");s.p.edit().putInt("learning.reviewAction",0).commit();Appearance.language(i.getTargetContext(),"ru");Appearance.theme(i.getTargetContext(),"system");
        });
    }
}
