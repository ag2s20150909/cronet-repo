package me.ag2s.cronet.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.util.ByteBufferUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class CronetUrlLoader<T> implements ModelLoader<GlideUrl, T> {


    private final ByteBufferParser<T> parser;

    CronetUrlLoader(ByteBufferParser<T> parser) {
        this.parser = parser;
    }

    @Override
    public LoadData<T> buildLoadData(@NonNull GlideUrl glideUrl, int width, int height, @NonNull Options options) {
        DataFetcher<T> fetcher = new CronetDataFetcher<>(parser, glideUrl);
        return new LoadData<>(glideUrl, fetcher);
    }

    @Override
    public boolean handles(@NonNull GlideUrl url) {
        return true;
    }

    /**
     * Loads {@link InputStream}s for {@link GlideUrl}s using cronet.
     */
    public static final class StreamFactory
            implements ModelLoaderFactory<GlideUrl, InputStream>, ByteBufferParser<InputStream> {

        public StreamFactory() {

        }

        @Override
        public ModelLoader<GlideUrl, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new CronetUrlLoader<>(this /*parser*/);
        }

        @Override
        public void teardown() {
        }

        @Override
        public InputStream parse(ByteBuffer byteBuffer) {
            try {
                return ByteBufferUtil.toStream(byteBuffer);
            } finally {
                byteBuffer.clear();
            }

        }

        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }
    }

    /**
     * Loads {@link ByteBuffer}s for {@link GlideUrl}s using cronet.
     */
    public static final class ByteBufferFactory
            implements ModelLoaderFactory<GlideUrl, ByteBuffer>, ByteBufferParser<ByteBuffer> {


        public ByteBufferFactory() {
        }

        @Override
        public ModelLoader<GlideUrl, ByteBuffer> build(MultiModelLoaderFactory multiFactory) {
            return new CronetUrlLoader<>(this /*parser*/);
        }

        @Override
        public void teardown() {
            // Do nothing.
        }

        @Override
        public ByteBuffer parse(ByteBuffer byteBuffer) {
            return byteBuffer;
        }

        @Override
        public Class<ByteBuffer> getDataClass() {
            return ByteBuffer.class;
        }
    }
}



