package app.mindpause.fixture;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

/** Test-only focus owner. Records real Android callbacks without playing sound.
 * Retains focus while covered, like a background player; it is not a Spotify mock.
 */
public final class AudioProbeActivity extends Activity {
    private AudioManager audio;
    private AudioFocusRequest request;
    private boolean requested;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        TextView text=new TextView(this);
        text.setText("AUDIO FOCUS PROBE\n\nSilent test fixture. Logs Android focus changes while Pause opens and closes.");
        text.setTextSize(24);text.setPadding(32,120,32,32);setContentView(text);
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener(change->Log.i("PauseAudioProbe","focus="+change),new Handler(Looper.getMainLooper()))
            .build();
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if(focused && !requested) {
            int result=audio.requestAudioFocus(request);
            requested=result==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            Log.i("PauseAudioProbe","request="+result);
        }
    }
    @Override protected void onDestroy() {
        if(requested)audio.abandonAudioFocusRequest(request);
        super.onDestroy();
    }
}
