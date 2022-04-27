package me.ag2s.cronet.glide;//package me.ag2s.lib.img;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.LibraryGlideModule;

import org.chromium.net.UrlRequest;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


@GlideModule
public final class CronetLibraryGlideModule extends LibraryGlideModule {

    static final Map<Priority, Integer> GLIDE_TO_CHROMIUM_PRIORITY =
            new EnumMap<>(Priority.class);

    static {
        GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.IMMEDIATE, UrlRequest.Builder.REQUEST_PRIORITY_HIGHEST);
        GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.HIGH, UrlRequest.Builder.REQUEST_PRIORITY_MEDIUM);
        GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.NORMAL, UrlRequest.Builder.REQUEST_PRIORITY_LOW);
        GLIDE_TO_CHROMIUM_PRIORITY.put(Priority.LOW, UrlRequest.Builder.REQUEST_PRIORITY_LOWEST);
    }

    public static final Executor executor= Executors.newFixedThreadPool(16);
    @Override
    public void registerComponents(
            @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.replace(GlideUrl.class, ByteBuffer.class, new CronetUrlLoaderFactory<GlideUrl>());
        registry.replace(String.class, ByteBuffer.class, new CronetUrlLoaderFactory<String>());
    }

}
