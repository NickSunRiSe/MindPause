package app.mindpause.android.core;

import java.text.Normalizer;
import java.util.*;

/** Short, deterministic answers. No essay evaluation or network calls. */
public final class Exercise implements java.io.Serializable {
    public final String id, title, question, hint, explanation, source;
    public final Subject subject;
    public final int level;
    public final List<String> answers, options;
    public final boolean strictSpelling, numeric, choices;
    public Exercise(String id, Subject subject, int level, String title, String question,
                    List<String> answers, List<String> options, String hint, String explanation,
                    String source, boolean strictSpelling, boolean numeric, boolean choices) {
        this.id=id;this.subject=subject;this.level=level;this.title=title;this.question=question;
        this.answers=Collections.unmodifiableList(new ArrayList<>(answers));
        this.options=Collections.unmodifiableList(new ArrayList<>(options));
        this.hint=hint;this.explanation=explanation;this.source=source;
        this.strictSpelling=strictSpelling;this.numeric=numeric;this.choices=choices;
    }
    public String normalize(String value) {
        String result=Normalizer.normalize(value,Normalizer.Form.NFKC).trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
        return strictSpelling?result:result.replace('ё','е');
    }
    public boolean accepts(String input) {
        if(input==null || input.trim().isEmpty()) return false;
        if(subject==Subject.MEMORY)return input.trim().equals(answers.get(0));
        if(numeric) { try { return Integer.parseInt(input.trim())==Integer.parseInt(answers.get(0)); } catch(NumberFormatException e){return false;} }
        for(String answer:answers) if(normalize(answer).equals(normalize(input))) return true;
        return false;
    }
    public Exercise withMode(int mode, Random random) {
        List<String> shuffled=new ArrayList<>(options);Collections.shuffle(shuffled,random);
        return new Exercise(id,subject,level,title,question,answers,shuffled,hint,explanation,source,
            strictSpelling,numeric,mode==2 || mode==0 && random.nextBoolean());
    }
    public static Exercise math(Problem p, int level, Random random) {
        Set<Integer> values=new LinkedHashSet<>(); values.add(p.answer);
        while(values.size()<4) values.add(p.answer+random.nextInt(21)-10);
        List<String> options=new ArrayList<>(); for(int n:values)options.add(Integer.toString(n));
        return new Exercise(p.question,Subject.MATH,level,p.title,p.question,Collections.singletonList(Integer.toString(p.answer)),
            options,p.hint,p.explanation,"",false,true,false);
    }
}
