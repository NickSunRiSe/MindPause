package app.mindpause.fixture;
public class FeedActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle state){super.onCreate(state);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        android.widget.TextView t=new android.widget.TextView(this);t.setText("ТЕСТОВАЯ ЛЕНТА\n\nПриложение на переднем плане.\nПауза должна прервать его через заданный интервал.");t.setTextSize(28);t.setPadding(32,120,32,32);setContentView(t);}
}
