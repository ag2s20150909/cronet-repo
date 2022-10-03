package me.ag2s.cronet.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import org.chromium.net.CronetEngine;
import org.chromium.net.UrlRequest;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

import me.ag2s.cronet.CoronetHolder;

public class CronetUrlLoaderFactory<T> implements ModelLoaderFactory<T, ByteBuffer> {

    static final Map<Priority, Integer> GLIDE_TO_CHROMIUM_PRIORITY =
            new EnumMap<>(Priority.class);


    CronetUrlLoaderFactory() {

    }

    static {
        GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.IMMEDIATE, UrlRequest.Builder.REQUEST_PRIORITY_HIGHEST);
        GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.HIGH, UrlRequest.Builder.REQUEST_PRIORITY_MEDIUM);
        GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.NORMAL, UrlRequest.Builder.REQUEST_PRIORITY_LOW);
        GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.LOW, UrlRequest.Builder.REQUEST_PRIORITY_LOWEST);
    }

    @NonNull
    @Override
    public ModelLoader<T, ByteBuffer> build(@NonNull MultiModelLoaderFactory multiModelLoaderFactory) {
        return new CronetUrlLoader<T, ByteBuffer>(CoronetHolder.getEngine());
    }

    @Override
    public void teardown() {

    }
}
