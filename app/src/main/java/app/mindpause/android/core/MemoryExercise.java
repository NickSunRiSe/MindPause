package app.mindpause.android.core;

import java.util.*;

/** Recall a sequence after hiding it. Distractors use exactly the same digits. */
public final class MemoryExercise {
    private MemoryExercise() {}
    public static Exercise generate(Random random,int level) {
        List<Character> digits=new ArrayList<>();
        for(char digit='1';digit<='9';digit++)digits.add(digit);
        Collections.shuffle(digits,random);
        int length=4+2*level;
        StringBuilder sequence=new StringBuilder();
        for(int i=0;i<length;i++)sequence.append(digits.get(i));
        String answer=sequence.toString();
        Set<String> options=new LinkedHashSet<>();
        options.add(answer);
        List<Integer> positions=new ArrayList<>();
        for(int i=0;i<length-1;i++)positions.add(i);
        Collections.shuffle(positions,random);
        for(int i=0;i<3;i++){
            char[] alternative=answer.toCharArray();
            int index=positions.get(i);
            alternative[index]=answer.charAt(index+1);
            alternative[index+1]=answer.charAt(index);
            options.add(new String(alternative));
        }
        Exercise result=new Exercise("memory."+answer,Subject.MEMORY,level,"","",
            Collections.singletonList(answer),new ArrayList<>(options),"","","",true,true,false);
        return localize(result,false);
    }
    public static Exercise localize(Exercise e,boolean english) {
        String question=english?"What was the sequence?":"В каком порядке шли цифры?";
        String hint=english?"Recall the digits in pairs.":"Вспомни цифры парами.";
        String explanation=english?"The digits were shown in this order.":"Цифры были показаны в таком порядке.";
        return new Exercise(e.id,e.subject,e.level,english?"Memory":"Память",question,e.answers,e.options,
            hint,explanation,"",true,true,e.choices);
    }
}
