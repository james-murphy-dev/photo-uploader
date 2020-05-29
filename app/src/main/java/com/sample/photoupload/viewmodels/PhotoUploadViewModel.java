package com.sample.photoupload.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.dropbox.core.DbxException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.FullAccount;
import com.sample.photoupload.R;
import com.sample.photoupload.data.dropbox.DropboxClientFactory;
import com.sample.photoupload.data.UploadFileTask;
import com.sample.photoupload.data.FileUploadResult;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class PhotoUploadViewModel extends AndroidViewModel {

    private Context context;
    private boolean showLoginMessage = false;
    private static final float  MEGABYTE = 1024L * 1024L;
    private FileUploadResult lastFileUploaded;

    public PhotoUploadViewModel(@NonNull Application application){
        super(application);
        context = application;
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
        SharedPreferences prefs = context.getSharedPreferences("com.sample.photoupload", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token",null);

        if (accessToken==null){
            accessToken = Auth.getOAuth2Token(); //generate Access Token
            if (accessToken!=null){
                prefs.edit().putString("access-token", accessToken).apply();
                DropboxClientFactory.init(accessToken);
                showLoginMessage = true;
            }
        }
        else {
            DropboxClientFactory.init(accessToken);
        }
    }

    public LiveData<FileUploadResult> uploadPhoto(Uri uri) {
        final MutableLiveData<FileUploadResult> photoUploadLiveData = new MutableLiveData<FileUploadResult>();
        final FileUploadResult fileUpload = new FileUploadResult();
        new UploadFileTask(context, DropboxClientFactory.getClient(), new UploadFileTask.Callback() {
            @Override
            public void onUploadComplete(FileMetadata result) {
                //convert file size from kb to mb with two decimal places
                float sizeMb = result.getSize()/MEGABYTE;
                String fileSize =String.format(Locale.US, "%.2f", sizeMb) + " Mb";
                fileUpload.setFileUpload(result);
                fileUpload.setFileSize(fileSize);
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

    public void loginMessageSeen(){
        showLoginMessage = false;
    }
    public boolean showLoginMessage() {
        return showLoginMessage;
    }
}
