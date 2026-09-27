package me.ag2s.cronet.okhttp;

import androidx.annotation.NonNull;

import org.chromium.net.UrlRequest;

import java.io.IOException;

import okhttp3.Response;

public interface AbsCallback {


    Response waitForDone(@NonNull UrlRequest urlRequest) throws IOException;

    /**
     * 请求成功后，通知子类结束阻塞，返回response
     *
     * @param response Response
     */
    void onSuccess(@NonNull Response response);

    /**
     * 当发生错误时，通知子类终止阻塞抛出错误
     *
     * @param error IOException
     */
    void onError(@NonNull IOException error);




}
