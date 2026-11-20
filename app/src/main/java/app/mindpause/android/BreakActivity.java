package app.mindpause.android;

import android.content.Intent;
import android.os.*;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.*;
import app.mindpause.android.core.Exercise;
import app.mindpause.android.core.Subject;

public final class BreakActivity extends BaseActivity {
    private Store store;
    private String pkg;
    private boolean demo, solved, resumed, reviewing, complete;
    private Exercise problem;
    private EditText answer;
    private TextView feedback;
    private Button skip;
    private long lastTick;
    private boolean timingFocus;
    private BreakAudio audio;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){@Override public void run(){
        if(!resumed || solved)return;
        if(!demo && (!store.enabled() || !store.apps().contains(pkg) || !store.inSchedule() || store.goalMet() || !store.pending(pkg))){finish();return;}
        accountVisibleTime();
        if(problem!=null && !store.subjects().contains(problem.subject.id)){problem=store.newProblem(pkg);reviewing=false;render();}
        updateSkip();handler.postDelayed(this,500);
    }};
    private void accountVisibleTime() {
        long now=SystemClock.elapsedRealtime();
        long delta=now-lastTick;
        lastTick=now;
        if(!resumed || !timingFocus || demo || solved || delta<=0 || delta>=2500 || !store.pending(pkg))return;
        PowerManager power=getSystemService(PowerManager.class);
        android.app.KeyguardManager guard=getSystemService(android.app.KeyguardManager.class);
        if(!power.isInteractive() || guard.isKeyguardLocked())return;
        android.content.SharedPreferences.Editor e=store.p.edit();
        e.putLong(pkg+".waited",store.p.getLong(pkg+".waited",0)+delta);
        if(!reviewing)e.putLong(pkg+".solveMs",store.p.getLong(pkg+".solveMs",0)+delta);
        e.apply();
    }
    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        accountVisibleTime();
        timingFocus=focus;
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);store=new Store(this);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::leave);
        if(state!=null && state.getSerializable("exercise")!=null){
            demo=state.getBoolean("demo");pkg=state.getString("package");solved=state.getBoolean("solved");complete=state.getBoolean("complete");reviewing=state.getBoolean("reviewing");
            problem=store.localize((Exercise)state.getSerializable("exercise"));if(solved || reviewing)renderResult();else render();
        }else configure(getIntent());
    }
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);state.putSerializable("exercise",problem);state.putString("package",pkg);state.putBoolean("demo",demo);state.putBoolean("solved",solved);state.putBoolean("complete",complete);state.putBoolean("reviewing",reviewing);
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);configure(intent);}
    private void configure(Intent intent){
        demo=intent.getBooleanExtra("demo",false);pkg=demo?"demo":intent.getStringExtra("package");
        if(pkg==null || (!demo && (!store.pending(pkg) || !store.apps().contains(pkg)))){finish();return;}
        if(demo)store.reset(pkg);solved=false;problem=store.problem(pkg);reviewing=store.revealed(pkg,problem.id);if(reviewing)renderResult();else render();
    }
    @Override protected void onResume(){
        super.onResume();if(refreshAppearance())return;if(reviewing)renderResult();resumed=true;timingFocus=hasWindowFocus();lastTick=SystemClock.elapsedRealtime();handler.removeCallbacks(ticker);handler.post(ticker);
        if(store.pauseMedia()){if(audio==null)audio=new BreakAudio(this);audio.pausePlayers();}
    }
    @Override protected void onPause(){accountVisibleTime();resumed=false;handler.removeCallbacksAndMessages(null);if(audio!=null)audio.release();super.onPause();}
    private void render(){
        LinearLayout page=Ui.page(this);Ui.add(page,getString(demo?R.string.demo_banner:R.string.break_banner),12,Ui.MUTED,true);Ui.space(page,30);
        Ui.add(page,getString(R.string.feed_can_wait),32,Ui.INK,true);Ui.space(page,10);
        String app;try{app=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception e){app=getString(R.string.fallback_app);}
        Ui.add(page,demo?getString(R.string.demo_intro):getString(R.string.break_intro,app),16,Ui.MUTED,false);Ui.space(page,24);
        LinearLayout card=Ui.card(page,Ui.INK);Ui.add(card,Texts.subject(this,problem.subject)+" · "+Texts.level(this,problem.level),12,Ui.LIME,true);Ui.space(card,18);
        boolean memorize=problem.subject==Subject.MEMORY && !problem.id.equals(store.p.getString(pkg+".recall",""));
        if(memorize){
            answer=null;
            Ui.add(card,getString(R.string.remember_digits),18,Ui.WHITE,false);
            Ui.add(card,problem.answers.get(0),34,Ui.WHITE,true);
            Ui.button(page,getString(R.string.ready_recall),true,()->{
                store.p.edit().putString(pkg+".recall",problem.id).apply();
                render();
            });
            addExitActions(page);
            return;
        }
        Ui.add(card,problem.question,27,Ui.WHITE,true);Ui.space(card,18);Ui.add(card,getString(R.string.question_number,store.p.getInt(pkg+".done",0)+1,demo?1:store.count(pkg)),13,Ui.LIME,false);
        answer=new EditText(this);answer.setHint(getString(problem.numeric?R.string.number_answer:R.string.short_answer));answer.setTextSize(24);answer.setTextColor(Ui.ink(this));answer.setHintTextColor(Ui.muted(this));answer.setInputType(problem.numeric?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);answer.setSingleLine();answer.setSelectAllOnFocus(true);answer.setPadding(Ui.dp(this,16),Ui.dp(this,14),Ui.dp(this,16),Ui.dp(this,14));answer.setBackground(Ui.bg(Ui.WHITE,16,this));
        if(problem.choices){for(String option:problem.options)Ui.button(page,option,false,()->check(option));}else page.addView(answer,new LinearLayout.LayoutParams(-1,-2));
        feedback=Ui.add(page,getString(R.string.no_rush),14,Ui.MUTED,false);feedback.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,4));
        if(!problem.choices)Ui.button(page,getString(R.string.check),true,()->check(answer.getText().toString()));
        answer.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);answer.setOnEditorActionListener((v,id,event)->{
            if(event==null || event.getAction()==android.view.KeyEvent.ACTION_DOWN && event.getRepeatCount()==0)check(answer.getText().toString());
            return true;
        });
        Ui.button(page,getString(R.string.hint),false,()->{feedback.setText(problem.hint);feedback.setTextColor(Ui.muted(this));feedback.announceForAccessibility(problem.hint);});
        Ui.button(page,getString(R.string.reveal),false,()->{if(solved || reviewing)return;accountVisibleTime();if(!store.reveal(pkg,problem.id)){finish();return;}reviewing=true;hideKeyboard();renderResult();});
        Ui.space(page,6);Ui.add(page,getString(R.string.reveal_note),13,Ui.MUTED,false);
        addExitActions(page);
    }
    private void addExitActions(LinearLayout page) {
        Ui.space(page,12);skip=Ui.button(page,getString(R.string.skip),false,()->{if(demo || store.skip(pkg))finish();else updateSkip();});updateSkip();
        Ui.button(page,getString(demo?R.string.close_demo:R.string.stop_viewing),false,this::leave);Ui.space(page,16);Ui.add(page,getString(demo?R.string.demo_note:R.string.leave_note),12,Ui.MUTED,false);
    }
    private void updateSkip(){
        if(skip==null)return;if(demo){skip.setText(getString(R.string.skip_demo));return;}
        long left=Math.max(0,store.skipDelay(pkg)*1000L-store.p.getLong(pkg+".waited",0));boolean available=store.canSkip(pkg);skip.setEnabled(available && left==0);skip.setAlpha(available && left==0?1f:.55f);
        skip.setText(!available?getString(R.string.skips_used):left>0?getString(R.string.skip_wait,(left+999)/1000):store.quota(pkg)<0?getString(reviewing?R.string.review_skip:R.string.skip):getString(reviewing?R.string.review_skip_remaining:R.string.skip_remaining,store.quota(pkg)-store.skipped(pkg)));
    }
    private void hideKeyboard(){if(answer!=null)((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(answer.getWindowToken(),0);}
    private void check(String input){
        if(solved || reviewing)return;
        if(!problem.accepts(input)){if(!demo && !input.trim().isEmpty())store.answerError(pkg,problem);feedback.setText(getString(input.trim().isEmpty()?R.string.empty_answer:R.string.wrong_answer));feedback.setTextColor(Ui.error(this));answer.selectAll();feedback.announceForAccessibility(feedback.getText());return;}
        accountVisibleTime();hideKeyboard();if(!demo && !store.completeQuestion(pkg,problem.id)){finish();return;}
        solved=true;complete=demo || !store.pending(pkg);handler.removeCallbacks(ticker);renderResult();
    }
    private void renderResult(){
        skip=null;LinearLayout page=Ui.page(this);Ui.space(page,32);Ui.add(page,getString(reviewing?R.string.review_banner:R.string.great),14,Ui.MUTED,true);Ui.space(page,20);
        Ui.add(page,getString(reviewing?R.string.review_title:R.string.success_title),34,Ui.INK,true);Ui.space(page,24);
        LinearLayout card=Ui.card(page,Ui.LIME);Ui.add(card,getString(R.string.correct_answer),14,Ui.ON_ACCENT,false);Ui.space(card,8);Ui.add(card,problem.answers.get(0),30,Ui.ON_ACCENT,true);Ui.space(card,14);Ui.add(card,problem.explanation,20,Ui.ON_ACCENT,false);
        Ui.space(page,16);
        if(reviewing){
            int action=store.reviewAction();
            Ui.add(page,getString(demo?R.string.review_demo_note:action==0?R.string.review_note_new:action==1?R.string.review_note_skip:R.string.review_note_ask),17,Ui.MUTED,false);
            Runnable replace=()->{if(!reviewing)return;Exercise next=store.replaceRevealed(pkg,problem.id);if(next==null){finish();return;}problem=next;reviewing=false;solved=false;render();lastTick=SystemClock.elapsedRealtime();handler.removeCallbacks(ticker);handler.post(ticker);};
            if(action!=1 || demo)Ui.button(page,getString(R.string.review_new_question),true,replace);
            if(action!=0 && !demo){skip=Ui.button(page,getString(R.string.review_skip),action==1,()->{if(store.skipRevealed(pkg,problem.id))finish();else updateSkip();});updateSkip();}
            if(action==1 && !demo)Ui.button(page,getString(R.string.review_new_question),false,replace);
            Ui.button(page,getString(demo?R.string.close_demo:R.string.stop_viewing),false,this::leave);
            return;
        }
        Ui.add(page,getString(!demo && store.goalMet()?R.string.goal_complete:complete?R.string.complete_note:R.string.next_note),17,Ui.MUTED,false);
        Ui.button(page,getString(complete?(demo?R.string.done:R.string.continue_viewing):R.string.next_question),true,()->{
            if(complete){finish();return;}solved=false;problem=store.problem(pkg);render();lastTick=SystemClock.elapsedRealtime();handler.removeCallbacks(ticker);handler.post(ticker);
        });
        if(!demo)Ui.button(page,getString(R.string.stop_viewing),false,this::leave);
    }
    private void leave(){if(!demo)startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));finish();}
    @android.annotation.SuppressLint("GestureBackNavigation") @Override public void onBackPressed(){leave();}
}
