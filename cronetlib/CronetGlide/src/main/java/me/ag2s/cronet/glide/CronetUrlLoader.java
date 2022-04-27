package me.ag2s.cronet.glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import org.chromium.net.CronetEngine;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class CronetUrlLoader<T, B extends Buffer> implements ModelLoader<T, ByteBuffer> {
    private final CronetEngine engine;
    CronetUrlLoader(CronetEngine cronetEngine){
        this.engine=cronetEngine;
    }
    @Nullable
    @Override
    public LoadData<ByteBuffer> buildLoadData(@NonNull T model, int width, int height, @NonNull Options options) {
        if (model instanceof GlideUrl){
            GlideUrl t= (GlideUrl) model;
            return new LoadData<>(t, /*fetcher=*/ new CronetDataFetcher<>(engine, t));
        }else {
            String s= (String) model;
            return new LoadData<>(new ObjectKey(s), new CronetDataFetcher<>(engine, s));
        }

    }

    @Override
    public boolean handles(@NonNull T url) {
        if (url instanceof GlideUrl){
            return true;
        }
        return url.toString().startsWith("http://")||url.toString().startsWith("https://");
    }
}
