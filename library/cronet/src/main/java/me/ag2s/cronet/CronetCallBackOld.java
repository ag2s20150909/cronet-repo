package me.ag2s.cronet;

import android.os.ConditionVariable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.chromium.net.UrlRequest;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.EventListener;
import okhttp3.Request;
import okhttp3.Response;

class CronetCallBackOld extends AbsCronetCallback {
    private final ConditionVariable mResponseConditon = new ConditionVariable();
    @Nullable
    private IOException mException;

    CronetCallBackOld(Request request, Call call) {
        super(request, call);
    }

    @Override
    Response waitForDone(@NonNull UrlRequest urlRequest) throws IOException {

        long timeOutMs = mCall.timeout().timeoutNanos() / 1000000;
        //Log.e(TAG, "timeout:" + timeOutMs);
        if (timeOutMs > 0) {
            mResponseConditon.block(timeOutMs);
        } else {
            mResponseConditon.block();
        }
        if (!urlRequest.isDone()) {
            urlRequest.cancel();
            mException = new IOException("time out");
        }


        if (mException != null) {
            throw mException;
        }

        return mResponse;
    }

    /**
     * 请求成功后，通知子类结束阻塞，返回response
     *
     * @param response Response
     */
    @Override
    void onSuccess(@NonNull Response response) {
        mResponseConditon.open();
    }

    /**
     * 当发生错误时，通知子类终止阻塞抛出错误
     *
     * @param error IOException
     */
    @Override
    void onError(@NonNull IOException error) {
        mException = error;
        mResponseConditon.open();
    }
}
