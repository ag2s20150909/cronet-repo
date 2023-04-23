package me.ag2s.cronet.okhttp;

import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.NonNull;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.Okio;
import okio.Source;
import okio.Timeout;

@SuppressWarnings("unused")
abstract class AbsCronetMemoryCallback extends UrlRequest.Callback implements AutoCloseable {
    private static final String TAG = "Callback";

    private static final int MAX_FOLLOW_COUNT = 20;
    public static final int BYTE_BUFFER_CAPACITY = 32 * 1024;

    private static final Set<String> ENCODINGS_HANDLED_BY_CRONET = Set.of("br", "deflate", "gzip", "x-gzip");
    private static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
    private static final String CONTENT_ENCODING_HEADER_NAME = "Content-Encoding";
    private static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";
    // See ArrayList.MAX_ARRAY_SIZE for reasoning.
    //private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;


    private final Request originalRequest;
    protected final Call mCall;

    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final ArrayBlockingQueue<CallbackResult> callbackResults = new ArrayBlockingQueue<>(2);
    //private final ArrayList<UrlResponseInfo> urlResponseInfoChain= new ArrayList<>();
    private UrlRequest request;


    private int followCount;
    protected Response mResponse;


    //public final Buffer buffer = new Buffer();


    AbsCronetMemoryCallback(@NonNull Request request, @NonNull Call call) {
        originalRequest = request;

        mCall = call;
        mResponse = new Response.Builder()
                .sentRequestAtMillis(System.currentTimeMillis())
                .request(request)
                .protocol(Protocol.HTTP_1_0)
                .code(0)
                .message("")
                .build();


    }

    abstract Response waitForDone(@NonNull UrlRequest urlRequest) throws IOException;

    /**
     * 请求成功后，通知子类结束阻塞，返回response
     *
     * @param response Response
     */
    abstract void onSuccess(@NonNull Response response);

    /**
     * 当发生错误时，通知子类终止阻塞抛出错误
     *
     * @param error IOException
     */
    abstract void onError(@NonNull IOException error);

    /**
     * 从UrlResponseInfo 中获取http协议
     *
     * @param responseInfo responseInfo
     * @return Protocol
     */
    @SuppressWarnings("deprecation")
    private static Protocol protocolFromNegotiatedProtocol(UrlResponseInfo responseInfo) {
        String negotiatedProtocol = responseInfo.getNegotiatedProtocol().toLowerCase();
        //Log.e(TAG, negotiatedProtocol + responseInfo.getUrl());

        if (negotiatedProtocol.contains("h3")) {
            return Protocol.QUIC;
        } else if (negotiatedProtocol.contains("quic")) {
            return Protocol.QUIC;
        } else if (negotiatedProtocol.contains("spdy")) {
            return Protocol.SPDY_3;
        } else if (negotiatedProtocol.contains("h2")) {
            return Protocol.HTTP_2;
        } else if (negotiatedProtocol.contains("1.1")) {
            return Protocol.HTTP_1_1;
        } else {
            return Protocol.HTTP_1_0;
        }
    }

