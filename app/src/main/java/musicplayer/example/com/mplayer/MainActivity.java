package musicplayer.example.com.mplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_READ_EXT_STORAGE = 1001;
    private ArrayList<Song> songs = new ArrayList<>();
    ;
    private AdapterSongList adapterSongList;
    private SeekBar seek;
    private TextView current_pos, total_pos, details;
    private RecyclerView recyclerView;
    private ImageView prev, next, pause_play, coverc, coverf;
    private MusicService musicService;
    private Intent intent;
    private boolean isBounded = false;
    private boolean isClosed = false;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) iBinder;
            musicService = binder.getService();
            adapterSongList.setList(songs);
            adapterSongList.setService(musicService);
            adapterSongList.notifyDataSetChanged();
            musicService.setContext(MainActivity.this);
            musicService.setList(songs);
            isBounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBounded = false;
        }
    };

    @Override
    protected void onDestroy() {
        if (isBounded)
            stopService(intent);
        musicService = null;
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadMusicDetails();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //START SERVICE
        if (intent == null && checkPermissions()) {
            try {
                intent = new Intent(this, MusicService.class);
                bindService(intent, serviceConnection, BIND_AUTO_CREATE);
                startService(intent);
            } catch (Exception ex) {
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //INIT VIEWS
        coverc = findViewById(R.id.CoverImageCircle);
        coverf = findViewById(R.id.CoverImageFull);
        details = findViewById(R.id.details);
        seek = findViewById(R.id.seek);
        current_pos = findViewById(R.id.current_position);
        total_pos = findViewById(R.id.total_length);
        prev = findViewById(R.id.prev);
        next = findViewById(R.id.next);
        pause_play = findViewById(R.id.pause_play);
        recyclerView = findViewById(R.id.recycler);

        //SET RECYCLER PARAMETERS
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapterSongList = new AdapterSongList(this);
        recyclerView.setAdapter(adapterSongList);
        recyclerView.setHasFixedSize(true);

        //ON CLICK
        pause_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (musicService != null && MusicService.player != null) {
                    if (MusicService.player.isPlaying())
                        MusicService.player.pause();
                    else
                        MusicService.player.start();

                    //DETECT PLAY PAUSE
                    if (MusicService.player.isPlaying()) {
                        pause_play.setImageResource(R.drawable.pause);
                    } else {
                        pause_play.setImageResource(R.drawable.play);
                    }

                }
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (musicService != null && MusicService.player != null) {
                    int position = MusicService.position + 1;
                    musicService.playSong(position);
                }
            }
        });
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (musicService != null && MusicService.player != null) {
                    int position = MusicService.position - 1;
                    musicService.playSong(position);
                }
            }
        });

        //START LOADING SONGS
        SongsFetcher fetcher = new SongsFetcher(MainActivity.this);
        fetcher.execute();

        //CHECK PERMISSIONS
        checkPermissions();

    }

    public void loadMusicDetails() {
        if (musicService != null && MusicService.player != null) {
            Song song = musicService.getCurrentSong();
            if (song != null) {
                //LOAD SONG NAME
                details.setText(song.name);
                details.setSelected(true);

                //DETECT PLAY PAUSE
                if (MusicService.player.isPlaying()) {
                    pause_play.setImageResource(R.drawable.pause);
                } else {
                    pause_play.setImageResource(R.drawable.play);
                }

                //SET PROGRESS DETAILS
                seek.setProgress(0);
                seek.setMax(MusicService.player.getDuration());
                total_pos.setText(Utilities.getTime(Integer.parseInt(song.duration)));
                seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if (MusicService.player != null && MusicService.player.isPlaying()) {
                            MusicService.player.seekTo(seekBar.getProgress());
                        } else {
                            seek.setProgress(0);
                        }
                    }
                });

                //START SEEKBAR THREAD
                startSeekBar();

                //LOAD IMAGES
                Uri Location_Uri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.ID);
                Bitmap image = Utilities.getBitMap(MainActivity.this, Location_Uri);
                if (image == null) {
                    coverc.setImageResource(R.drawable.ic_launcher);
                    coverf.setImageDrawable(getResources().getDrawable(R.drawable.background_main_player));
                } else {
                    coverc.setImageBitmap(image);
                    coverf.setImageDrawable(new BitmapDrawable(getResources(), image));
                }

                //MAKE IMAGE ROTATE
                RotateAnimation rotateAnimation = new RotateAnimation(0, 360f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setInterpolator(new LinearInterpolator());
                rotateAnimation.setDuration(25000);
                rotateAnimation.setRepeatCount(Animation.INFINITE);
                findViewById(R.id.CoverImageCircle).startAnimation(rotateAnimation);
            }
        }
    }

    public void startSeekBar() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                int Total = MusicService.player.getDuration();
                int Current = 0;
                int current_Position = MusicService.position;
                while (Current < Total) {
                    try {
                        Thread.sleep(200);
                        Current = MusicService.player.getCurrentPosition();
                        if (MusicService.position != current_Position) {
                            Current = Total + 1000;
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                seek.setProgress(MusicService.player.getCurrentPosition());
                                current_pos.setText(Utilities.getTime((int) MusicService.player.getCurrentPosition()));
                            }
                        });
                        Log.d("ThreadsCount", "run: " + Thread.activeCount());
                    } catch (InterruptedException | IllegalStateException e) {
                        //ERROR WHILE SEEKING
                    }
                }
            }
        };
        thread.start();
    }

    @SuppressLint("StaticFieldLeak")
    class SongsFetcher extends AsyncTask<Void, Void, Void> {

        private ProgressDialog progressDialog;
        private Context context;

        SongsFetcher(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setCancelable(false);
            progressDialog.setTitle("Fetching");
            progressDialog.setMessage("Loading Songs Please Wait...");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ContentResolver musicResolver = getContentResolver();
            Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
            if (musicCursor != null && musicCursor.moveToFirst()) {
                int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                while (musicCursor.moveToNext()) {
                    Song song = new Song();
                    song.ID = musicCursor.getLong(idColumn);
                    song.name = musicCursor.getString(titleColumn);
                    song.artist = musicCursor.getString(artistColumn);
                    song.duration = musicCursor.getString(durationColumn);
                    songs.add(song);
                }
                musicCursor.close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();

        }
    }

    @SuppressLint("InlinedApi")
    private boolean checkPermissions() {
        String[] P = {Manifest.permission.READ_EXTERNAL_STORAGE};
        int status = ContextCompat.checkSelfPermission(getApplicationContext(), P[0]);
        if (status != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, P, REQUEST_CODE_READ_EXT_STORAGE);
            return false;
        }
        return true;
    }

    @Override
    @SuppressLint("InlinedApi")
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_READ_EXT_STORAGE) {
            String[] P = {Manifest.permission.READ_EXTERNAL_STORAGE};
            int status = ContextCompat.checkSelfPermission(getApplicationContext(), P[0]);
            if (status != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(musicService, "Permission Denied...", Toast.LENGTH_SHORT).show();
            } else {
                //FETCH ALL SONGS
                SongsFetcher fetcher = new SongsFetcher(this);
                fetcher.execute();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isClosed)
            super.onBackPressed();
        else {
            new AlertDialog.Builder(this).setTitle("Music Will be Stopped").setMessage("Are you sure want to Exit?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            isClosed = true;
                            onBackPressed();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            isClosed = false;
                        }
                    })
                    .setCancelable(false)
                    .create().show();
        }
    }
}