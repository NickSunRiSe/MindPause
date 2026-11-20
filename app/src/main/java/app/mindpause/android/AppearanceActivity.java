package app.mindpause.android;

import android.os.Bundle;
import android.widget.*;

public final class AppearanceActivity extends BaseActivity {
    @Override public void onCreate(Bundle state){super.onCreate(state);render();}
    @Override protected void onResume(){super.onResume();refreshAppearance();}
    private void render(){
        LinearLayout page=Ui.page(this);Ui.add(page,getString(R.string.appearance),30,Ui.INK,true);Ui.space(page,12);
        Ui.add(page,getString(R.string.appearance_note),15,Ui.MUTED,false);Ui.space(page,20);
        options(page,getString(R.string.language),new String[]{"system","ru","en"},new String[]{getString(R.string.system),"Русский","English"},Appearance.language(this),value->{Appearance.language(this,value);recreate();});
        options(page,getString(R.string.theme),new String[]{"system","light","dark"},new String[]{getString(R.string.system),getString(R.string.light),getString(R.string.dark)},Appearance.theme(this),value->{Appearance.theme(this,value);recreate();});
        Ui.button(page,getString(R.string.done),true,this::finish);
    }
    private void options(LinearLayout page,String title,String[] values,String[] labels,String selected,java.util.function.Consumer<String> changed){
        LinearLayout card=Ui.card(page,Ui.WHITE);Ui.add(card,title,20,Ui.INK,true);Ui.space(card,8);
        RadioGroup group=new RadioGroup(this);card.addView(group);
        java.util.Map<Integer,String> choices=new java.util.HashMap<>();
        // Preferences are the source of truth after recreation. Never restore old checked
        // states over them, or reuse child IDs across the language and theme groups.
        group.setSaveEnabled(false);
        for(int i=0;i<values.length;i++){RadioButton b=new RadioButton(this);b.setId(android.view.View.generateViewId());b.setSaveEnabled(false);choices.put(b.getId(),values[i]);b.setText(labels[i]);b.setTextSize(17);b.setTextColor(Ui.ink(this));b.setMinHeight(Ui.dp(this,52));group.addView(b);if(values[i].equals(selected))group.check(b.getId());}
        group.setOnCheckedChangeListener((g,id)->{String value=choices.get(id);if(value!=null && !value.equals(selected))changed.accept(value);});
    }
}
