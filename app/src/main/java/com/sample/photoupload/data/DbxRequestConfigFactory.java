package com.sample.photoupload.data;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;

public class DbxRequestConfigFactory {
    private static DbxRequestConfig sDbxRequestConfig;

    public static DbxRequestConfig getRequestConfig() {
        if (sDbxRequestConfig == null) {
            sDbxRequestConfig = DbxRequestConfig.newBuilder("photo-upload")
                    .build();
        }
        return sDbxRequestConfig;
    }
}
