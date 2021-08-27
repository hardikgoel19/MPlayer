package musicplayer.example.com.mplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

public class Utilities {

    public static Bitmap getBitMap(Context context, Uri Location) {
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(context, Location);
            byte[] file = mediaMetadataRetriever.getEmbeddedPicture();
            return BitmapFactory.decodeByteArray(file, 0, file.length);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressLint("DefaultLocale")
    public static String getTime(int duration) {
        duration = duration / 1000;
        int Minutes = (int) duration / 60;
        int Seconds = (int) duration % 60;
        return String.format("%02d:%02d",Minutes,Seconds);
    }

}
