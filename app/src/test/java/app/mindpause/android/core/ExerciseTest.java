package app.mindpause.android.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class ExerciseTest {
    @Test public void memoryLevelsHaveDistinctSequencesAndOneExactAnswer() {
        Random random=new Random(31);
        for(int level=0;level<3;level++){
            Set<String> distinct=new HashSet<>();
            for(int i=0;i<400;i++){
                Exercise exercise=MemoryExercise.generate(random,level).withMode(2,random);
                String digits=exercise.answers.get(0);
                assertEquals(4+2*level,digits.length());
                assertEquals(digits.length(),digits.chars().distinct().count());
                assertEquals(4,new HashSet<>(exercise.options).size());
                assertEquals(1,exercise.options.stream().filter(exercise::accepts).count());
                assertFalse(exercise.accepts("0"+digits));
                Exercise translated=MemoryExercise.localize(exercise,true);
                assertEquals(exercise.options,translated.options);
                assertTrue(translated.choices && translated.accepts(digits));
                distinct.add(digits);
            }
            assertTrue(distinct.size()>=100);
        }
    }
    private QuestionBank bank() throws IOException {return new QuestionBank(new InputStreamReader(new FileInputStream("src/main/assets/questions.tsv"),StandardCharsets.UTF_8));}
    @Test public void everySubjectHasAllLevelsAndUnambiguousOptions() throws Exception {
        QuestionBank bank=bank();int count=0;Set<String> questions=new HashSet<>();Set<String> russianWords=new HashSet<>();
        for(Subject s:Subject.values())if(s!=Subject.MATH && s!=Subject.MEMORY)for(int level=0;level<3;level++) {
            List<Exercise> pool=bank.pool(s,level);assertTrue(s+"/"+level+" must contain at least 100 questions",pool.size()>=100);count+=pool.size();
            for(Exercise e:pool){assertTrue("Repeated prompt: "+e.question,questions.add(e.question));if(s==Subject.RUSSIAN)assertTrue("Repeated Russian word: "+e.answers.get(0),russianWords.add(e.answers.get(0)));assertEquals(1,e.options.stream().filter(e::accepts).count());assertFalse(e.source.isEmpty());for(String a:e.answers)assertTrue(e.id+" rejects "+a,e.accepts("  "+a.toUpperCase(Locale.ROOT)+"  "));}
        }
        assertTrue(count>=1200);
    }
    @Test public void finiteBankCyclesWithoutHangingOrImmediateRepeats() throws Exception {
        List<Exercise> pool=bank().pool(Subject.HISTORY,0);List<String> recent=new ArrayList<>();Random r=new Random(91);
        for(int i=0;i<2000;i++){Exercise e=QuestionBank.choose(pool,recent,r);assertFalse(recent.contains(e.id));recent.add(e.id);while(recent.size()>pool.size()-1)recent.remove(0);}
        List<String> exhausted=new ArrayList<>();for(Exercise e:pool)exhausted.add(e.id);
        assertEquals(exhausted.get(0),QuestionBank.choose(pool,exhausted,r).id);
    }
    @Test public void modesPersistAsConcreteExercisesAndMathOptionsAreUnique() {
        Random r=new Random(17);boolean seenText=false,seenChoices=false;
        for(int i=0;i<1000;i++) {
            Exercise e=Exercise.math(Problem.generate(r,2),2,r);
            assertEquals(4,new HashSet<>(e.options).size());assertEquals(1,e.options.stream().filter(e::accepts).count());
            assertTrue(e.withMode(2,r).choices);assertFalse(e.withMode(1,r).choices);
            if(e.withMode(0,r).choices)seenChoices=true;else seenText=true;
        }assertTrue(seenText&&seenChoices);
    }
    @Test public void spellingIsStrictButNamesCanNormalizeYo() {
        Exercise name=new Exercise("id",Subject.GEOGRAPHY,0,"","",Arrays.asList("Орёл"),Arrays.asList("Орёл","Москва","Курск"),"","","",false,false,false);
        assertTrue(name.accepts("  ОРЕЛ "));assertFalse(name.accepts("Орелл"));assertFalse(name.accepts(""));
        Exercise spelling=new Exercise("id",Subject.RUSSIAN,0,"","",Arrays.asList("всё"),Arrays.asList("всё","все","всю"),"","","",true,false,false);
        assertFalse(spelling.accepts("все"));assertTrue(spelling.accepts("ВСЁ"));
    }
    @Test public void liveServiceEvidenceWinsOverMissingSystemSetting() {
        assertEquals(AccessState.READY,AccessState.resolve(true,false,false));
        assertEquals(AccessState.READY,AccessState.resolve(true,true,true));
        assertEquals(AccessState.WAITING,AccessState.resolve(false,true,false));
        assertEquals(AccessState.WAITING,AccessState.resolve(false,false,true));
        assertEquals(AccessState.OFF,AccessState.resolve(false,false,false));
    }
    @Test public void historySupportsDatesAndShortNamesAndMathHasHundredsOfConditions() throws Exception {
        List<Exercise> history=bank().pool(Subject.HISTORY,0);
        assertTrue(history.stream().anyMatch(e->e.numeric && e.accepts("1941")));
        assertTrue(history.stream().anyMatch(e->!e.numeric && e.accepts("Менделеев")));
        for(int level=0;level<3;level++){
            Set<String> conditions=new HashSet<>();Random r=new Random(734+level);
            for(int i=0;i<10000 && conditions.size()<200;i++)conditions.add(Problem.generate(r,level).question);
            assertEquals(200,conditions.size());
        }
    }
}
