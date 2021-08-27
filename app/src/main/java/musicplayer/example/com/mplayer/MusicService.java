package musicplayer.example.com.mplayer;

import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;

import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    public static MediaPlayer player = null;
    public static int position = 0;
    private ArrayList<Song> songs;
    private final IBinder musicBind = new MusicBinder();
    private Context context = null;

    public void setList(ArrayList<Song> theSongs) {
        songs = theSongs;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Song getCurrentSong() {
        return songs.get(position);
    }

    public void playSong(int position) {
        if (position >= songs.size())
            position = 0;
        if (position < 0) {
            position = songs.size() - 1;
        }
        MusicService.position = position;
        try {
            player.reset();
            Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songs.get(position).ID);
            player.setDataSource(getApplicationContext(), trackUri);
            player.prepareAsync();
        } catch (Exception e) {
            //ERROR PLAYING MEDIA
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        position = 0;
        if (player == null) {
            player = new MediaPlayer();
            player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setOnPreparedListener(this);
            player.setOnCompletionListener(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        position = position + 1;
        playSong(position);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        if (context != null) {
            ((MainActivity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((MainActivity) context).loadMusicDetails();
                }
            });
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.reset();
        player.release();
        return false;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
}
