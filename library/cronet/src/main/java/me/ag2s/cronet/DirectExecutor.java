package me.ag2s.cronet;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

public enum DirectExecutor implements Executor {
    INSTANCE;

    @Override
    public void execute(Runnable command) {
        command.run();
    }

    @NonNull
    @Override
    public String toString() {
        return "MoreExecutors.directExecutor()";
    }
}