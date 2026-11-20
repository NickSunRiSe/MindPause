package app.mindpause.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import java.util.*;
import app.mindpause.android.core.AccessState;
import app.mindpause.android.core.Subject;

public final class MainActivity extends BaseActivity {
    private Store store;
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private LinearLayout accessCard;
    private TextView heroStatus;
    private AccessState lastAccess;
    private final Runnable statusTick=new Runnable(){ @Override public void run(){ refreshAccess(); handler.postDelayed(this,1000); }};
    @Override public void onCreate(Bundle state) { super.onCreate(state); store=new Store(this); }
    @Override protected void onResume() { super.onResume(); if(refreshAppearance())return;render(); handler.post(statusTick); }
    @Override protected void onPause() { handler.removeCallbacks(statusTick); super.onPause(); }
    private String summary() {
        if(!store.enabled())return getString(R.string.paused);
        if(store.apps().isEmpty())return getString(R.string.pick_first);
        if(AccessStatus.read(this)!=AccessState.READY)return getString(R.string.connect_first);
        if(store.goalMet())return getString(R.string.goal_complete);
        if(!store.inSchedule())return getString(R.string.outside_schedule);
        return getString(R.string.ready_summary);
    }
    private void refreshAccess() {
        if(accessCard==null)return;
        if(heroStatus!=null)heroStatus.setText(summary());
        AccessState state=AccessStatus.read(this);if(state==lastAccess)return;lastAccess=state;
        accessCard.removeAllViews();
        if(state==AccessState.READY) {
            Ui.add(accessCard,getString(R.string.access_ready),20,Ui.INK,true);Ui.space(accessCard,8);
            Ui.add(accessCard,getString(R.string.access_ready_body),15,Ui.MUTED,false);
            Ui.button(accessCard,getString(R.string.try_break),false,()->startActivity(new Intent(this,BreakActivity.class).putExtra("demo",true)));
        } else if(state==AccessState.WAITING) {
            Ui.add(accessCard,getString(R.string.access_waiting),19,Ui.INK,true);Ui.space(accessCard,8);
            Ui.add(accessCard,getString(R.string.access_waiting_body),15,Ui.MUTED,false);
            Ui.button(accessCard,getString(R.string.open_service),true,this::openAccessSettings);
        } else {
            Ui.add(accessCard,getString(R.string.access_off),20,Ui.INK,true);Ui.space(accessCard,8);
            Ui.add(accessCard,getString(R.string.access_off_body),15,Ui.MUTED,false);
            Ui.button(accessCard,getString(R.string.enable_access),true,this::permission);
        }
    }
    private void render() {
        LinearLayout page=Ui.page(this);
        Ui.add(page,getString(R.string.brand),15,Ui.INK,true); Ui.space(page,24);
        Ui.add(page,getString(R.string.headline),32,Ui.INK,true); Ui.space(page,10);
        Ui.add(page,getString(R.string.tagline),16,Ui.MUTED,false); Ui.space(page,24);
        LinearLayout hero=Ui.card(page,Ui.INK);
        Switch enabled=new Switch(this); enabled.setText(getString(R.string.smart_breaks)); enabled.setTextSize(18); enabled.setTextColor(Ui.WHITE); enabled.setChecked(store.enabled()); enabled.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,8)); hero.addView(enabled);
        enabled.setOnCheckedChangeListener((b,value)->{store.p.edit().putBoolean("enabled",value).apply(); render();});
        heroStatus=Ui.add(hero,summary(),15,Ui.LIME,false);
        Ui.space(hero,18);
        Ui.add(hero,getString(R.string.statistics,store.today("solved"),store.today("reviewed"),store.today("skips")),16,Ui.WHITE,true);
        Ui.add(hero,getString(R.string.today),12,Ui.LIME,false);
        if(store.goal()>0)Ui.add(hero,getString(R.string.goal_progress,store.today("solved"),store.goal()),14,Ui.LIME,false);
        Ui.button(page,getString(R.string.study_stats),false,()->startActivity(new Intent(this,StatsActivity.class)));
        accessCard=Ui.card(page,Ui.WHITE);lastAccess=null;refreshAccess();
        Ui.space(page,12);
        Ui.add(page,getString(R.string.where),21,Ui.INK,true); Ui.space(page,6);
        Ui.add(page,getString(R.string.rhythm),14,Ui.MUTED,false); Ui.space(page,16);
        List<String> selected=new ArrayList<>(store.apps()); selected.sort(Comparator.comparing(this::label));
        if(selected.isEmpty()) {
            LinearLayout empty=Ui.card(page,Ui.WHITE);
            Ui.add(empty,getString(R.string.empty_title),18,Ui.INK,true); Ui.space(empty,6);
            Ui.add(empty,getString(R.string.empty_body),15,Ui.MUTED,false);
        }
        for(String pkg:selected) {
            LinearLayout row=Ui.card(page,Ui.WHITE);
            Ui.add(row,label(pkg),20,Ui.INK,true); Ui.space(row,6);
            String quota=store.quota(pkg)==-1?getString(R.string.unlimited):getString(R.string.per_day,store.quota(pkg));
            Ui.add(row,getString(R.string.app_rule,store.interval(pkg),store.count(pkg),quota),14,Ui.MUTED,false);
            long left=Math.max(0,store.interval(pkg)*60-store.used(pkg)/1000);
            if(!store.goalMet()&&!store.limited(pkg))Ui.add(row,store.pending(pkg)?getString(R.string.pending_question):left<=60?getString(R.string.next_break_seconds,left):getString(R.string.next_break,(left+59)/60),13,Ui.MUTED,false);
            if(store.limit(pkg)>0)Ui.add(row,getString(R.string.limit_progress,
                Texts.duration(this,store.viewing.today(pkg,java.time.LocalDate.now().toString())),store.limit(pkg)),14,Ui.MUTED,false);
            Ui.button(row,getString(R.string.configure),false,()->ruleDialog(pkg));
        }
        Ui.button(page,getString(R.string.pick_apps),true,this::appPicker);
        Ui.space(page,22);
        Ui.add(page,getString(R.string.subjects_title),21,Ui.INK,true);Ui.space(page,8);
        List<String> names=new ArrayList<>();for(Subject s:Subject.values())if(store.subjects().contains(s.id))names.add(Texts.subject(this,s));
        Ui.add(page,android.text.TextUtils.join(" · ",names),15,Ui.MUTED,false);
        Ui.button(page,getString(R.string.subjects_settings),false,()->startActivity(new Intent(this,SubjectsActivity.class)));
        Ui.button(page,getString(R.string.try_question),false,()->startActivity(new Intent(this,BreakActivity.class).putExtra("demo",true)));
        Ui.button(page,getString(R.string.appearance),false,()->startActivity(new Intent(this,AppearanceActivity.class)));
        Ui.space(page,16);
        LinearLayout advanced=Ui.column(this);
        Button expand=Ui.button(page,getString(R.string.advanced),false,()->advanced.setVisibility(advanced.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
        page.addView(advanced);advanced.setVisibility(View.GONE);
        Ui.button(advanced,getString(R.string.daily_goal),false,this::goalDialog);
        Ui.button(advanced,getString(R.string.schedule),false,this::scheduleDialog);
        Ui.button(advanced,getString(R.string.break_music),false,this::musicDialog);
        Ui.button(advanced,getString(R.string.system_access),false,this::openAccessSettings);
        Ui.button(advanced,getString(R.string.how_it_works),false,this::help);
        Ui.space(page,24); Ui.add(page,getString(R.string.footer,BuildConfig.VERSION_NAME),12,Ui.MUTED,false);
    }
    private void musicDialog(){
        LinearLayout content=Ui.column(this);content.setPadding(Ui.dp(this,20),Ui.dp(this,8),Ui.dp(this,20),0);
        RadioGroup group=new RadioGroup(this);content.addView(group);
        RadioButton unchanged=new RadioButton(this),pause=new RadioButton(this);
        unchanged.setId(View.generateViewId());pause.setId(View.generateViewId());
        unchanged.setText(R.string.music_unchanged);pause.setText(R.string.music_pause);
        for(RadioButton b:new RadioButton[]{unchanged,pause}){b.setTextColor(Ui.ink(this));b.setTextSize(17);b.setMinHeight(Ui.dp(this,52));group.addView(b);}
        group.check(store.pauseMedia()?pause.getId():unchanged.getId());
        Ui.space(content,10);Ui.add(content,getString(R.string.music_note),15,Ui.MUTED,false);
        new AlertDialog.Builder(this).setTitle(R.string.break_music).setView(content)
            .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.save,(d,w)->store.p.edit().putBoolean("pauseMedia",group.getCheckedRadioButtonId()==pause.getId()).apply()).show();
    }
    private String label(String pkg) { try { return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString(); } catch(Exception e){return pkg;} }
    private void permission() {
        if(AccessStatus.read(this)!=AccessState.OFF){refreshAccess();return;}
        new AlertDialog.Builder(this).setTitle(getString(R.string.allow_breaks))
            .setMessage(getString(R.string.permission_body))
            .setNegativeButton(getString(R.string.not_now),null).setPositiveButton(getString(R.string.open_settings),(d,w)->openAccessSettings()).show();
    }
    private void openAccessSettings() {
        try {startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
        catch(android.content.ActivityNotFoundException e){Toast.makeText(this,getString(R.string.access_help),Toast.LENGTH_LONG).show();}
    }
    private void appPicker() {
        LinearLayout content=Ui.column(this); content.setPadding(Ui.dp(this,20),Ui.dp(this,8),Ui.dp(this,20),0);
        EditText search=new EditText(this); search.setSingleLine(); search.setHint(getString(R.string.search_apps)); content.addView(search);
        ListView list=new ListView(this); list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE); content.addView(list,new LinearLayout.LayoutParams(-1,Ui.dp(this,330)));
        List<ResolveInfo> available=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
        TreeMap<String,String> names=new TreeMap<>(); Set<String> excluded=new HashSet<>(); excluded.add(getPackageName()); excluded.add("com.android.settings");
        for(ResolveInfo home:getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0)) excluded.add(home.activityInfo.packageName);
        for(ResolveInfo info:available) if(!excluded.contains(info.activityInfo.packageName)) names.put(info.activityInfo.packageName,info.loadLabel(getPackageManager()).toString());
        List<String> packages=new ArrayList<>(names.keySet()); packages.sort(Comparator.comparing(names::get,String.CASE_INSENSITIVE_ORDER));
        Set<String> picked=store.apps(); picked.retainAll(names.keySet()); List<String> visible=new ArrayList<>();
        Runnable filter=()->{
            String query=search.getText().toString().toLowerCase(Locale.ROOT); visible.clear(); List<String> labels=new ArrayList<>();
            for(String pkg:packages) if(names.get(pkg).toLowerCase(Locale.ROOT).contains(query) || pkg.toLowerCase(Locale.ROOT).contains(query)) { visible.add(pkg); labels.add(names.get(pkg)); }
            list.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_multiple_choice,labels)); list.clearChoices();
            for(int i=0;i<visible.size();i++)list.setItemChecked(i,picked.contains(visible.get(i)));
        };
        list.setOnItemClickListener((p,v,pos,id)->{if(list.isItemChecked(pos))picked.add(visible.get(pos));else picked.remove(visible.get(pos));});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){filter.run();}public void afterTextChanged(Editable e){}});
        filter.run();
        new AlertDialog.Builder(this).setTitle(getString(R.string.your_apps)).setView(content).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.save),(d,w)->{store.select(picked);render();}).show();
    }
    private Spinner spinner(LinearLayout parent,String title,String[] items,int selection) {
        Ui.space(parent,14); Ui.add(parent,title,14,Ui.MUTED,true);
        Spinner s=new Spinner(this); ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items); s.setAdapter(a); s.setSelection(selection); s.setMinimumHeight(Ui.dp(this,48)); parent.addView(s); return s;
    }
    private int index(int[] values,int v) { for(int i=0;i<values.length;i++)if(values[i]==v)return i; return 0; }
    private void ruleDialog(String pkg) {
        ScrollView scroll=new ScrollView(this); LinearLayout form=Ui.column(this); form.setPadding(Ui.dp(this,24),0,Ui.dp(this,24),Ui.dp(this,16));scroll.addView(form);
        int[] intervals={1,3,5,10,15,20,30,45,60}, quotas={0,1,3,5,-1}, delays={0,10,30,60};
        Spinner interval=spinner(form,getString(R.string.interval),new String[]{getString(R.string.minute_1),getString(R.string.minute_3),getString(R.string.minute_5),getString(R.string.minute_10),getString(R.string.minute_15),getString(R.string.minute_20),getString(R.string.minute_30),getString(R.string.minute_45),getString(R.string.minute_60)},index(intervals,store.interval(pkg)));
        Ui.space(form,12);Ui.add(form,getString(R.string.subject_setting_help),13,Ui.MUTED,false);
        EditText dailyLimit=numberSetting(form,R.string.daily_limit,store.limit(pkg));
        Ui.add(form,getString(R.string.limit_help),13,Ui.MUTED,false);
        Spinner count=spinner(form,getString(R.string.questions_per_break),new String[]{getString(R.string.question_1),getString(R.string.question_2),getString(R.string.question_3)},store.count(pkg)-1);
        Spinner quota=spinner(form,getString(R.string.skip_limit),new String[]{getString(R.string.no_skips),getString(R.string.skip_1),getString(R.string.skip_3),getString(R.string.skip_5),getString(R.string.no_limit)},index(quotas,store.quota(pkg)));
        Spinner delay=spinner(form,getString(R.string.skip_delay),new String[]{getString(R.string.immediately),getString(R.string.seconds_10),getString(R.string.seconds_30),getString(R.string.after_minute)},index(delays,store.skipDelay(pkg)));
        Ui.space(form,12); Ui.add(form,getString(R.string.rule_help),13,Ui.MUTED,false);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(label(pkg)).setView(scroll)
            .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            int limit=validated(dailyLimit,1440);
            if(limit<0)return;
            store.p.edit().putInt(pkg+".dailyLimit",limit).putInt(pkg+".minutes",intervals[interval.getSelectedItemPosition()]).putInt(pkg+".count",count.getSelectedItemPosition()+1).putInt(pkg+".quota",quotas[quota.getSelectedItemPosition()]).putInt(pkg+".skipDelay",delays[delay.getSelectedItemPosition()]).apply();store.reset(pkg);dialog.dismiss();render();
        }));
        dialog.show();
    }
    private EditText numberSetting(LinearLayout form,int label,int current) {
        Ui.add(form,getString(label),15,Ui.INK,true);
        EditText field=new EditText(this);
        field.setSingleLine();
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        field.setHint(R.string.setting_off);
        if(current>0)field.setText(String.format(getResources().getConfiguration().getLocales().get(0),"%d",current));
        form.addView(field);
        return field;
    }
    private int validated(EditText field,int max) {
        String value=field.getText().toString().trim();
        if(value.isEmpty())return 0;
        try {
            int n=Integer.parseInt(value);
            if(n>=0 && n<=max)return n;
        } catch(NumberFormatException ignored) {}
        field.setError(getString(R.string.setting_range,max));
        return -1;
    }
    private void goalDialog() {
        LinearLayout form=Ui.column(this);
        form.setPadding(Ui.dp(this,24),Ui.dp(this,10),Ui.dp(this,24),0);
        EditText field=numberSetting(form,R.string.goal_count,store.goal());
        Ui.add(form,getString(R.string.goal_help),14,Ui.MUTED,false);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.daily_goal).setView(form)
            .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            int goal=validated(field,999);
            if(goal<0)return;
            store.setGoal(goal);dialog.dismiss();render();
        }));
        dialog.show();
    }
    private void scheduleDialog() {
        LinearLayout form=Ui.column(this); form.setPadding(Ui.dp(this,24),Ui.dp(this,8),Ui.dp(this,24),Ui.dp(this,16));
        Switch schedule=new Switch(this);schedule.setText(getString(R.string.selected_hours));schedule.setChecked(store.p.getBoolean("schedule",false));form.addView(schedule);
        int[] times={store.p.getInt("start",9*60),store.p.getInt("end",23*60)};
        Button start=Ui.button(form,getString(R.string.schedule_from,formatTime(times[0])),false,()->{}),end=Ui.button(form,getString(R.string.schedule_to,formatTime(times[1])),false,()->{});
        start.setOnClickListener(v->new TimePickerDialog(this,(t,h,m)->{times[0]=h*60+m;start.setText(getString(R.string.schedule_from,formatTime(times[0])));},times[0]/60,times[0]%60,true).show());
        end.setOnClickListener(v->new TimePickerDialog(this,(t,h,m)->{times[1]=h*60+m;end.setText(getString(R.string.schedule_to,formatTime(times[1])));},times[1]/60,times[1]%60,true).show());
        Ui.space(form,12);String[] names={getString(R.string.monday),getString(R.string.tuesday),getString(R.string.wednesday),getString(R.string.thursday),getString(R.string.friday),getString(R.string.saturday),getString(R.string.sunday)};CheckBox[] days=new CheckBox[7];int mask=store.p.getInt("days",127);
        for(int i=0;i<7;i++){days[i]=new CheckBox(this);days[i].setText(names[i]);days[i].setChecked((mask&(1<<i))!=0);form.addView(days[i]);}
        Ui.add(form,getString(R.string.schedule_help),12,Ui.MUTED,false);
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(getString(R.string.schedule_title)).setView(scroll).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.save),null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{int bits=0;for(int i=0;i<7;i++)if(days[i].isChecked())bits|=1<<i;
            if(schedule.isChecked()&&bits==0){Toast.makeText(this,getString(R.string.pick_day),Toast.LENGTH_SHORT).show();return;}
            store.p.edit().putBoolean("schedule",schedule.isChecked()).putInt("start",times[0]).putInt("end",times[1]).putInt("days",bits).apply();dialog.dismiss();render();}));dialog.show();
    }
    private String formatTime(int value) {return String.format(Locale.ROOT,"%02d:%02d",value/60,value%60);}
    private void help() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.help_title)).setMessage(getString(R.string.help_body))
            .setNeutralButton(getString(R.string.app_info),(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).setPositiveButton(getString(R.string.understood),null).show();
    }
}
