package me.ag2s.cronet.okhttp;

import android.util.Log;

import androidx.annotation.NonNull;

import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;

import java.io.IOException;
import java.nio.ByteBuffer;

import okhttp3.RequestBody;
import okio.Buffer;

public class RequestBodyUploadProvider extends UploadDataProvider {
    private final RequestBody body;
    private final Buffer buffer;
    private volatile boolean filled=false;



    public RequestBodyUploadProvider(@NonNull RequestBody body) {

        buffer = new Buffer();
        this.body = body;
    }

    private void fillBuffer(){
        try {

            buffer.clear();
            filled=true;
            body.writeTo(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getLength() throws IOException {
        return body.contentLength();
    }

    @Override
    public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) throws IOException {
        if (!filled){
            fillBuffer();
        }



        if (!byteBuffer.hasRemaining()) {
            throw new IllegalStateException("Cronet passed a buffer with no bytes remaining");
        } else {
            int read = 0;
            while (read==0){
                read = buffer.read(byteBuffer);
                Log.e("Cronet","Cronrt write "+read+" to request");
            }
            uploadDataSink.onReadSucceeded(false);
//            for (int bytesRead = 0; bytesRead == 0; bytesRead += read) {
//                read = buffer.read(byteBuffer);
//                Log.e("Cronet","Cronrt write "+read+" to request");
//            }
//            uploadDataSink.onReadSucceeded(false);
        }
    }

    @Override
    public void rewind(@NonNull UploadDataSink uploadDataSink) throws IOException {
        if (body.isOneShot()) {
            uploadDataSink.onRewindError(new IOException("body is oneShot"));
        } else {
            fillBuffer();
            uploadDataSink.onRewindSucceeded();
        }

    }

    @Override
    public void close() throws IOException {
        buffer.close();
        super.close();
    }
}