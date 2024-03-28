package me.ag2s.cronet.okhttp;

import androidx.annotation.NonNull;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class CronetNoOpCallBack extends UrlRequest.Callback implements DownLoadInterface{
    private static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
    private static final int BYTE_BUFFER_CAPACITY = 64 * 1024;
    private long length = 0;
    private long total = -1;

    public CronetNoOpCallBack(){

    }
    /**
     * Returns the numerical value of the Content-Header length, or -1 if not set or invalid.
     */
    private static long getBodyLength(@NonNull UrlResponseInfo info) {
        List<String> contentLengthHeader = info.getAllHeaders().get(CONTENT_LENGTH_HEADER_NAME);
        if (contentLengthHeader == null || contentLengthHeader.size() != 1) {
            return -1;
        }
        try {
            return Long.parseLong(contentLengthHeader.get(0));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    @Override
    public void onRedirectReceived(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, String s) throws Exception {
        urlRequest.followRedirect();
    }

    @Override
    public void onResponseStarted(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo) throws Exception {
        onHeaders(urlResponseInfo);
        total = getBodyLength(urlResponseInfo);
        urlRequest.read(ByteBuffer.allocateDirect(BYTE_BUFFER_CAPACITY));
    }

    @Override
    public void onReadCompleted(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, ByteBuffer byteBuffer) throws Exception {
        byteBuffer.flip();
        length+=byteBuffer.limit();
        onProgress(length, total);
        byteBuffer.clear();
        urlRequest.read(byteBuffer);
    }

    @Override
    public void onSucceeded(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo) {
        onSuccess();
    }

    @Override
    public void onFailed(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, CronetException e) {
        onError(e);
    }


    /**
     * Notify  download completed and success
     */
    @Override
    public abstract void onSuccess() ;

    /**
     * Notify  download Progress
     *
     * @param write bytes written to file
     * @param total Total bytes the file download,if server headers don't have Content-Header ,it will be -1.
     */
    @Override
    public abstract void onProgress(long write, long total);

    /**
     * Notify  download header info
     *
     * @param urlResponseInfo
     */
    @Override
    public abstract void onHeaders(UrlResponseInfo urlResponseInfo) ;

    /**
     * Notify  download error
     *
     * @param error
     */
    @Override
    public abstract void onError(@NonNull IOException error) ;
}
