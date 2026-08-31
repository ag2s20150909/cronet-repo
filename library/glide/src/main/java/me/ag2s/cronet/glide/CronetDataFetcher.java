package me.ag2s.cronet.glide;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.util.Synthetic;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map;

import me.ag2s.cronet.CronetHolder;


public class CronetDataFetcher<T> extends UrlRequest.Callback implements DataFetcher<T> {



    private static final int BYTE_BUFFER_CAPACITY = 32 * 1024;
    @NonNull
    private static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
    // See ArrayList.MAX_ARRAY_SIZE for reasoning.
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    @NonNull
    private final RequestFactory requestFactory;
    @NonNull
    private final ByteBufferParser<T> parser;


    @NonNull
    private final GlideUrl url;
    @Nullable
    private ByteArrayOutputStream mResponseBodyStream;
    @Nullable
    private WritableByteChannel mResponseBodyChannel;
    @Nullable
    private UrlRequest urlRequest;
    @Nullable
    private DataCallback<? super T> dataCallback;

    public CronetDataFetcher(@NonNull ByteBufferParser<T> parser, @NonNull GlideUrl url) {
        this.url = url;
        this.parser = parser;
        this.requestFactory = new DefaultRequestFactory();


    }

    /**
     * Returns the numerical value of the Content-Header length, or 32 if not set or invalid.
     */
    private static long getBodyLength(@NonNull UrlResponseInfo info) {
        List<String> contentLengthHeader = info.getAllHeaders().get(CONTENT_LENGTH_HEADER_NAME);
        if (contentLengthHeader == null || contentLengthHeader.size() != 1) {
            return 32;
        }
        try {
            return Long.parseLong(contentLengthHeader.get(0));
        } catch (NumberFormatException e) {
            return 32;
        }
    }

    static void closeAll(OutputStream out, WritableByteChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out != null) {
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull final DataCallback<? super T> dataCallback) {
        this.dataCallback = dataCallback;
        urlRequest = requestFactory.createRequest(url, priority, this);
        urlRequest.start();
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
    public void cleanup() {
        closeAll(mResponseBodyStream, mResponseBodyChannel);

        //bytesReceived.reset();
    }

    @Override
    public void cancel() {
        if (urlRequest != null) {
            urlRequest.cancel();
            closeAll(mResponseBodyStream, mResponseBodyChannel);
        }
    }

    @Override
    public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
        request.followRedirect();
    }

    @Override
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
        String negotiatedProtocol = info.getNegotiatedProtocol().toLowerCase();
        Log.i("Cronet", String.format("%s %d %s %s", negotiatedProtocol, info.getHttpStatusCode(), info.getHttpStatusText(), info.getUrl()));
        if (info.getHttpStatusCode() < 200 || info.getHttpStatusCode() > 299) {
            if (dataCallback != null) {
                dataCallback.onLoadFailed(new IOException("http request faired"));
            }
            if (urlRequest != null) {
                urlRequest.cancel();
            }
            closeAll(mResponseBodyStream, mResponseBodyChannel);
        } else {
            long bodyLength = getBodyLength(info);
            if (bodyLength > MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(
                        "The body is too large and wouldn't fit in a byte array!");
            } else {
                mResponseBodyStream = new ByteArrayOutputStream((int) bodyLength);
                mResponseBodyChannel = Channels.newChannel(mResponseBodyStream);
                request.read(ByteBuffer.allocateDirect(BYTE_BUFFER_CAPACITY));
            }

        }


    }

    @Override
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
        byteBuffer.flip();

        try {
            if (mResponseBodyChannel != null) {
                mResponseBodyChannel.write(byteBuffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        byteBuffer.clear();
        request.read(byteBuffer);


    }

    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
        try {
            if (dataCallback != null && mResponseBodyStream != null) {
                dataCallback.onDataReady(parser.parse(ByteBuffer.wrap(mResponseBodyStream.toByteArray())));
            }
        } catch (Exception e) {
            e.printStackTrace();
            dataCallback.onLoadFailed(e);
        }
        closeAll(mResponseBodyStream, mResponseBodyChannel);

    }

    @Override
    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
        if (dataCallback != null) {
            dataCallback.onLoadFailed(error);
        }
        closeAll(mResponseBodyStream, mResponseBodyChannel);
    }


    interface RequestFactory {
        UrlRequest createRequest(@NonNull GlideUrl url, @NonNull Priority priority, @NonNull UrlRequest.Callback cb);
    }

    private static final class DefaultRequestFactory implements RequestFactory {
        @Synthetic
        DefaultRequestFactory() {
        }

        @Override
        public UrlRequest createRequest(@NonNull GlideUrl url, @NonNull Priority priority, @NonNull UrlRequest.Callback cb) {
            UrlRequest.Builder builder = CronetHolder.getEngine().newUrlRequestBuilder(url.toStringUrl(), cb, CronetLibraryGlideModule.glideThreadPool);
            builder.setPriority(CronetLibraryGlideModule.GLIDE_TO_CHROMIUM_PRIORITY.get(priority));
            builder.allowDirectExecutor();
            for (Map.Entry<String, String> headerEntry : url.getHeaders().entrySet()) {
                String key = headerEntry.getKey();
                if ("Accept-Encoding".equalsIgnoreCase(key)) {
                    continue;
                }
                builder.addHeader(key, headerEntry.getValue());
            }
            return builder.build();
        }
    }

}
