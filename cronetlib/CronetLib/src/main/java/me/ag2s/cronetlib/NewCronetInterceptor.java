package me.ag2s.cronetlib;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.chromium.net.urlconnection.CronetHttpURLConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.RealResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

public class NewCronetInterceptor implements okhttp3.Interceptor {
    private final @Nullable
    CookieJar mCookieJar;

    public NewCronetInterceptor(@Nullable CookieJar cookieJar) {
        this.mCookieJar = cookieJar;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        //CronetEngine
        if (CronetClient.getInstance().getEngine() == null || !CronetClient.getInstance().useCronet) {
            return chain.proceed(chain.request());
        }
        Request req = chain.request();


        // covert okhttp request to cornet request
        CronetHttpURLConnection connection = CronetClient.getInstance().openConnection(req.url());

        connection.setInstanceFollowRedirects(true);
        //connection.setChunkedStreamingMode(CronetClient.DEFAULT_CHUNKED_LEN);
        //connection.setConnectTimeout(chain.call().timeout().timeoutNanos());
        // add headers
        Set<String> headerlist = req.headers().names();

        for (String headerName : headerlist) {
            connection.addRequestProperty(headerName, req.headers().get(headerName));
        }

        // todo pass cookie
        connection.addRequestProperty("Cookie", getCookieString(mCookieJar, req.url()));
        // method
        connection.setRequestMethod(req.method());

        // add body
        RequestBody requestBody = req.body();
        if (requestBody != null) {
            connection.setRequestProperty("Content-Type", requestBody.contentType() != null ? Objects.requireNonNull(requestBody.contentType()).toString() : "text/plain");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os = new OutputStreamProxy(os, chain.call(), connection);
            BufferedSink sink = Okio.buffer(Okio.sink(os));
            requestBody.writeTo(sink);
            sink.flush();
            os.close();
        }
        Log.e(CronetClient.TAG, connection.toString());


        int statusCode = connection.getResponseCode();

        // handling http redirect
        if (statusCode >= 300 && statusCode <= 310) {
            return chain.proceed(req);
        }
        long contentLength = connection.getContentLength();

        Log.e("DEBUG", contentLength + "AAA");

        Response.Builder respBuilder = new Response.Builder();
        respBuilder
                .request(req)
                .protocol(Protocol.QUIC)
                .code(statusCode)
                .message(connection.getResponseMessage() != null ? connection.getResponseMessage() : "");

        Map<String, List<String>> respHeaders = connection.getHeaderFields();


        for (Map.Entry<String, List<String>> stringListEntry : respHeaders.entrySet()) {
            for (String valueString : stringListEntry.getValue()) {
                if (stringListEntry.getKey() != null) {
                    respBuilder.addHeader(stringListEntry.getKey(), valueString);
                }
            }
        }

        InputStream inputStream = null;
        if (statusCode >= 200 && statusCode <= 399) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        boolean cp = isCompressed(connection);
        Log.e("CronetUrl", "压缩:" + cp);

        inputStream = new InputStreamProxy(inputStream, chain.call(), connection);
        BufferedSource bodySource = Okio.buffer(Okio.source(inputStream));


        List<String> contentTypeList = respHeaders.get("Content-Type");
        String contentTypeString = "";

        if (contentTypeList != null && contentTypeList.size() > 0) {
            contentTypeString = contentTypeList.get(contentTypeList.size() - 1);
        }

//        contentLength=getContentLength(respHeaders);

        Log.e("CronetUrl","压缩:"+contentLength);

        MediaType contentType = MediaType.parse(contentTypeString != null ? contentTypeString : "text/plain; charset=\"utf-8\"");
        if (contentLength > 0&&!isCompressed(connection)) {

            assert contentType != null;
            ResponseBody realResponseBody = new RealResponseBody(contentType.toString(), contentLength, bodySource);
            return respBuilder.body(realResponseBody).build();
        }


        ByteString byteString = bodySource.readByteString();

        //ResponseBody realResponseBody = ResponseBody.create(byteString, contentType);
        ResponseBody realResponseBody = ResponseBody.create(byteString, contentType);

        respBuilder.body(realResponseBody);

        return respBuilder.build();

    }

    public static String getCookieString(CookieJar cookieJar, HttpUrl url) {
        StringBuilder sb = new StringBuilder();
        if (cookieJar != null) {
            List<Cookie> cookies = cookieJar.loadForRequest(url);
            for (Cookie cookie : cookies) {
                sb.append(cookie.name()).append("=").append(cookie.value()).append("; ");
            }
        }
        return sb.toString();
    }


    /**
     * 判断是否被压缩
     * @param connection connection
     * @return true or false
     */
    private static boolean isCompressed(HttpURLConnection connection) {
        String transferEncoding=connection.getHeaderField("Transfer-Encoding");
        if(transferEncoding!=null&&transferEncoding.equalsIgnoreCase("chunked")){
            return true;
        }
        String contentEncoding = connection.getHeaderField("Content-Encoding");
        if(contentEncoding==null){
            return false;
        }
        Log.e("CronetUrl", "压缩:" + contentEncoding);
        return !"identity".equalsIgnoreCase(contentEncoding);
    }


}
