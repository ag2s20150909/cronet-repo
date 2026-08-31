package me.ag2s.cronet.okhttp;

import static me.ag2s.cronet.okhttp.CronetConstants.BYTE_BUFFER_CAPACITY;
import static me.ag2s.cronet.okhttp.CronetConstants.MAX_FOLLOW_COUNT;
import static me.ag2s.cronet.okhttp.CronetConstants.buildPriorResponse;
import static me.ag2s.cronet.okhttp.CronetConstants.getContentLength;
import static me.ag2s.cronet.okhttp.CronetConstants.getMediaType;
import static me.ag2s.cronet.okhttp.CronetConstants.keepEncodingAffectedHeaders;
import static me.ag2s.cronet.okhttp.CronetConstants.responseFromResponse;

import androidx.annotation.NonNull;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.Okio;
import okio.Source;
import okio.Timeout;

abstract class AbsStreamCallback extends UrlRequest.Callback implements AbsCallback {
    private static final String TAG = "Callback";

    @NonNull
    protected final Interceptor.Chain mChain;
    @NonNull
    private final Call mCall;

    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private final AtomicBoolean canceled = new AtomicBoolean(false);

    private final ArrayBlockingQueue<CallbackResult> callbackResults = new ArrayBlockingQueue<>(2);

    private long startTime;
    private UrlRequest request;
    private int followCount;
    protected Response.Builder mResponseBuilder;

    private final long receiveTimeout;

    private final ArrayList<UrlResponseInfo> urlResponseInfoChain = new ArrayList<>();


    public AbsStreamCallback(@NonNull Interceptor.Chain chain) {
        mChain = chain;
        mCall = chain.call();
        this.startTime = System.currentTimeMillis();
        this.receiveTimeout =mChain.connectTimeoutMillis()+mChain.writeTimeoutMillis();
        mResponseBuilder = new Response.Builder()
                .sentRequestAtMillis(System.currentTimeMillis())
                .request(chain.request())
                .protocol(Protocol.HTTP_1_0)
                .code(0)
                .message("");
    }

    public void start(UrlRequest urlRequest) {
        this.request = urlRequest;
        this.startTime = System.currentTimeMillis();
        mResponseBuilder.sentRequestAtMillis(startTime);
        this.request.start();

    }

    @Override
    public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl){
        try {

            if (!connected.get()&&receiveTimeout > 0 && System.currentTimeMillis() - startTime > receiveTimeout) {
                throw new IOException("Request connect timeout");
            }
            connected.set(true);
            this.startTime=System.currentTimeMillis();
            if (mCall.isCanceled()) {
                request.cancel();
                canceled.set(true);
                throw new IOException("Request Canceled");

            }
            if (followCount > MAX_FOLLOW_COUNT) {
                canceled.set(true);
                throw  new IOException("Too many redirect");
            } else {
                urlResponseInfoChain.add(info);
                followCount += 1;
                request.followRedirect();
            }
        } catch (IOException e) {
            request.cancel();
            onError(e);
        }

    }


    @Override
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {

        this.request = request;
        try {
            if (mCall.isCanceled()) {
                throw new IOException("Request Canceled");

            }
            if (!connected.get()&&receiveTimeout > 0 && System.currentTimeMillis() - startTime > receiveTimeout) {
                throw new IOException("Request connect timeout");
            }
            connected.set(true);
            mResponseBuilder = buildPriorResponse(mResponseBuilder, mChain.request(), urlResponseInfoChain, info.getUrlChain());
            boolean keepEncodingAffectedHeaders = keepEncodingAffectedHeaders(info);

            mResponseBuilder = responseFromResponse(mResponseBuilder, mChain.call(), info, keepEncodingAffectedHeaders);
            if (mChain.call().request().method().equalsIgnoreCase("HEAD")) {
                onSuccess(mResponseBuilder.build());
                return;
            }

            long contentLength = getContentLength(info);
            CronetBodySource cronetBodySource = new CronetBodySource(contentLength);
            if (!keepEncodingAffectedHeaders) {
                contentLength = -1;
            }
            MediaType mediaType = getMediaType(info);

            ResponseBody responseBody = ResponseBody.create(Okio.buffer(cronetBodySource), mediaType, contentLength);
            mResponseBuilder.body(responseBody);
            onSuccess(mResponseBuilder.build());
        } catch (IOException e) {
            onError(e);
            canceled.set(true);
            request.cancel();
        }


    }

    @Override
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
        if (mCall.isCanceled()) {
            canceled.set(true);
            request.cancel();
        }


        callbackResults.add(new CallbackResult(CallbackStep.ON_READ_COMPLETED, byteBuffer));
    }

    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
        callbackResults.add(new CallbackResult(CallbackStep.ON_SUCCESS));
    }

    @Override
    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
        onError(error);
        callbackResults.add(new CallbackResult(CallbackStep.ON_FAILED, null, error));
    }


    @Override
    public void onCanceled(UrlRequest request, UrlResponseInfo info) {
        canceled.set(true);
        callbackResults.add(new CallbackResult(CallbackStep.ON_CANCELED));
    }


    class CronetBodySource implements Source {
        private ByteBuffer buffer;

        public CronetBodySource(long bufferSize) {
            if (bufferSize < 0 || bufferSize > BYTE_BUFFER_CAPACITY) {
                bufferSize = BYTE_BUFFER_CAPACITY;
            }
            this.buffer = ByteBuffer.allocateDirect((int) bufferSize);
        }


        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final long timeout = timeout().timeoutNanos();

        @Override
        public void close() {
            if (closed.get()) {
                return;
            }
            closed.set(true);
            if (!finished.get()) {
                if (request != null) {
                    request.cancel();
                }
            }

        }

        @Override
        public long read(@NonNull Buffer sink, long byteCount) throws IOException {
            if (canceled.get()) {
                if (request != null) {
                    request.cancel();
                }
                throw new IOException("Request Canceled");
            }
            if (closed.get()) {
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
            return new Timeout().timeout(mChain.readTimeoutMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
