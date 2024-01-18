package me.ag2s.cronet.okhttp;

import androidx.annotation.NonNull;

import org.chromium.net.UrlResponseInfo;

import java.io.IOException;

public interface DownLoadInterface {
    /**
     * Notify  download completed and success
     */
    public abstract void onSuccess();

    /**
     * Notify  download Progress
     *
     * @param write bytes written to file
     * @param total Total bytes the file download,if server headers don't have Content-Header ,it will be -1.
     */
    public abstract void onProgress(long write, long total);

    /**
     * Notify  download header info
     */
    public abstract void onHeaders(UrlResponseInfo urlResponseInfo);

    /**
     * Notify  download error
     */
    public abstract void onError(@NonNull IOException error);
}
