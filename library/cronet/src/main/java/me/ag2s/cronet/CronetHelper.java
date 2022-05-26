package me.ag2s.cronet;

import org.chromium.net.CronetEngine;
import org.chromium.net.UploadDataProviders;
import org.chromium.net.UrlRequest;
import org.chromium.net.urlconnection.CronetHttpURLConnection;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

public class CronetHelper {
    public static UrlRequest buildRequest(CronetEngine engine,Request request, UrlRequest.Callback callback) throws IOException {
        String url = request.url().toString();

        Executor executor= Executors.newSingleThreadExecutor();
        UrlRequest.Builder requestBuilder = engine.newUrlRequestBuilder(url, callback, executor);
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
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            requestBuilder.setUploadDataProvider(UploadDataProviders.create(buffer.readByteArray()), executor);
        }

        return requestBuilder.build();
    }

    public static CronetHttpURLConnection openConnection(CronetEngine engine,HttpUrl httpUrl) {
        return new CronetHttpURLConnection(httpUrl.url(), engine);
    }
}
