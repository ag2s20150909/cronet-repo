package me.ag2s.cronetlib;

import androidx.annotation.Nullable;

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

    public CronetInterceptor(@Nullable CookieJar cookieJar) {
        this.mCookieJar = cookieJar;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {


        final Request request = chain.request();
        chain.connectTimeoutMillis();


        if ((!CronetClient.getInstance().useCronet) || (!CronetLoader.getInstance().install())) {
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
        if (CronetClient.getInstance().getEngine() != null) {
            Response response = proceedWithCronet(copy, chain.call());
            if (mCookieJar != null) {
                mCookieJar.saveFromResponse(copy.url(), Cookie.parseAll(copy.url(), response.headers()));
            }
            return response;
        } else {
            return chain.proceed(chain.request());
        }
    }

    private Response proceedWithCronet(Request request, Call call) throws IOException {


        CronetCallback callback = new CronetCallback(request, call, CronetClient.getInstance().getOkhttpClient().eventListenerFactory().create(call));
        UrlRequest urlRequest = CronetClient.getInstance().buildRequest(request, callback);
        urlRequest.start();
        return callback.waitForDone(urlRequest);


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
