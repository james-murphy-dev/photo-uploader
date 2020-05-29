package com.sample.photoupload.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URI;

public class UploadFileTask extends AsyncTask<Uri, Void, FileMetadata> {

    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;
    private final String REMOTE_DIR = "/photo_upload_sample";
    private WeakReference<Context> contextRef;

    public interface Callback {
        void onUploadComplete(FileMetadata result);
        void onError(Exception e);
    }

    public UploadFileTask(Context context, DbxClientV2 dbxClient, Callback callback) {
        mDbxClient = dbxClient;
        mCallback = callback;
        contextRef = new WeakReference<>(context);
    }

    @Override
    protected void onPostExecute(FileMetadata result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else if (result == null) {
            mCallback.onError(null);
        } else {
            mCallback.onUploadComplete(result);
        }
    }

    @Override
    protected FileMetadata doInBackground(Uri... params) {
        Uri uri = params[0];
        if (uri != null && contextRef.get()!=null) {

            Context context = contextRef.get();

            Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);

            try{
                returnCursor.moveToFirst();
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                String filename = returnCursor.getString(nameIndex);

                try (InputStream inputStream =  context.getContentResolver().openInputStream(uri)) {

                    returnCursor.close();
                    return mDbxClient.files().uploadBuilder(REMOTE_DIR + "/" + filename)
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(inputStream);
                } catch (DbxException | IOException e) {
                    mException = e;
                }

            } catch (NullPointerException e){
                mException = e;
            }


        }

        return null;
    }
}