package com.sample.photoupload.data;

import com.dropbox.core.v2.files.FileMetadata;

public class FileUploadResult {

    private FileMetadata fileUpload;
    private Exception error;
    private boolean uploadSuccess;
    private String fileSizeMb;

    public FileUploadResult(){}

    public FileMetadata getMetadata() {
        return fileUpload;
    }

    public void setFileUpload(FileMetadata fileUpload) {
        this.fileUpload = fileUpload;
        uploadSuccess = true;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
        uploadSuccess = false;
    }

    public boolean uploadSuccessful(){
        return uploadSuccess;
    }

    public String getFileSize() {
        return fileSizeMb;
    }

    public void setFileSize(String fileSizeMb) {
        this.fileSizeMb = fileSizeMb;
    }
}
