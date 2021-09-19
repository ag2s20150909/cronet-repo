package me.ag2s.cronetlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetProvider;
import org.chromium.net.ExperimentalCronetEngine;
import org.chromium.net.ICronetEngineBuilder;
import org.chromium.net.MyCronetEngine;
import org.chromium.net.UploadDataProviders;
import org.chromium.net.UrlRequest;
import org.chromium.net.impl.JavaCronetProvider;
import org.chromium.net.impl.NativeCronetProvider;
import org.chromium.net.urlconnection.CronetHttpURLConnection;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;


public class CronetClient {


    public static final String TAG = "CronetClient";
    /**
     * 默认缓冲区大小
     */
    public static final int DEFAULT_CHUNKED_LEN = 4096;
    /**
     * 是否使用Cronet
     */
    public boolean useCronet = true;
    @SuppressLint("StaticFieldLeak")
    private static volatile CronetClient helper = null;
    public static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private OkHttpClient okHttpClient;
    @Nullable
    private CookieJar cookieJar;

    @SuppressWarnings("unused")
    public void setOkHttpClient(@NonNull OkHttpClient okHttpClient) {
        this.cookieJar = okHttpClient.cookieJar();
        this.okHttpClient = okHttpClient.newBuilder().addInterceptor(new CronetInterceptor(cookieJar)).build();
    }

    @SuppressWarnings("unused")
    public void setCookiejar(@Nullable CookieJar cookieJar) {
        this.cookieJar = cookieJar;
    }

    public OkHttpClient getOkhttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder().addInterceptor(new CronetInterceptor(cookieJar)).build();
        }
        return okHttpClient;
    }


    public void setUseCronet(boolean useCronet) {
        this.useCronet = useCronet;
    }

    public static CronetClient getInstance() {

        if (helper == null) {
            synchronized (CronetClient.class) {
                if (helper == null) {
                    helper = new CronetClient();
                }
            }
        }
        return helper;
    }

    private CronetClient() {
        engine = getEngine();
    }

    private ExperimentalCronetEngine engine;

    public synchronized ExperimentalCronetEngine getEngine() {
        if (engine == null) {
            Context mContext = CronetContentProvider.getCtx();


            ExperimentalCronetEngine.Builder builder = new MyCronetEngine.Builder(mContext)
//                    .setStoragePath(mContext.getExternalCacheDir().getAbsolutePath())
//                    .enableHttpCache(HTTP_CACHE_DISK, 1024 * 1024 * 500)
                    .enableQuic(true)
                    .enablePublicKeyPinningBypassForLocalTrustAnchors(true)
                    .enableNetworkQualityEstimator(true)
                    .addQuicHint("http3.is", 443, 443)
                    .enableHttp2(true)
                    .enableSdch(true)
                    .setExperimentalOptions("")

                    .setThreadPriority(0);

            Log.e("DDD", "" + CronetLoader.getInstance().install());
            if (CronetLoader.getInstance().install() && CronetLoader.getInstance().need()) {
                //builder.setLibraryLoader(CronetLoader.getInstance());
            }
            builder.enableBrotli(true);
            engine = builder.build();

            //URL.setURLStreamHandlerFactory(engine.createURLStreamHandlerFactory());
        }
        return engine;
    }



    public UrlRequest buildRequest(Request request, UrlRequest.Callback callback) throws IOException {
        String url = request.url().toString();

        UrlRequest.Builder requestBuilder = getEngine().newUrlRequestBuilder(url, callback, executor);
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

    public CronetHttpURLConnection openConnection(HttpUrl httpUrl) {
        return new CronetHttpURLConnection(httpUrl.url(), getEngine());
    }


    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();

        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        }
    }


}
