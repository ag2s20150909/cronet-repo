package me.ag2s.cronet.okhttp;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.chromium.net.UrlRequest;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Interceptor;
import okhttp3.Response;

@RequiresApi(api = Build.VERSION_CODES.N)
class CronetCallBackNewMemory extends AbsStreamCallback {

    private final CompletableFuture<Response> responseFuture = new CompletableFuture<>();


    CronetCallBackNewMemory(@NonNull Interceptor.Chain chain) {
        super(chain);
    }


    @Override
    public Response waitForDone(@NonNull UrlRequest urlRequest) throws IOException {
        start(urlRequest);
        try {
            if (mChain.call().timeout().timeoutNanos() > 0) {
                return responseFuture.get(mChain.call().timeout().timeoutNanos(), TimeUnit.NANOSECONDS);
            } else {
                return responseFuture.get();
            }

        }
        catch (CancellationException|InterruptedException cancellationException){
            throw new IOException("Request was cancelled", cancellationException.getCause());
        }
        catch (TimeoutException timeoutException){
            throw new IOException("Request timeout", timeoutException.getCause());
        }
        catch (ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 请求成功后，通知子类结束阻塞，返回response
     *
     * @param response Response
     */
    @Override
    public void onSuccess(@NonNull Response response) {
        responseFuture.complete(response);
    }

    /**
     * 当发生错误时，通知子类终止阻塞抛出错误
     *
     * @param error IOException
     */
    @Override
    public void onError(@NonNull IOException error) {
        responseFuture.completeExceptionally(error);
    }


}
