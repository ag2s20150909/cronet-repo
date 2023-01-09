package me.ag2s.cronet.okhttp;

import android.util.Log;

import androidx.annotation.NonNull;

import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;

import java.io.IOException;
import java.nio.ByteBuffer;

import okhttp3.RequestBody;
import okio.Buffer;

public class RequestBodyUploadProvider extends UploadDataProvider implements AutoCloseable {
    private final RequestBody body;
    private final Buffer buffer;

    public RequestBodyUploadProvider(@NonNull RequestBody body) {
        buffer = new Buffer();
        this.body = body;
        try {
            body.writeTo(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getLength() throws IOException {
        return body.contentLength();
    }

    @Override
    public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) throws IOException {
        if (!byteBuffer.hasRemaining()) {
            throw new IllegalStateException("Cronet passed a buffer with no bytes remaining");
        } else {
            int read;
            for (int bytesRead = 0; bytesRead == 0; bytesRead += read) {
                read = buffer.read(byteBuffer);
                Log.e("Cronet","Cronrt write "+read+" to request");
            }
            uploadDataSink.onReadSucceeded(false);
        }
    }

    @Override
    public void rewind(@NonNull UploadDataSink uploadDataSink) throws IOException {
        if(body.isOneShot()){
            uploadDataSink.onRewindError(new IOException("body is oneShot"));
        }else {
            buffer.clear();
            body.writeTo(buffer);
            uploadDataSink.onRewindSucceeded();
        }

    }

    @Override
    public void close() throws IOException {
        if (buffer != null) {
            buffer.close();
        }
        super.close();
    }
}