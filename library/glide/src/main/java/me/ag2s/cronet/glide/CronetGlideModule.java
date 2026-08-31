package me.ag2s.cronet.glide;


import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.GlideModule;

import java.io.InputStream;
import java.nio.ByteBuffer;

@Keep
public class CronetGlideModule implements GlideModule {

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {

    }


    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.replace(GlideUrl.class, InputStream.class, new CronetUrlLoader.StreamFactory());
        registry.replace(GlideUrl.class, ByteBuffer.class, new CronetUrlLoader.ByteBufferFactory());
    }
}
