package me.ag2s.cronet.okhttp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.chromium.net.CronetException;

import java.nio.ByteBuffer;

public class CallbackResult {
    @NonNull
    final CallbackStep     CallbackStep;
    @Nullable
    ByteBuffer byteBuffer =null;
    @Nullable
    CronetException exception=null;

    public CallbackResult(@NonNull CallbackStep callbackStep) {
        this.CallbackStep = callbackStep;
    }

    public CallbackResult(@NonNull CallbackStep callbackStep, @Nullable ByteBuffer byteBuffer) {
        this.CallbackStep = callbackStep;
        this.byteBuffer=byteBuffer;
    }

    public CallbackResult(@NonNull CallbackStep callbackStep, @Nullable ByteBuffer byteBuffer, @Nullable CronetException exception) {
        this.CallbackStep = callbackStep;
        this.byteBuffer=byteBuffer;
        this.exception=exception;
    }
}
