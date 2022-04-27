package me.ag2s.cronet.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

public class CronetDataFetcher<T> extends UrlRequest.Callback implements DataFetcher<ByteBuffer>{
    private static final int BYTE_BUFFER_CAPACITY_BYTES = 64 * 1024;

    private final ByteArrayOutputStream bytesReceived = new ByteArrayOutputStream();
    private final WritableByteChannel receiveChannel = Channels.newChannel(bytesReceived);



    private DataCallback<? super ByteBuffer> dataCallback;
    private UrlRequest urlRequest;
    private final UrlRequest.Builder builder;
    T url;

    public CronetDataFetcher(CronetEngine engine,T url) {
        this.url=url;
        if (url instanceof GlideUrl){
            GlideUrl s=(GlideUrl) url;
            builder= engine.newUrlRequestBuilder(s.toStringUrl(), this, CronetLibraryGlideModule.executor);
        }else {
            builder= engine.newUrlRequestBuilder((String)url,this, CronetLibraryGlideModule.executor);
        }

    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super ByteBuffer> dataCallback) {
        this.dataCallback = dataCallback;
        builder.setPriority(CronetLibraryGlideModule.GLIDE_TO_CHROMIUM_PRIORITY.get(priority));
        builder.allowDirectExecutor();
        urlRequest=builder.build();
        if (url instanceof GlideUrl){
            GlideUrl t=(GlideUrl) url;
            for (Map.Entry<String, String> headerEntry : t.getHeaders().entrySet()) {
                String key = headerEntry.getKey();
                builder.addHeader(key, headerEntry.getValue());
            }
        }

        urlRequest.start();
    }

    @Override
    public void cleanup() {
        bytesReceived.reset();
    }

    @Override
    public void cancel() {
        if (urlRequest != null) {
            urlRequest.cancel();
        }
    }


    @NonNull
    @Override
    public Class<ByteBuffer> getDataClass() {
        return ByteBuffer.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }


    @Override
    public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) throws Exception {
        request.followRedirect();
    }


    @Override
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) throws Exception {
        request.read(ByteBuffer.allocateDirect(BYTE_BUFFER_CAPACITY_BYTES));
    }

    @Override
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) throws Exception {
        byteBuffer.flip();

        try {
            receiveChannel.write(byteBuffer);
        } catch (IOException e) {
            android.util.Log.i("CronetDataFetcher", "IOException during ByteBuffer read. Details: ", e);
        }
        // Reset the buffer to prepare it for the next read
        byteBuffer.clear();

        // Continue reading the request
        request.read(byteBuffer);
    }


    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytesReceived.toByteArray());
        dataCallback.onDataReady(byteBuffer);
    }

    @Override
    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
        dataCallback.onLoadFailed(error);
    }
}
