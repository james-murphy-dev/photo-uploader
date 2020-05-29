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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.v2.files.FileMetadata;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.sample.photoupload.data.FileUploadResult;
import com.sample.photoupload.viewmodels.PhotoUploadViewModel;

import java.text.DateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PhotoUploadViewModel viewModel;
    private Button loginButton, uploadButton;
    private ProgressBar progressBar;
    private MaterialCardView fileMetatDataLabel;
    private TextView  fileNameLabel, fileSizeLabel, fileDateLabel;
    private static final int PICKFILE_REQUEST_CODE = 1;
    private static final float  MEGABYTE = 1024L * 1024L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewModel = new PhotoUploadViewModel(getApplication());

        progressBar = findViewById(R.id.progress_bar);
        loginButton = findViewById(R.id.login_btn);
        uploadButton = findViewById(R.id.upload_btn);

        fileMetatDataLabel = findViewById(R.id.file_meta_date_rl);

        fileNameLabel = findViewById(R.id.file_name);
        fileSizeLabel = findViewById(R.id.file_size);
        fileDateLabel = findViewById(R.id.file_modified_date);

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
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private boolean shouldDisplayRationaleForRead() {
        return ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE);
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
                progressBar.setVisibility(View.VISIBLE);
                uploadButton.setVisibility(View.GONE);
                fileMetatDataLabel.setVisibility(View.GONE);

                viewModel.uploadPhoto(data.getData()).observe(this, new Observer<FileUploadResult>() {
                    @Override
                    public void onChanged(FileUploadResult upload) {

                        String message = "";

                        if (upload.uploadSuccessful()){
                            final FileMetadata fileMetadata = upload.getMetadata();

                            //convert file size from kb to mb with two decimal places
                            float sizeMb = fileMetadata.getSize()/MEGABYTE;
                            String fileSize =String.format(Locale.US, "%.2f", sizeMb) + " Mb";

                            String modified = "Modified on " +DateFormat.getDateTimeInstance().format(fileMetadata.getClientModified());

                            fileNameLabel.setText(fileMetadata.getName());
                            fileDateLabel.setText(modified);
                            fileSizeLabel.setText(fileSize);

                            fileMetatDataLabel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                  //filemetatadata object does not provide link to download from remote server
                                  //accessing file download could not be accomplished using Dropbox upload method

                                }
                            });

                            progressBar.setVisibility(View.GONE);
                            uploadButton.setVisibility(View.VISIBLE);
                            fileMetatDataLabel.setVisibility(View.VISIBLE);

                             message+= "Upload successful";

                        }
                        else{
                            message+= "Upload failed: "+upload.getError().getMessage();
                        }

                        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
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

            loginButton.setVisibility(View.GONE);

            viewModel.userLoggedIn().observe(this, new Observer<Boolean>() {
                @Override
                public void onChanged(Boolean loggedIn) {
                    if (loggedIn){

                        if (viewModel.showLoginMessage()){
                            //user has logged in during this session
                            Snackbar.make(findViewById(android.R.id.content), "Login success", Snackbar.LENGTH_SHORT)
                                    .show();

                            viewModel.loginMessageSeen();
                            progressBar.setVisibility(View.GONE);
                        }
                        uploadButton.setVisibility(View.VISIBLE);
                        loginButton.setVisibility(View.GONE);
                    }
                    else{
                        loginButton.setVisibility(View.VISIBLE);
                        uploadButton.setVisibility(View.GONE);
                    }

                }
            });
        }
        else{
            loginButton.setVisibility(View.VISIBLE);
        }


    }


}
