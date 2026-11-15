package me.ag2s.cronet.okhttp;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.system.Os;

import androidx.annotation.NonNull;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class CronetParcelFileDescriptorCallback extends UrlRequest.Callback implements DownLoadInterface {
    private static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
    private static final int BYTE_BUFFER_CAPACITY = 64 * 1024;
    @NotNull
    private final ParcelFileDescriptor pfd;
    private long length = 0;
    private long total = -1;

    /**
     * 警告：在写入完成之前不要关闭ParcelFileDescriptor
     * Warning: Do not close ParcelFileDescriptor until writing is complete
     *
     * @param pfd
     */
    public CronetParcelFileDescriptorCallback(@NonNull ParcelFileDescriptor pfd) {
        this.pfd = pfd;
    }

    public CronetParcelFileDescriptorCallback(@NonNull ContentResolver contentResolver, @NonNull Uri uri) throws FileNotFoundException {
        this.pfd = contentResolver.openFileDescriptor(uri, "w");
    }

    public CronetParcelFileDescriptorCallback(@NonNull File file) throws FileNotFoundException {
        this.pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_WRITE_ONLY);
    }

    public CronetParcelFileDescriptorCallback(@NonNull FileDescriptor fd) throws IOException {
        this.pfd = ParcelFileDescriptor.dup(fd);
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

    @Override
    public void onRedirectReceived(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, String s) throws Exception {
        urlRequest.followRedirect();
    }

    @Override
    public void onResponseStarted(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo) throws Exception {
        onHeaders(urlResponseInfo);
        total = getBodyLength(urlResponseInfo);
        if (checkPfd()) {
            onError(new IOException("pfd closed"));
            urlRequest.cancel();
        } else if (total > 0) {
            //Extend file to target size
            Os.ftruncate(pfd.getFileDescriptor(), total);
        } else {
            //empty old File
            Os.ftruncate(pfd.getFileDescriptor(), 0);
        }
        urlRequest.read(ByteBuffer.allocateDirect(BYTE_BUFFER_CAPACITY));
    }

    @Override
    public void onReadCompleted(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, ByteBuffer byteBuffer) throws Exception {
        byteBuffer.flip();
        if (checkPfd()) {
            onError(new IOException("pfd closed"));
            urlRequest.cancel();
        } else {
            try {
                length += Os.write(pfd.getFileDescriptor(), byteBuffer);
                onProgress(length, total);

            } catch (IOException e) {
                e.printStackTrace();
                onError(e);
            }

        }

        byteBuffer.clear();
        urlRequest.read(byteBuffer);


    }

    @Override
    public void onSucceeded(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo) {
        onSuccess();
        closePfd();
    }

    @Override
    public void onFailed(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, CronetException e) {
        onError(e);
        closePfd();

    }

    @Override
    public void onCanceled(UrlRequest request, UrlResponseInfo info) {
        super.onCanceled(request, info);
        closePfd();
    }

    private boolean checkPfd() {
        return !pfd.getFileDescriptor().valid();

    }


    private void closePfd() {
        try {
            pfd.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
