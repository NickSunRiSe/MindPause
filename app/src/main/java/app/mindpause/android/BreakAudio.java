package app.mindpause.android;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

/** Opt-in player pause. Permanent loss deliberately does not promise automatic resume. */
final class BreakAudio {
    private final AudioManager manager;
    private AudioFocusRequest request;
    BreakAudio(Context context){manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);}
    void pausePlayers(){
        if(request!=null)return;
        AudioFocusRequest next=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            // Respect another player/call taking focus; never repeatedly take it back.
            .setOnAudioFocusChangeListener(change->{}).build();
        if(manager.requestAudioFocus(next)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED)request=next;
    }
    void release(){if(request!=null){manager.abandonAudioFocusRequest(request);request=null;}}
}
