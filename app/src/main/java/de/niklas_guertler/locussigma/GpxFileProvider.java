package de.niklas_guertler.locussigma;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class GpxFileProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String fileName = new File(uri.getLastPathSegment()).getName();
        File file = new File(new File(getContext().getCacheDir(), "sharegpx"), fileName);
//        Log.i("GpxFileProvider", "openFile: " + file.getAbsolutePath());
        try {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY /*,
                    new Handler(Looper.getMainLooper()),
                    e -> file.delete() */);
        } catch (IOException e) {
            throw new FileNotFoundException(file.getPath());
        }
    }

    @Override
    public String getType(Uri uri) {
        return "application/gpx+xml";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
