package app.mindpause.android.core;

import java.io.*;
import java.util.*;

public final class QuestionBank {
    private final List<Exercise> all=new ArrayList<>();
    public QuestionBank(Reader reader) throws IOException {
        BufferedReader lines=new BufferedReader(reader);String line;
        Set<String> ids=new HashSet<>();
        while((line=lines.readLine())!=null) {
            if(line.trim().isEmpty() || line.startsWith("#"))continue;
            String[] v=line.split("\t",-1);
            if(v.length!=9 || !ids.add(v[0]))throw new IOException("Invalid question row: "+line);
            Subject subject=Subject.byId(v[1]); int level=Integer.parseInt(v[2]);
            List<String> answers=Arrays.asList(v[4].split("\\|")), options=Arrays.asList(v[5].split("\\|"));
            Exercise e=new Exercise(v[0],subject,level,subject.label,v[3],answers,options,v[6],v[7],v[8],
                subject==Subject.RUSSIAN || subject==Subject.ENGLISH,answers.stream().allMatch(a->a.matches("-?\\d+")),false);
            long correct=options.stream().filter(e::accepts).count();
            if(level<0 || level>2 || options.size()<3 || correct!=1 || options.stream().map(e::normalize).distinct().count()!=options.size())
                throw new IOException("Invalid answers for "+v[0]);
            all.add(e);
        }
    }
    public List<Exercise> pool(Subject subject,int level) {
        List<Exercise> pool=new ArrayList<>();for(Exercise e:all)if(e.subject==subject && e.level==level)pool.add(e);return pool;
    }
    public Exercise byId(String id) {for(Exercise e:all)if(e.id.equals(id))return e;return null;}
    /** Recycle the oldest questions when a finite pool is exhausted, never rejection-loop. */
    public static Exercise choose(List<Exercise> pool,List<String> recent,Random random) {
        if(pool.isEmpty())throw new IllegalArgumentException("Empty subject pool");
        List<Exercise> available=new ArrayList<>(pool);
        for(String id:recent)available.removeIf(e->e.id.equals(id));
        for(int i=0;available.isEmpty() && i<recent.size();i++) {
            String oldest=recent.get(i);for(Exercise e:pool)if(e.id.equals(oldest))available.add(e);
        }
        if(available.isEmpty())available.addAll(pool);
        return available.get(random.nextInt(available.size()));
    }
}
