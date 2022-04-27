package me.ag2s.cronet.glide;

import androidx.annotation.NonNull;


import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import org.chromium.net.CronetEngine;


import java.nio.ByteBuffer;

import me.ag2s.cronetlib.CronetClient;


public class CronetUrlLoaderFactory<T> implements ModelLoaderFactory<T, ByteBuffer> {



    private final CronetEngine engine;

    CronetUrlLoaderFactory() {
        this.engine = CronetClient.getInstance().getEngine();
    }



    @NonNull
    @Override
    public ModelLoader<T, ByteBuffer> build(@NonNull MultiModelLoaderFactory multiModelLoaderFactory) {
        return new CronetUrlLoader<T,ByteBuffer>(engine);
    }

    @Override
    public void teardown() {

    }
}
