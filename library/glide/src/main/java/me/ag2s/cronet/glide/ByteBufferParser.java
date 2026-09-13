package me.ag2s.cronet.glide;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * Parses a {@link ByteBuffer} to a particular data type.
 *
 * @param <T> The type of data to parse the buffer to.
 */
interface ByteBufferParser<T> {
    /**
     * Returns the required type of data parsed from the given {@link ByteBuffer}.
     */
    T parse(@NonNull ByteBuffer byteBuffer);

    /**
     * Returns the {@link Class} of the data that will be parsed from {@link ByteBuffer}s.
     */
    Class<T> getDataClass();
}