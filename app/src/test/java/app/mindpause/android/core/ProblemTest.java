package app.mindpause.android.core;
import java.util.*;
import java.util.regex.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProblemTest {
    @Test public void generatedAnswersMatchIndependentCalculations() {
        Random r=new Random(71);Set<String> types=new HashSet<>();
        for(int difficulty=0;difficulty<3;difficulty++)for(int i=0;i<10000;i++){
            Problem p=Problem.generate(r,difficulty);types.add(p.title);
            Matcher m=Pattern.compile("-?\\d+").matcher(p.question);List<Integer> n=new ArrayList<>();while(m.find())n.add(Integer.valueOf(m.group()));
            int answer;
            switch(p.title){
                case "Разогрев":answer=n.get(0)+n.get(1)-n.get(2);break;
                case "Порядок действий":answer=n.get(0)*n.get(1)+n.get(2);break;
                case "Найди число":answer=n.get(1)-n.get(0);break;
                case "В уме, как в жизни":answer=n.get(2)-n.get(0)*n.get(1);break;
                case "Найди закономерность":answer=n.get(3)+(n.get(1)-n.get(0));break;
                case "Проценты без калькулятора":answer=n.get(0)*n.get(1)/100;break;
                case "Два шага":answer=(n.get(0)/n.get(1)+n.get(2))*n.get(3);break;
                case "Маленькое уравнение":answer=(n.get(2)+n.get(1))/n.get(0);break;
                default:throw new AssertionError(p.title);
            }
            assertEquals(p.question,answer,p.answer);assertTrue(p.accepts(" "+answer+" "));assertFalse(p.accepts(Integer.toString(answer+1)));assertFalse(p.hint.isEmpty());assertFalse(p.explanation.isEmpty());
        }
        assertEquals(8,types.size());
    }
    @Test public void rejectsMalformedAnswers(){Problem p=new Problem("","",10,"","");for(String s:new String[]{"","ten","10.0","99999999999999","1 0","10x"})assertFalse(p.accepts(s));}
}
