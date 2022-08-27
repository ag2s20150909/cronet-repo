package me.ag2s.cronet.okhttp;

import android.util.Log;

import androidx.annotation.NonNull;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
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
import okio.Buffer;
import okio.ByteString;

@SuppressWarnings("unused")
abstract class AbsCronetCallback extends UrlRequest.Callback {
    private static final String TAG = "Callback";

    private static final int MAX_FOLLOW_COUNT = 20;

    private final Request originalRequest;
    protected final Call mCall;

    private int followCount;
    protected Response mResponse;



    public final Buffer buffer = new Buffer();



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
        Log.e(TAG, negotiatedProtocol);

        if (negotiatedProtocol.contains("h3")) {
            return Protocol.HTTP_3;
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

    @NonNull
    private Response responseFromResponse(@NonNull Response response, UrlResponseInfo responseInfo) {
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
        }else {
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


        request.read(ByteBuffer.allocateDirect(32 * 1024));
        //request.read(ByteBuffer.allocateDirect(64 * 1024));
    }

    @Override
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) throws Exception {
        byteBuffer.flip();

        try {
            buffer.write(byteBuffer);
            //mReceiveChannel.write(byteBuffer);
        } catch (Exception e) {
            Log.i(TAG, "IOException during ByteBuffer read. Details: ", e);
            throw e;
        }

        byteBuffer.clear();
        request.read(byteBuffer);
    }

    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {


        String contentTypeString = mResponse.header("content-type");
        MediaType contentType = MediaType.parse(contentTypeString != null ? contentTypeString : "text/plain; charset=\"utf-8\"");
        ByteString bytes = buffer.readByteString();
        mResponse = mResponse.newBuilder().body(ResponseBody.create(bytes, contentType)).request(originalRequest.newBuilder().url(info.getUrl()).build()).build();

        //mResponseConditon.open();
        onSuccess(mResponse);


    }

    @Override
    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
        IOException e = new IOException(Objects.requireNonNull(error.getMessage()).substring(31), error);
        //mException = e;
        //mResponseConditon.open();
        onError(e);

    }

    @Override
    public void onCanceled(UrlRequest request, UrlResponseInfo info) {
        onError(new IOException("CronetClient Request Canceled"));
    }
}
