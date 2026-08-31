package me.ag2s.cronet.okhttp;

import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;

import java.io.IOException;
import java.nio.ByteBuffer;

import okhttp3.RequestBody;
import okio.Buffer;

class BodyUploadProvider extends UploadDataProvider {
    private final RequestBody body;
    private final Buffer buffer;
    private volatile boolean filled = false;

    public BodyUploadProvider(RequestBody body) {
        this.body = body;
        this.buffer=new Buffer();
    }

    private synchronized void fillBuffer(){
        try {
            buffer.clear();
            filled=true;
            body.writeTo(buffer);
            buffer.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public long getLength() throws IOException {
        return body.contentLength();
    }

    @Override
    public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) throws IOException {
        if (!filled) {
            fillBuffer();
        }

        if (!byteBuffer.hasRemaining()) {
            throw new IllegalStateException("Cronet passed a buffer with no bytes remaining");
        } else {
            int read = 0;
            while (read <= 0) {
                read = buffer.read(byteBuffer);
            }
            uploadDataSink.onReadSucceeded(false);
        }
    }

    @Override
    public void rewind(UploadDataSink uploadDataSink) throws IOException {
        if (body.isOneShot()) {
            uploadDataSink.onRewindError(new IOException("body is oneShot"));
        } else {
            fillBuffer();
            uploadDataSink.onRewindSucceeded();
        }
    }
}
