package me.ag2s.cronet.okhttp;

import androidx.annotation.NonNull;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;

public abstract class CronetFileCallBack extends UrlRequest.Callback {
    private static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
    private static final int BYTE_BUFFER_CAPACITY = 64 * 1024;
    private final OutputStream outputStream;
    private final WritableByteChannel mResponseBodyChannel;
    private long length = 0;
    private long total = -1;

    public CronetFileCallBack(@NonNull OutputStream outputStream) {
        this.outputStream = outputStream;
        this.mResponseBodyChannel = Channels.newChannel(this.outputStream);
    }

    public CronetFileCallBack(@NonNull File file) throws FileNotFoundException {
        this.outputStream = new FileOutputStream(file);
        this.mResponseBodyChannel = Channels.newChannel(this.outputStream);
    }

    public CronetFileCallBack(@NonNull String path) throws FileNotFoundException {
        this.outputStream = new FileOutputStream(path);
        this.mResponseBodyChannel = Channels.newChannel(this.outputStream);
    }

    public CronetFileCallBack(@NonNull FileDescriptor fd) throws FileNotFoundException {
        this.outputStream = new FileOutputStream(fd);
        this.mResponseBodyChannel = Channels.newChannel(this.outputStream);
    }

    /**
     * Returns the numerical value of the Content-Header length, or 32 if not set or invalid.
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

    public abstract void onSuccess();

    public abstract void onProgress(long write, long total);

    public abstract void onHeaders(UrlResponseInfo urlResponseInfo);

    public abstract void onError(@NonNull IOException error);

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
        try {
            length += mResponseBodyChannel.write(byteBuffer);
            onProgress(length, total);

        } catch (IOException e) {
            e.printStackTrace();
            onError(e);
        }

        byteBuffer.clear();
        urlRequest.read(byteBuffer);


    }

    @Override
    public void onSucceeded(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo) {
        onSuccess();
        CronetHelper.closeAll(outputStream, mResponseBodyChannel);
    }

    @Override
    public void onFailed(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, CronetException e) {
        onError(e);
        CronetHelper.closeAll(outputStream, mResponseBodyChannel);

    }

    @Override
    public void onCanceled(UrlRequest request, UrlResponseInfo info) {
        super.onCanceled(request, info);
        CronetHelper.closeAll(outputStream, mResponseBodyChannel);
    }

}
