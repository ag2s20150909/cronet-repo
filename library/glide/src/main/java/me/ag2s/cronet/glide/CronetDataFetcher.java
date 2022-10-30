package me.ag2s.cronet.glide;

import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;
import org.chromium.net.impl.ImplVersion;

import java.nio.ByteBuffer;
import java.util.Map;

import me.ag2s.cronet.CronetHolder;
import me.ag2s.cronet.DirectExecutor;


public class CronetDataFetcher<T> extends UrlRequest.Callback implements DataFetcher<T>, AutoCloseable {


    private static final int BYTE_BUFFER_CAPACITY = 32 * 1024;
    private final GlideUrl url;
    private final UrlRequest.Builder builder;
    private final ByteBufferParser<T> parser;
    private UrlRequest urlRequest;
    private DataCallback<? super T> dataCallback;
    private BufferQueue.Builder bufferQueue;

    public CronetDataFetcher(ByteBufferParser<T> parser, GlideUrl url) {
        this.url = url;
        this.parser = parser;
        builder = CronetHolder.getEngine().newUrlRequestBuilder(url.toStringUrl(), this, DirectExecutor.INSTANCE);


    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull final DataCallback<? super T> dataCallback) {
        this.dataCallback = dataCallback;
        builder.setPriority(CronetLibraryGlideModule.GLIDE_TO_CHROMIUM_PRIORITY.get(priority));
        builder.allowDirectExecutor();

        builder.addHeader("Cronet", ImplVersion.getCronetVersion());
        if (url != null) {
            for (Map.Entry<String, String> headerEntry : url.getHeaders().entrySet()) {
                String key = headerEntry.getKey();
                builder.addHeader(key, headerEntry.getValue());
            }
        }
        urlRequest = builder.build();
        urlRequest.start();
    }

    @Override
    public void cleanup() {
        //bytesReceived.reset();
    }

    @Override
    public void cancel() {
        if (urlRequest != null) {
            urlRequest.cancel();
        }
    }


    @NonNull
    @Override
    public Class<T> getDataClass() {
        return parser.getDataClass();
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
        String negotiatedProtocol = info.getNegotiatedProtocol().toLowerCase();
        Log.e("Cronet", negotiatedProtocol + info.getUrl());

        bufferQueue = BufferQueue.builder();
        request.read(bufferQueue.getFirstBuffer(info));


    }

    @Override
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) throws Exception {
        request.read(bufferQueue.getNextBuffer(byteBuffer));

    }


    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
        dataCallback.onDataReady(parser.parse(bufferQueue.build().coalesceToBuffer()));
    }

    @Override
    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
        dataCallback.onLoadFailed(error);
    }


    @Override
    public void close() throws Exception {

    }

}
