package me.ag2s.cronet;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.chromium.net.CronetEngine;
import org.chromium.net.UrlRequest;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class CronetInterceptor implements okhttp3.Interceptor {
    private final CookieJar mCookieJar;
    private final CronetEngine engine;

    public CronetInterceptor(@NonNull CronetEngine engine, @Nullable CookieJar cookieJar) {
        this.mCookieJar = cookieJar == null ? CookieJar.NO_COOKIES : cookieJar;
        this.engine = engine;
    }

    public CronetInterceptor(@NonNull CronetEngine engine) {
        this(engine, null);
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {


        final Request request = chain.request();
        chain.connectTimeoutMillis();


        if ((!CronetLoader.getInstance().install())) {
            return chain.proceed(request);
        }
        Request.Builder builder = request.newBuilder();
        builder.removeHeader("Keep-Alive");
        builder.removeHeader("Accept-Encoding");
        if (mCookieJar != null) {
            String cookieString = getCookieString(mCookieJar, request.url());
            if (cookieString.length() > 4) {
                builder.header("Cookie", cookieString);
            }
        }
        final Request copy = builder.build();

        Response response = proceedWithCronet(engine, copy, chain.call());
        if (mCookieJar != null) {
            mCookieJar.saveFromResponse(copy.url(), Cookie.parseAll(copy.url(), response.headers()));
        }
        return response;

    }

    private Response proceedWithCronet(CronetEngine engine, Request request, Call call) throws IOException {
        try {
            AbsCronetCallback callback;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                callback = new CronetCallBackNew(request, call);
            } else {
                callback = new CronetCallBackOld(request, call);
            }
            UrlRequest urlRequest = CronetHelper.buildRequest(engine,request, callback);
            urlRequest.start();
            return callback.waitForDone(urlRequest);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage(), e);
        }


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


}
