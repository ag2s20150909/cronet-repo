package me.ag2s.cronet.okhttp;

import android.os.ConditionVariable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.chromium.net.UrlRequest;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class CronetCallBackOldMemory extends AbsStreamCallback {
    private final ConditionVariable mResponseCondition = new ConditionVariable();
    @Nullable
    private IOException mException;

    CronetCallBackOldMemory(@NonNull Interceptor.Chain chain) {
        super(chain);
    }

    @Override
    public Response waitForDone(@NonNull UrlRequest urlRequest) throws IOException {
        start(urlRequest);

        long timeOutMs = mChain.call().timeout().timeoutNanos() / 1000000;
        //Log.e(TAG, "timeout:" + timeOutMs);
        if (timeOutMs > 0) {
            mResponseCondition.block(timeOutMs);
        } else {
            mResponseCondition.block();
        }
        if (!urlRequest.isDone()) {
            urlRequest.cancel();
            mException = new IOException("time out");
        }


        if (mException != null) {
            throw mException;
        }

        return mResponseBuilder.build();
    }

    /**
     * 请求成功后，通知子类结束阻塞，返回response
     *
     * @param response Response
     */
    @Override
    public void onSuccess(@NonNull Response response) {
        mResponseCondition.open();
    }

    /**
     * 当发生错误时，通知子类终止阻塞抛出错误
     *
     * @param error IOException
     */
    @Override
    public void onError(@NonNull IOException error) {
        mException = error;
        mResponseCondition.open();
    }
}
