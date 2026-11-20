package app.mindpause.android.core;

public enum Subject {
    MATH("math", "Математика"), RUSSIAN("russian", "Русский язык"),
    ENGLISH("english", "Английский"), HISTORY("history", "История"), GEOGRAPHY("geography", "География"), MEMORY("memory", "Память");
    public final String id, label;
    Subject(String id, String label) { this.id=id; this.label=label; }
    public static Subject byId(String id) {
        for(Subject s:values()) if(s.id.equals(id)) return s;
        throw new IllegalArgumentException("Unknown subject: "+id);
    }
}
