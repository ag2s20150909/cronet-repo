package me.ag2s.cronetlib;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import okhttp3.Call;
import okhttp3.CipherSuite;
import okhttp3.Handshake;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.TlsVersion;
import okio.Buffer;
import okio.ByteString;

@RequiresApi(api = Build.VERSION_CODES.N)
public class NewCallBack extends UrlRequest.Callback {


    private static final int MAX_FOLLOW_COUNT = 20;
    //Request request;
    Call mCall;
    @Nullable
    private IOException mException;
    private int followCount;
    Response mResponse;


    public CompletableFuture<Response> responseFuture = new CompletableFuture<>();
    Buffer buffer = new Buffer();
    private final Request originalRequest;

    public NewCallBack(Request request, Call call) {
        this.mCall = call;
        this.originalRequest = request;
        Handshake handshake = Handshake.get(TlsVersion.TLS_1_3, CipherSuite.TLS_AES_128_CCM_SHA256, new ArrayList<>(), new ArrayList<>());
        mResponse = new Response.Builder()
                .sentRequestAtMillis(System.currentTimeMillis())
                .request(request)
                .handshake(handshake)
                .protocol(Protocol.HTTP_1_0)
                .code(0)
                .message("")
                .build();
    }


    @Override
    public void onRedirectReceived(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, String newLocationUrl) throws Exception {
        if (mCall.isCanceled()) {
            urlRequest.cancel();
            mException = new IOException("Request Canceled");
            responseFuture.completeExceptionally(mException);
        }

        if (followCount > MAX_FOLLOW_COUNT) {
            urlRequest.cancel();
            mException = new IOException("Too many redirect");
            responseFuture.completeExceptionally(mException);
        }
        followCount += 1;
        OkHttpClient client = CronetClient.getInstance().getOkhttpClient();
        if (originalRequest.url().isHttps() && newLocationUrl.startsWith("http://") && client.followSslRedirects()) {
            urlRequest.followRedirect();
        } else if (!originalRequest.url().isHttps() && newLocationUrl.startsWith("https://") && client.followSslRedirects()) {
            urlRequest.followRedirect();
        } else if (client.followRedirects()) {
            urlRequest.followRedirect();
        } else {
            urlRequest.cancel();
            mException = new IOException("Too many redirect");
            responseFuture.completeExceptionally(mException);
        }
    }

    @Override
    public void onResponseStarted(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo) throws Exception {
        urlRequest.read(ByteBuffer.allocateDirect(64 * 1024));
    }

    @Override
    public void onReadCompleted(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, ByteBuffer byteBuffer) throws Exception {

        if (mCall.isCanceled()) {
            urlRequest.cancel();
            mException = new IOException("Request Canceled");
            responseFuture.completeExceptionally(mException);
        }
        byteBuffer.flip();

        buffer.write(byteBuffer);

        byteBuffer.clear();
        urlRequest.read(byteBuffer);
    }


    @Override
    public void onSucceeded(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo) {
        String contentTypeString = mResponse.header("content-type");
        MediaType contentType = MediaType.parse(contentTypeString != null ? contentTypeString : "text/plain; charset=\"utf-8\"");
        ByteString bytes = buffer.readByteString();
        mResponse = mResponse.newBuilder().body(ResponseBody.create(contentType, bytes)).request(originalRequest.newBuilder().url(urlResponseInfo.getUrl()).build()).build();

        //mResponseConditon.open();

//        if (//eventListener != null) {
//            //eventListener.callEnd(mCall);
//        }
//
//        if (responseCallback != null) {
//            try {
//                responseCallback.onResponse(mCall, mResponse);
//            } catch (IOException e) {
//                // Pass?
//            }
//        }
        mResponse = responseFromResponse(mResponse, urlResponseInfo);
        responseFuture.complete(mResponse);
    }

    @Override
    public void onFailed(UrlRequest urlRequest, UrlResponseInfo urlResponseInfo, CronetException e) {
        responseFuture.completeExceptionally(e);
    }

    /**
     * 从UrlResponseInfo 中获取http协议
     *
     * @param responseInfo responseInfo
     * @return Protocol
     */
    private static Protocol protocolFromNegotiatedProtocol(UrlResponseInfo responseInfo) {
        String negotiatedProtocol = responseInfo.getNegotiatedProtocol().toLowerCase();

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
}
