package me.ag2s.cronet.okhttp;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.chromium.net.UrlRequest;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

@RequiresApi(api = Build.VERSION_CODES.N)
class CronetCallBackNew extends AbsCronetCallback {

    private final CompletableFuture<Response> responseFuture = new CompletableFuture<>();


    CronetCallBackNew(Request request, Call call) {
        super(request, call);
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
