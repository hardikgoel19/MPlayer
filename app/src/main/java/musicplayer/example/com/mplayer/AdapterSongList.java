package musicplayer.example.com.mplayer;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class AdapterSongList extends RecyclerView.Adapter<AdapterSongList.ViewHolder> {

    private Context context;
    private ArrayList<Song> songs;
    private MusicService musicservice = null;

    AdapterSongList(Context context) {
        this.context = context;
    }

    public void setList(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public void setService(MusicService service){
        this.musicservice = service;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.song_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        //SET SONG NAME
        holder.songName.setText(songs.get(position).name);
        //holder.songName.setSelected(true);

        //SET SONG DURATION
        holder.songDuration.setText(Utilities.getTime(Integer.parseInt(songs.get(position).duration)));
        //holder.songDuration.setSelected(true);

        //SET SONG ALBUM
        holder.songArtist.setText(songs.get(position).artist);
        //holder.songArtist.setSelected(true);

        //SET SONG IMAGE
        long UriPath = songs.get(position).ID;
        Uri Location_Uri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, UriPath);
        Bitmap image = Utilities.getBitMap(context, Location_Uri);
        if (image == null) {
            holder.songImage.setImageResource(R.drawable.ic_launcher);
        } else {
            holder.songImage.setImageBitmap(image);
        }

        //SET ON CLICK LISTENER
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(musicservice != null){
                    musicservice.setContext(context);
                    musicservice.playSong(position);
                }
                else{
                    Toast.makeText(context, "No Service", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs == null ? 0 : songs.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        LinearLayout layout;
        TextView songName, songDuration, songArtist;
        ImageView songImage;

        ViewHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.layout);
            songName = itemView.findViewById(R.id.songName);
            songDuration = itemView.findViewById(R.id.songDuration);
            songArtist = itemView.findViewById(R.id.songArtist);
            songImage = itemView.findViewById(R.id.songImage);
        }
    }
}
