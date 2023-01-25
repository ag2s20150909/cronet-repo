package me.ag2s.cronet.okhttp;

import android.util.Log;

import androidx.annotation.NonNull;

import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;

import java.io.IOException;
import java.nio.ByteBuffer;

import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Pipe;

class LargeBodyUploadProvider extends UploadDataProvider {
    private final RequestBody body;
    private volatile boolean filled = false;
    private final Pipe pipe = new Pipe(AbsCronetMemoryCallback.BYTE_BUFFER_CAPACITY);
    private final BufferedSource source=Okio.buffer(pipe.source());


    public LargeBodyUploadProvider(@NonNull RequestBody body) {

        //buffer = new Buffer();
        this.body = body;
    }

    private synchronized void fillBuffer() {
        CronetHelper.uploadExecutor.execute(() -> {

            try {
                filled = true;
                BufferedSink sink = Okio.buffer(pipe.sink());
                //buffer.clear();
                body.writeTo(sink);
                sink.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


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
                read = source.read(byteBuffer);
            }
            uploadDataSink.onReadSucceeded(false);
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
        //.pipe.cancel();
        super.close();
    }
}