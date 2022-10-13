package me.ag2s.cronet.okhttp;

import android.util.Log;

import androidx.annotation.NonNull;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@SuppressWarnings("unused")
abstract class AbsCronetCallback extends UrlRequest.Callback implements AutoCloseable {
    private static final String TAG = "Callback";

    private static final int MAX_FOLLOW_COUNT = 20;
    private static final int BYTE_BUFFER_CAPACITY = 32 * 1024;

    private static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
    // See ArrayList.MAX_ARRAY_SIZE for reasoning.
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private ByteArrayOutputStream mResponseBodyStream;
    private WritableByteChannel mResponseBodyChannel;

    private final Request originalRequest;
    protected final Call mCall;

    private int followCount;
    protected Response mResponse;


    //public final Buffer buffer = new Buffer();


    AbsCronetCallback(Request request, Call call) {
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
        Log.e(TAG, negotiatedProtocol + responseInfo.getUrl());

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
    private Headers headersFromResponse(@NonNull UrlResponseInfo responseInfo) {
        List<Map.Entry<String, String>> headers = responseInfo.getAllHeadersAsList();


        Headers.Builder headerBuilder = new Headers.Builder();
        for (Map.Entry<String, String> entry : headers) {
            try {
                if (entry.getKey().equalsIgnoreCase("content-encoding")) {
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

    @NonNull
    private Response responseFromResponse(@NonNull Response response, @NonNull UrlResponseInfo responseInfo) {
        Protocol protocol = protocolFromNegotiatedProtocol(responseInfo);
        Headers headers = headersFromResponse(responseInfo);

        return response.newBuilder()
                .receivedResponseAtMillis(System.currentTimeMillis())
                .protocol(protocol)
                .request(mCall.request())
                .code(responseInfo.getHttpStatusCode())
                .message(responseInfo.getHttpStatusText())
                .headers(headers)
                .build();
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
        } else {
            followCount += 1;
            request.followRedirect();
        }


    }

    @Override
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
        mResponse = responseFromResponse(mResponse, info);
        if (mCall.isCanceled()) {
            onError(new IOException("Request Canceled"));
            request.cancel();
        }
        long bodyLength = getBodyLength(info);
        if (bodyLength > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(
                    "The body is too large and wouldn't fit in a byte array!");
        }
        mResponseBodyStream = new ByteArrayOutputStream((int) bodyLength);
        mResponseBodyChannel = Channels.newChannel(mResponseBodyStream);
        request.read(ByteBuffer.allocateDirect(BYTE_BUFFER_CAPACITY));

    }

    @Override
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) throws Exception {
        byteBuffer.flip();

        try {
            //buffer.write(byteBuffer);
            mResponseBodyChannel.write(byteBuffer);
        } catch (Exception e) {
            Log.i(TAG, "IOException during ByteBuffer read. Details: ", e);
            throw e;
        }

        byteBuffer.clear();
        request.read(byteBuffer);
    }

    @Override
    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
        IOException e = new IOException(Objects.requireNonNull(error.getMessage()).substring(31), error);
        onError(e);

    }

    @Override
    public void onCanceled(UrlRequest request, UrlResponseInfo info) {
        onError(new IOException("CronetClient Request Canceled"));
    }

    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {


        String contentTypeString = mResponse.header("content-type");
        MediaType contentType = MediaType.parse(contentTypeString != null ? contentTypeString : "text/plain; charset=\"utf-8\"");
        //yteString bytes = buffer.readByteString();
        mResponse = mResponse.newBuilder().body(ResponseBody.create(mResponseBodyStream.toByteArray(), contentType))
                .request(originalRequest.newBuilder()
                        .url(info.getUrl()).build())
                .build();
        onSuccess(mResponse);


    }

    @Override
    public void close() {
        //buffer.close();

    }
}
