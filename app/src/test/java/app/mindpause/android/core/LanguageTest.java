package app.mindpause.android.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LanguageTest {
    private QuestionBank bank(String suffix)throws Exception{return new QuestionBank(new InputStreamReader(new FileInputStream("src/main/assets/questions"+suffix+".tsv"),StandardCharsets.UTF_8));}
    @Test public void completeEnglishBankPreservesAnswerAndOptionPositions()throws Exception{
        QuestionBank ru=bank(""),en=bank("-en");Random random=new Random(48);int count=0;
        for(Subject s:Subject.values())if(s!=Subject.MATH && s!=Subject.MEMORY)for(int level=0;level<3;level++){
            assertEquals(ru.pool(s,level).size(),en.pool(s,level).size());
            for(Exercise canonical:ru.pool(s,level)){
                Exercise translation=en.byId(canonical.id);assertNotNull(translation);assertEquals(canonical.options.size(),translation.options.size());
                for(int i=0;i<canonical.options.size();i++)assertEquals(canonical.accepts(canonical.options.get(i)),translation.accepts(translation.options.get(i)));
                Exercise saved=canonical.withMode(2,random),english=ExerciseLanguage.project(saved,canonical,translation),restored=ExerciseLanguage.project(english,translation,canonical);
                assertEquals(saved.options,restored.options);assertTrue(english.choices);assertEquals(saved.id,english.id);
                for(String a:translation.answers)assertTrue(translation.accepts(a.toUpperCase(Locale.ROOT)));
                if(s!=Subject.RUSSIAN)assertFalse((translation.question+translation.hint+translation.explanation+translation.options).matches("(?s).*[А-Яа-яЁё].*"));
                count++;
            }
        }assertEquals(1231,count);
    }
    @Test public void mathTranslationKeepsProblemAndAnswerAcrossBothLanguages(){
        Random r=new Random(912);Set<String> types=new HashSet<>();
        for(int i=0;i<3000;i++){
            Exercise original=Exercise.math(Problem.generate(r,2),2,r).withMode(2,r);types.add(original.title);
            Exercise en=ExerciseLanguage.math(original,true),back=ExerciseLanguage.math(en,false);
            assertFalse((en.question+en.hint+en.explanation).matches("(?s).*[А-Яа-яЁё].*"));
            assertEquals(original.question,back.question);assertEquals(original.id,en.id);assertEquals(original.answers,en.answers);assertEquals(original.options,en.options);assertTrue(en.accepts(original.answers.get(0)));
        }assertEquals(8,types.size());
    }
    @Test public void irregularPluralHintExplainsPatternWithoutSpellingTheAnswer()throws Exception{
        for(String suffix:new String[]{"","-en"}){
            Exercise goose=bank(suffix).pool(Subject.ENGLISH,1).stream().filter(e->e.question.contains("goose")).findFirst().get();
            assertFalse(goose.hint.matches("(?s).*\\boo\\b.*"));assertFalse(goose.hint.matches("(?s).*\\bee\\b.*"));assertFalse(goose.hint.contains("geese"));
            assertTrue(goose.hint.contains(suffix.isEmpty()?"гласного":"vowel"));
            assertTrue(goose.explanation.contains("oo"));assertTrue(goose.explanation.contains("ee"));assertTrue(goose.explanation.contains("geese"));
            assertTrue(goose.accepts("geese"));assertFalse(goose.accepts("gooses"));
        }
    }
    @Test public void grammarHintsAreSeparateFromWorkedAnswers()throws Exception{
        for(String suffix:new String[]{"","-en"})for(Exercise e:bank(suffix).pool(Subject.ENGLISH,2)){
            if(e.question.startsWith(suffix.isEmpty()?"Заполни пропуск:":"Fill the gap:"))assertNotEquals(e.id,e.explanation,e.hint);
        }
    }
    @Test public void equationTranslationPreservesNegativeRightHandSide(){
        Exercise e=Exercise.math(new Problem("","3x − 28 = -19\nЧему равен x?",3,"",""),2,new Random(1));
        assertEquals("x = (-19 + 28) ÷ 3 = 3.",ExerciseLanguage.math(e,true).explanation);
    }
}
