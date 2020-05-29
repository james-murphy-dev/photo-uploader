package com.sample.photoupload.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.dropbox.core.DbxException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.FullAccount;
import com.sample.photoupload.MainActivity;
import com.sample.photoupload.R;
import com.sample.photoupload.data.DropboxClientFactory;
import com.sample.photoupload.data.UploadFileTask;
import com.sample.photoupload.data.FileUpload;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static com.sample.photoupload.MainActivity.EXTRA_PATH;

public class PhotoUploadViewModel extends AndroidViewModel {

    private Context context;


    public PhotoUploadViewModel(@NonNull Application application){
        super(application);
        context = application;
        String path = getIntent(context, "").getStringExtra(EXTRA_PATH);

    }

    public Intent getIntent(Context context, String path) {
        Intent filesIntent = new Intent(context, MainActivity.class);
        filesIntent.putExtra(MainActivity.EXTRA_PATH, path);
        return filesIntent;
    }

    public void login(){
        Auth.startOAuth2Authentication(context, context.getString(R.string.dropbox_api_key));
    }

    public boolean hasToken() {
        SharedPreferences prefs = context.getSharedPreferences("com.sample.photoupload", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        return accessToken != null;
    }

    public void getAccessToken() {
        DbxCredential credential = Auth.getDbxCredential();

        SharedPreferences prefs = context.getSharedPreferences("com.sample.photoupload", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token",null);

        if (accessToken==null){
            accessToken = Auth.getOAuth2Token(); //generate Access Token
            if (accessToken!=null){
                prefs.edit().putString("access-token", accessToken).apply();
                DropboxClientFactory.init(accessToken);
            }
        }
        else {
            DropboxClientFactory.init(accessToken);
        }
    }

    public LiveData<FileUpload> uploadPhoto(Uri uri) {
        final MutableLiveData<FileUpload> photoUploadLiveData = new MutableLiveData();
        final FileUpload fileUpload = new FileUpload();
        new UploadFileTask(context, DropboxClientFactory.getClient(), new UploadFileTask.Callback() {
            @Override
            public void onUploadComplete(FileMetadata result) {
                fileUpload.setFileUpload(result);
                photoUploadLiveData.setValue(fileUpload);
            }

            @Override
            public void onError(Exception e) {
                fileUpload.setError(e);
                photoUploadLiveData.setValue(fileUpload);
            }
        }).execute(uri);

        return photoUploadLiveData;
    }

    public LiveData<Boolean> userLoggedIn() {

        final MutableLiveData<Boolean> userLoggedIn = new MutableLiveData<>();

        new Thread(){
            @Override
            public void run() {
                try{
                    FullAccount account = DropboxClientFactory.getClient().users().getCurrentAccount();
                    userLoggedIn.postValue(true);
                }catch (DbxException e){
                    userLoggedIn.postValue(false);
                }
            }
        }.start();

        return userLoggedIn;

    }
}
