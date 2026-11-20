package app.mindpause.android.core;

import java.util.*;
import java.util.regex.*;

/** Language is a projection: IDs, answer mode and option order stay stable. */
public final class ExerciseLanguage {
    public static Exercise project(Exercise saved,Exercise canonical,Exercise translated){
        List<String> options=new ArrayList<>();
        for(String value:saved.options){int index=canonical.options.indexOf(value);if(index<0)index=translated.options.indexOf(value);options.add(index<0?value:translated.options.get(index));}
        return copy(saved,translated.question,translated.answers,options,translated.hint,translated.explanation);
    }
    private static Exercise copy(Exercise e,String q,List<String>a,List<String>o,String h,String x){return new Exercise(e.id,e.subject,e.level,e.title,q,a,o,h,x,e.source,e.strictSpelling,e.numeric,e.choices);}
    public static Exercise math(Exercise e,boolean en){
        String q=e.id,h=e.hint,x=e.explanation;List<Integer> ns=new ArrayList<>();Matcher matcher=Pattern.compile("-?\\d+").matcher(q);while(matcher.find())ns.add(Integer.parseInt(matcher.group()));
        int answer=Integer.parseInt(e.answers.get(0));
        if(q.startsWith("В блокноте")){
            int pages=ns.get(0),count=ns.get(1),target=ns.get(2);
            h=en?"Multiply pages per notebook by notebooks filled. Subtract that from the target.":"Найди число заполненных страниц и вычти его из цели.";
            x=target+" − "+pages+" × "+count+" = "+answer+".";
            if(en)q="A notebook has "+pages+" pages. You filled "+count+" notebooks.\nHow many more pages will reach "+target+"?";
        }else if(q.startsWith("Каждый раз")){
            int step=ns.get(1)-ns.get(0);h=en?"Subtract a number from the next one to find the constant step.":"Посчитай разность между соседними числами.";
            x=(en?"The step is ":"Шаг равен ")+step+". "+ns.get(3)+" + "+step+" = "+answer+".";
            if(en)q="The same number is added each time:\n"+q.substring(q.indexOf('\n')+1);
        }else if(q.startsWith("Найди ")){
            int percent=ns.get(0),total=ns.get(1);h=en?(percent==20?"20% is one fifth: divide by 5.":percent==25?"25% is one quarter: divide by 4.":percent==50?"50% is one half: divide by 2.":"10% is one tenth: divide by 10."):(percent==20?"20% — это пятая часть: раздели на 5.":percent==25?"25% — это четверть: раздели на 4.":percent==50?"50% — это половина: раздели на 2.":"10% — это десятая часть: раздели на 10.");
            x=total+" × "+percent+" ÷ 100 = "+answer+".";if(en)q="Find "+percent+"% of "+total+".";
        }else if(q.startsWith("x +")){
            h=en?"Subtract the known addend from the total.":"Вычти известное слагаемое из суммы.";x=ns.get(1)+" − "+ns.get(0)+" = "+answer+".";
        }else if(q.contains("x −")){
            h=en?"Add "+ns.get(1)+" to both sides, then divide by "+ns.get(0)+".":"Прибавь "+ns.get(1)+" к обеим частям, затем раздели на "+ns.get(0)+".";
            x="x = ("+ns.get(2)+" + "+ns.get(1)+") ÷ "+ns.get(0)+" = "+answer+".";
        }else if(q.startsWith("(")){
            int div=ns.get(0)/ns.get(1),sum=div+ns.get(2);h=en?"Do the division and addition inside the parentheses first.":"Сначала выполни деление и сложение внутри скобок.";
            x=ns.get(0)+" ÷ "+ns.get(1)+" = "+div+"; "+div+" + "+ns.get(2)+" = "+sum+"; "+sum+" × "+ns.get(3)+" = "+answer+".";
        }else if(q.contains("×")){
            int product=ns.get(0)*ns.get(1);h=en?"Multiply before adding.":"Умножение выполняется раньше сложения.";x=ns.get(0)+" × "+ns.get(1)+" = "+product+"; "+product+" + "+ns.get(2)+" = "+answer+".";
        }else{
            int sum=ns.get(0)+ns.get(1);h=en?"Add the first two numbers, then subtract the third.":"Сначала сложи два числа, затем вычти третье.";x=ns.get(0)+" + "+ns.get(1)+" = "+sum+"; "+sum+" − "+ns.get(2)+" = "+answer+".";
        }
        if(en)q=q.replace("Чему равен x?","What is x?");
        return copy(e,q,e.answers,e.options,h,x);
    }
}
