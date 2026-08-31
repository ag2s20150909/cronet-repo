package me.ag2s.cronet.okhttp;

import static me.ag2s.cronet.okhttp.CronetConstants.BYTE_BUFFER_CAPACITY;
import static me.ag2s.cronet.okhttp.CronetConstants.MAX_ARRAY_SIZE;
import static me.ag2s.cronet.okhttp.CronetConstants.MAX_FOLLOW_COUNT;
import static me.ag2s.cronet.okhttp.CronetConstants.getContentLength;
import static me.ag2s.cronet.okhttp.CronetConstants.getMediaType;
import static me.ag2s.cronet.okhttp.CronetConstants.responseFromResponse;

import androidx.annotation.NonNull;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class AbsMemoryCallback extends UrlRequest.Callback implements AbsCallback,AutoCloseable {
    @NonNull
    protected final Interceptor.Chain mChain;


    private int followCount;
    protected Response.Builder mResponseBuilder;
    private ByteArrayOutputStream mResponseBodyStream;
    private WritableByteChannel mResponseBodyChannel;

    protected AbsMemoryCallback(@NonNull Interceptor.Chain chain) {
        this.mChain=chain;
        mResponseBuilder = new Response.Builder()
                .sentRequestAtMillis(System.currentTimeMillis())
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(0)
                .message("");
    }


    @Override
    public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) throws Exception {
        if (mChain.call().isCanceled()) {
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
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) throws Exception {
        if (mChain.call().isCanceled()) {
            onError(new IOException("Request Canceled"));
            request.cancel();
        }

        mResponseBuilder=responseFromResponse(mResponseBuilder,mChain.call(),info,false);
        if(mChain.request().method().equalsIgnoreCase("HEAD")){
            onSuccess(mResponseBuilder.build());
        }else {
            long bodyLength = getContentLength(info);
            if (bodyLength > MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(
                        "The body is too large and wouldn't fit in a byte array!");
            }
            if (bodyLength<0){
                bodyLength=32;
            }
            mResponseBodyStream = new ByteArrayOutputStream((int) bodyLength);
            mResponseBodyChannel = Channels.newChannel(mResponseBodyStream);
            request.read(ByteBuffer.allocateDirect((int) Math.min(BYTE_BUFFER_CAPACITY,bodyLength)));
        }

    }

    @Override
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) throws Exception {
        byteBuffer.flip();

        try {
            //buffer.write(byteBuffer);
            mResponseBodyChannel.write(byteBuffer);
        } catch (Exception e) {
            //Log.i(TAG, "IOException during ByteBuffer read. Details: ", e);
            throw e;
        }

        byteBuffer.clear();
        request.read(byteBuffer);
    }

    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
        mResponseBuilder.body(ResponseBody.create(mResponseBodyStream.toByteArray(), getMediaType(info)));
        onSuccess(mResponseBuilder.build());
        CronetHelper.closeAll(mResponseBodyStream, mResponseBodyChannel);
    }

    @Override
    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
        onError(new IOException(error));
        CronetHelper.closeAll(mResponseBodyStream, mResponseBodyChannel);
    }

    @Override
    public void close() throws Exception {
        CronetHelper.closeAll(mResponseBodyStream, mResponseBodyChannel);
    }
}
