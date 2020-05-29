package com.sample.photoupload;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.dropbox.core.v2.files.FileMetadata;
import com.sample.photoupload.data.FileUpload;
import com.sample.photoupload.viewmodels.PhotoUploadViewModel;

import java.text.DateFormat;

public class MainActivity extends AppCompatActivity {

    private PhotoUploadViewModel viewModel;
    private Button loginButton, uploadButton;
    private Spinner loadingSpinner;
    private static final int PICKFILE_REQUEST_CODE = 1;
    public final static String EXTRA_PATH = "MainActivity_Path";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewModel = new PhotoUploadViewModel(getApplication());

        loadingSpinner = findViewById(R.id.spinner);
        loginButton = findViewById(R.id.login_btn);
        uploadButton = findViewById(R.id.upload_btn);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewModel.login();
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                choosePhotoWithPermission();
            }
        });

    }

    private boolean hasPermissionsForRead() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_DENIED) {
            return false;
        }
        return true;
    }

    private boolean shouldDisplayRationaleForRead() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return true;
        }
        return false;
    }

    private void requestPermissionsForRead() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PICKFILE_REQUEST_CODE
        );
    }

    private void choosePhotoWithPermission() {
        if (hasPermissionsForRead()) {
            launchFilePicker();
            return;
        }

        if (shouldDisplayRationaleForRead()) {
            new AlertDialog.Builder(this)
                    .setMessage("This app requires storage access to upload files.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissionsForRead();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();
        } else {
            requestPermissionsForRead();
        }
    }

    private void launchFilePicker() {
        // Launch intent to pick file for upload
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICKFILE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // This is the result of a call to launchFilePicker
                loadingSpinner.setVisibility(View.VISIBLE);

                viewModel.uploadPhoto(data.getData()).observe(this, new Observer<FileUpload>() {
                    @Override
                    public void onChanged(FileUpload upload) {
                        loadingSpinner.setVisibility(View.GONE);

                        String message = "";

                        if (upload.uploadSuccessful()){
                            FileMetadata fileMetadata = upload.getMetadata();

                             message+= fileMetadata.getName() + " size " + fileMetadata.getSize() + " modified " +
                                    DateFormat.getDateTimeInstance().format(fileMetadata.getClientModified());


                        }
                        else{
                            message+= "Upload failed: "+upload.getError().getMessage();
                        }

                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT)
                                .show();

                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int actionCode, @NonNull String [] permissions, @NonNull int [] grantResults) {

        boolean granted = true;
        for (int i = 0; i < grantResults.length; ++i) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                Log.w("Permission", "User denied " + permissions[i] +
                        " permission to perform file upload");
                granted = false;
                break;
            }
        }

        if (granted) {
            launchFilePicker();
        } else {
            Toast.makeText(this,
                    "Can't upload file: read access denied. " +
                            "Please grant storage permissions to use this functionality.",
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        viewModel.getAccessToken();

        if (viewModel.hasToken()){

            loadingSpinner.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
            viewModel.userLoggedIn().observe(this, new Observer<Boolean>() {
                @Override
                public void onChanged(Boolean loggedIn) {
                    loadingSpinner.setVisibility(View.VISIBLE);
                    if (loggedIn){
                        loginButton.setVisibility(View.GONE);
                        uploadButton.setVisibility(View.VISIBLE);
                    }
                    else{
                        loginButton.setVisibility(View.VISIBLE);
                    }

                }
            });
        }


    }


}
