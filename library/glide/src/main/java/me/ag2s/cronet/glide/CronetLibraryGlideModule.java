package me.ag2s.cronet.glide;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.LibraryGlideModule;

import org.chromium.net.CronetEngine;
import org.chromium.net.ExperimentalCronetEngine;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@GlideModule
public final class CronetLibraryGlideModule extends LibraryGlideModule {
    public static final ExecutorService executor = Executors.newCachedThreadPool();



    @Override
    public void registerComponents(
            @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.replace(GlideUrl.class, ByteBuffer.class, new CronetUrlLoaderFactory());
    }
}
