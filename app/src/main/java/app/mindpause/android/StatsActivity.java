package app.mindpause.android;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.LinearLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import app.mindpause.android.core.Subject;

public final class StatsActivity extends BaseActivity {
    private Store store;
    private LocalDate date;
    private boolean allDays;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        store=new Store(this);
        date=state==null?LocalDate.now():LocalDate.parse(state.getString("date"));
        allDays=state!=null&&state.getBoolean("allDays");
    }
    @Override protected void onResume(){super.onResume();if(!refreshAppearance())render();}
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);state.putString("date",date.toString());state.putBoolean("allDays",allDays);
    }
    private void render(){
        LinearLayout page=Ui.page(this);
        Ui.add(page,getString(R.string.study_stats),30,Ui.INK,true);
        Ui.space(page,18);
        LinearLayout watch=Ui.card(page,Ui.WHITE);
        Ui.add(watch,getString(R.string.view_total),19,Ui.INK,true);
        long total=0;
        List<String> packages=new ArrayList<>(store.viewing.packages());
        packages.sort((a,b)->Long.compare(store.viewing.total(b),store.viewing.total(a)));
        for(String pkg:packages)total+=store.viewing.total(pkg);
        Ui.add(watch,Texts.duration(this,total),28,Ui.INK,true);
        for(String pkg:packages){
            String label=pkg;
            try {label=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}
            catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
            Ui.add(watch,label+" · "+Texts.duration(this,store.viewing.total(pkg)),16,Ui.INK,false);
        }
        Ui.space(watch,8);
        Ui.add(watch,getString(R.string.tracking_since),13,Ui.MUTED,false);

        String formatted=date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            .withLocale(getResources().getConfiguration().getLocales().get(0)));
        Ui.add(page,allDays?getString(R.string.all_study_days):formatted,22,Ui.INK,true);
        Ui.button(page,getString(R.string.choose_day),false,this::pickDay);
        Ui.button(page,getString(allDays?R.string.today:R.string.all_study_days),false,()->{
            allDays=!allDays;date=LocalDate.now();render();
        });
        List<StudyHistory.Day> days=new ArrayList<>();
        if(allDays)for(String day:store.history.dates())days.add(store.history.day(day));
        else days.add(store.history.day(date.toString()));
        Ui.space(page,16);
        long solved=0,reviewed=0,skips=0,wrong=0,timed=0,ms=0;
        for(StudyHistory.Day d:days){
            solved+=d.total(StudyHistory.SOLVED);reviewed+=d.total(StudyHistory.REVIEWED);
            skips+=d.total(StudyHistory.SKIPS);wrong+=d.total(StudyHistory.WRONG);
            timed+=d.total("timed");ms+=d.total(StudyHistory.TIME);
        }
        if(solved+reviewed+skips+wrong==0){
            Ui.add(page,getString(R.string.empty_study_day),16,Ui.MUTED,false);
        } else {
            Ui.add(page,getString(R.string.study_counts,solved,reviewed,skips,wrong),17,Ui.INK,true);
            if(timed>0)Ui.add(page,getString(R.string.avg_time,Texts.duration(this,ms/timed)),16,Ui.MUTED,false);
            Ui.space(page,16);
            for(Subject subject:Subject.values())addSubject(page,days,subject);
        }
        Ui.button(page,getString(R.string.done),false,this::finish);
    }

    private void addSubject(LinearLayout page,List<StudyHistory.Day> days,Subject subject){
        LinearLayout card=null;
        for(int level=0;level<3;level++){
            long solved=0,reviewed=0,skips=0,wrong=0,ms=0;
            for(StudyHistory.Day d:days){
                JSONObject g=d.group(subject,level);
                if(g==null)continue;
                solved+=g.optLong(StudyHistory.SOLVED);reviewed+=g.optLong(StudyHistory.REVIEWED);
                skips+=g.optLong(StudyHistory.SKIPS);wrong+=g.optLong(StudyHistory.WRONG);ms+=g.optLong(StudyHistory.TIME);
            }
            if(solved+reviewed+skips+wrong==0)continue;
            if(card==null){
                card=Ui.card(page,Ui.WHITE);
                Ui.add(card,Texts.subject(this,subject),20,Ui.INK,true);
            }
            Ui.space(card,10);
            Ui.add(card,Texts.level(this,level),15,Ui.INK,true);
            Ui.add(card,getString(R.string.study_counts,solved,reviewed,skips,wrong),15,Ui.MUTED,false);
            if(solved>0)Ui.add(card,getString(R.string.avg_time,Texts.duration(this,ms/solved)),15,Ui.MUTED,false);
        }
    }
    private void pickDay(){
        DatePickerDialog picker=new DatePickerDialog(this,(v,y,m,d)->{
            date=LocalDate.of(y,m+1,d);allDays=false;render();
        },date.getYear(),date.getMonthValue()-1,date.getDayOfMonth());
        picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();
    }
}
