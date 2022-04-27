package me.ag2s.cronetlib;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.chromium.net.UrlRequest;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.EventListener;
import okhttp3.Request;
import okhttp3.Response;

@RequiresApi(api = Build.VERSION_CODES.N)
public class CronetNewCallBack extends AbsCronetCallback {

    private final CompletableFuture<Response> responseFuture = new CompletableFuture<Response>();


    CronetNewCallBack(Request request, Call call) {
        super(request, call);
    }

    CronetNewCallBack(Request request, Call call, EventListener eventListener) {
        super(request, call, eventListener);
    }

    CronetNewCallBack(Request request, Call call, @Nullable EventListener eventListener, @Nullable Callback responseCallback) {
        super(request, call, eventListener, responseCallback);
    }

    @Override
    Response waitForDone(@NonNull UrlRequest urlRequest) throws IOException {
        try {
            if (mCall.timeout().timeoutNanos() > 0) {
                return responseFuture.get(mCall.timeout().timeoutNanos(), TimeUnit.NANOSECONDS);
            } else {
                return responseFuture.get();
            }

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
    }

    /**
     * 请求成功后，通知子类结束阻塞，返回response
     *
     * @param response Response
     */
    @Override
    void onSuccess(@NonNull Response response) {
        responseFuture.complete(response);
    }

    /**
     * 当发生错误时，通知子类终止阻塞抛出错误
     *
     * @param error IOException
     */
    @Override
    void onError(@NonNull IOException error) {
        responseFuture.completeExceptionally(error);
    }
}
