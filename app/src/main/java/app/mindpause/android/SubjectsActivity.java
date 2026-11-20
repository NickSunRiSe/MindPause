package app.mindpause.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import app.mindpause.android.core.Subject;
import java.util.*;

public final class SubjectsActivity extends BaseActivity {
    private Store store;
    @Override public void onCreate(Bundle state){super.onCreate(state);store=new Store(this);render();}
    @Override protected void onResume(){super.onResume();refreshAppearance();}
    private void render() {
        LinearLayout page=Ui.page(this);Ui.add(page,getString(R.string.subjects_title),30,Ui.INK,true);Ui.space(page,10);
        Ui.add(page,getString(R.string.subjects_intro),16,Ui.MUTED,false);
        Ui.space(page,12);Ui.add(page,getString(R.string.subjects_autosave),13,Ui.MUTED,false);
        Ui.button(page,getString(R.string.easy_preset),true,()->{store.preset();render();Toast.makeText(this,getString(R.string.preset_applied),Toast.LENGTH_SHORT).show();});
        Ui.space(page,20);
        LinearLayout review=Ui.card(page,Ui.WHITE);
        choose(review,getString(R.string.review_setting),new String[]{getString(R.string.review_setting_new),getString(R.string.review_setting_skip),getString(R.string.review_setting_ask)},store.reviewAction(),n->store.p.edit().putInt("learning.reviewAction",n).apply());
        Ui.add(review,getString(R.string.review_setting_help),13,Ui.MUTED,false);
        List<String> apps=new ArrayList<>(store.apps());Collections.sort(apps);String sample=apps.isEmpty()?"demo":apps.get(0);
        for(Subject subject:Subject.values()) {
            LinearLayout card=Ui.card(page,Ui.WHITE);Switch toggle=new Switch(this);toggle.setText(Texts.subject(this,subject));toggle.setTextSize(20);toggle.setTextColor(Ui.ink(this));toggle.setChecked(store.subjects().contains(subject.id));card.addView(toggle);
            LinearLayout options=Ui.column(this);card.addView(options);options.setVisibility(toggle.isChecked()?View.VISIBLE:View.GONE);
            choose(options,getString(R.string.difficulty),new String[]{getString(R.string.basic),getString(R.string.medium),getString(R.string.hard)},store.level(subject,sample),n->store.p.edit().putInt("learning."+subject.id+".level",n).apply());
            choose(options,getString(R.string.answer_mode),new String[]{getString(R.string.mixed),getString(R.string.type_answer),getString(R.string.choices)},store.mode(subject),n->store.p.edit().putInt("learning."+subject.id+".mode",n).apply());
            Ui.add(options,description(subject),13,Ui.MUTED,false);
            toggle.setOnCheckedChangeListener((button,checked)->{
                Set<String> selected=store.subjects();
                if(checked)selected.add(subject.id);else selected.remove(subject.id);
                if(selected.isEmpty()){toggle.setChecked(true);Toast.makeText(this,getString(R.string.keep_subject),Toast.LENGTH_SHORT).show();return;}
                store.subjects(selected);options.setVisibility(checked?View.VISIBLE:View.GONE);
            });
        }
        Ui.button(page,getString(R.string.try_subjects),true,()->startActivity(new android.content.Intent(this,BreakActivity.class).putExtra("demo",true)));
        Ui.button(page,getString(R.string.done),false,this::finish);
    }
    private String description(Subject s) {
        if(s==Subject.MEMORY)return getString(R.string.memory_description);
        String size=s==Subject.MATH?"":" "+getString(R.string.bank_size,store.bankSize(s,0),store.bankSize(s,1),store.bankSize(s,2));
        switch(s) {
            case MATH:return getString(R.string.math_description);
            case RUSSIAN:return getString(R.string.russian_description)+size;
            case ENGLISH:return getString(R.string.english_description)+size;
            case HISTORY:return getString(R.string.history_description)+size;
            default:return getString(R.string.geography_description)+size;
        }
    }
    private void choose(LinearLayout parent,String title,String[] items,int initial,java.util.function.IntConsumer save) {
        Ui.space(parent,12);Ui.add(parent,title,14,Ui.MUTED,true);
        Spinner spinner=new Spinner(this);spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items));spinner.setSelection(initial);spinner.setMinimumHeight(Ui.dp(this,52));parent.addView(spinner);
        int[] last={initial};spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> a){}
            public void onItemSelected(android.widget.AdapterView<?> a,View v,int position,long id){if(position!=last[0]){last[0]=position;save.accept(position);}}
        });
    }
}