    @NonNull
    private static Headers headersFromResponse(@NonNull UrlResponseInfo responseInfo, boolean keepEncodingAffectedHeaders) {
        List<Map.Entry<String, String>> headers = responseInfo.getAllHeadersAsList();


        Headers.Builder headerBuilder = new Headers.Builder();
        for (Map.Entry<String, String> entry : headers) {

            //Log.e(TAG, entry.getKey() + ":" + entry.getValue());


            try {
                if (!keepEncodingAffectedHeaders && (entry.getKey().equalsIgnoreCase(CONTENT_LENGTH_HEADER_NAME) || entry.getKey().equalsIgnoreCase(CONTENT_ENCODING_HEADER_NAME))) {
                    // Strip all content encoding headers as decoding is done handled by cronet
                    continue;
                }

                headerBuilder.add(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                Log.w(TAG, "Invalid HTTP header/value: " + entry.getKey() + entry.getValue());
                // Ignore that header
            }
        }

        return headerBuilder.build();
    }



    @NonNull
    private static Response responseFromResponse(@NonNull Response response, @NonNull UrlResponseInfo responseInfo, boolean keepEncodingAffectedHeaders) {
        Protocol protocol = protocolFromNegotiatedProtocol(responseInfo);
        Headers headers = headersFromResponse(responseInfo, keepEncodingAffectedHeaders);

        return response.newBuilder()
                .receivedResponseAtMillis(System.currentTimeMillis())
                .protocol(protocol)
                //.request()
                .code(responseInfo.getHttpStatusCode())
                .message(responseInfo.getHttpStatusText())
                .headers(headers)
                .build();
    }

    private static long getContentLength(@NonNull UrlResponseInfo info) {
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

    private static MediaType getMediaType(@NonNull UrlResponseInfo info) {
        List<String> contentLengthHeader = info.getAllHeaders().get(CONTENT_TYPE_HEADER_NAME);
        if (contentLengthHeader == null || contentLengthHeader.size() != 1) {
            return MediaType.parse("text/plain; charset=\"utf-8\"");
        } else {
            return MediaType.parse(contentLengthHeader.get(0));
        }

    }

    private static boolean keepEncodingAffectedHeaders(@NonNull UrlResponseInfo info) {
        List<String> strings = info.getAllHeaders().get(CONTENT_ENCODING_HEADER_NAME);

        if (strings == null) {
            return true;
        }


        if (strings.size() == 0) {
            return true;
        }
        Set<String> types = new HashSet<>();
        for (String s : strings) {
            types.addAll(Arrays.asList(s.split(",")));
        }

        return !ENCODINGS_HANDLED_BY_CRONET.containsAll(types);


    }


    @Override
    public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
        if (mCall.isCanceled()) {
            request.cancel();
            onError(new IOException("Request Canceled"));
            return;
        }
        if (followCount > MAX_FOLLOW_COUNT) {
            request.cancel();
            onError(new IOException("Too many redirect"));
        } else {
            //urlResponseInfoChain.add(info);
            followCount += 1;
            request.followRedirect();
        }


    }

    @Override
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
        this.request = request;
        if (mCall.isCanceled()) {
            onError(new IOException("Request Canceled"));
            request.cancel();
        }
        boolean keepEncodingAffectedHeaders = keepEncodingAffectedHeaders(info);

        mResponse = responseFromResponse(mResponse, info, keepEncodingAffectedHeaders);
        if (originalRequest.method().equalsIgnoreCase("HEAD")) {
            onSuccess(mResponse);
        }

        long contentLength = getContentLength(info);
        if (!keepEncodingAffectedHeaders) {
            contentLength = -1;
        }
        MediaType mediaType = getMediaType(info);


        //Log.e(TAG, "keepEncodingAffectedHeaders:" + keepEncodingAffectedHeaders + " contentLength:" + contentLength + " mediaType:" + mediaType);


        CronetBodySource cronetBodySource = new CronetBodySource();

        ResponseBody responseBody = ResponseBody.create(Okio.buffer(cronetBodySource), mediaType, contentLength);
        mResponse = mResponse.newBuilder().body(responseBody)
                .request(originalRequest.newBuilder().url(info.getUrl()).build())
                .build();
        onSuccess(mResponse);


    }

    @Override
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) throws Exception {
        callbackResults.add(new CallbackResult(CallbackStep.ON_READ_COMPLETED, byteBuffer));
    }

    @Override
    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
        callbackResults.add(new CallbackResult(CallbackStep.ON_FAILED, null, error));
        //onError(e);
        //CronetHelper.closeAll(mResponseBodyStream, mResponseBodyChannel);

    }

    @Override
    public void onCanceled(UrlRequest request, UrlResponseInfo info) {
        callbackResults.add(new CallbackResult(CallbackStep.ON_CANCELED));
        //CronetHelper.closeAll(mResponseBodyStream, mResponseBodyChannel);
    }

    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {

        callbackResults.add(new CallbackResult(CallbackStep.ON_SUCCESS));


    }

    @Override
    public void close() throws Exception {
        //CronetHelper.closeAll(mResponseBodyStream, mResponseBodyChannel);
    }


    class CronetBodySource implements Source {

        private ByteBuffer buffer = ByteBuffer.allocateDirect(32 * 1024);
        private boolean closed = false;
        private final long timeout = timeout().timeoutNanos();

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            if (!finished.get()) {
                if (request != null) {
                    request.cancel();
                }
            }

        }

        @Override
        public long read(@NonNull Buffer sink, long byteCount) throws IOException {
            if (mCall.isCanceled()) {
                if (request != null) {
                    request.cancel();
                }
                throw new IOException("Request Canceled");
            }
            if (closed) {
                throw new IOException("Source Closed");
            }
            if (finished.get()) {
                return -1;
            }
            if (byteCount < buffer.limit()) {
                buffer.limit((int) byteCount);
            }
            if (request != null) {
                request.read(buffer);
            }
            try {
                CallbackResult result = callbackResults.poll(timeout, TimeUnit.NANOSECONDS);
                if (result == null) {
                    if (request != null) {
                        request.cancel();
                    }
                    throw new IOException("Request Timeout");
                }

                switch (result.CallbackStep) {
                    case ON_FAILED: {
                        finished.set(true);
                        buffer = null;
                        throw new IOException(result.exception);
                    }
                    case ON_SUCCESS: {
                        finished.set(true);
                        buffer = null;
                        return -1;
                    }
                    case ON_CANCELED: {
                        buffer = null;
                        throw new IOException("Request Canceled");
                    }
                    case ON_READ_COMPLETED: {
                        if (result.byteBuffer != null) {
                            result.byteBuffer.flip();
                        }
                        long bytesWritten = sink.write(result.byteBuffer);
                        result.byteBuffer.clear();
                        return bytesWritten;
                    }

                }


            } catch (InterruptedException e) {
                if (request != null) {
                    request.read(buffer);
                }
                throw new IOException("Request Timeout", e);
            }

            return 0;
        }

        @NonNull
        @Override
        public Timeout timeout() {
            return mCall.timeout();
        }
    }
}
