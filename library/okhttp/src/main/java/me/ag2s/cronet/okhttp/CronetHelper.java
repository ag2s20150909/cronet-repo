package me.ag2s.cronet.okhttp;

import androidx.annotation.NonNull;

import org.chromium.net.CronetEngine;
import org.chromium.net.UploadDataProvider;
import org.chromium.net.UrlRequest;
import org.chromium.net.apihelpers.UploadDataProviders;
import org.chromium.net.urlconnection.CronetHttpURLConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import me.ag2s.cronet.CronetHolder;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class CronetHelper {
    static final Executor uploadExecutor = Executors.newFixedThreadPool(4);

    @NonNull
    public static UrlRequest buildRequest(@NonNull CronetEngine engine, @NonNull Request request, @NonNull UrlRequest.Callback callback) throws IOException {
        String url = request.url().toString();


        UrlRequest.Builder requestBuilder = engine.newUrlRequestBuilder(url, callback, CronetHolder.getExecutor());
        requestBuilder.setHttpMethod(request.method());
        requestBuilder.allowDirectExecutor();


        Headers headers = request.headers();
        //Log.e("Cronet", headers.toString());
        for (int i = 0; i < headers.size(); i += 1) {
            requestBuilder.addHeader(headers.name(i), headers.value(i));
        }


        RequestBody requestBody = request.body();
        if (requestBody != null) {
            requestBuilder.allowDirectExecutor();

            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                requestBuilder.addHeader("Content-Type", contentType.toString());
            }
            if(requestBody instanceof FileDescriptorRequestBody){
                UploadDataProvider provider=UploadDataProviders.create(((FileDescriptorRequestBody) requestBody).getPfd());
                requestBuilder.setUploadDataProvider(provider,uploadExecutor);
            }else {
                requestBuilder.setUploadDataProvider(new RequestBodyUploadProvider(requestBody), uploadExecutor);
            }

        }

        return requestBuilder.build();
    }

    public static CronetHttpURLConnection openConnection(CronetEngine engine, HttpUrl httpUrl) {
        return new CronetHttpURLConnection(httpUrl.url(), engine);
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
}
