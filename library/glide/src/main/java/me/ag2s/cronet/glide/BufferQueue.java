package me.ag2s.cronet.glide;

import androidx.annotation.NonNull;

import org.chromium.net.UrlResponseInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;

/**
 * A utility for processing response bodies, as one contiguous buffer rather than an asynchronous
 * stream.
 */
final class BufferQueue implements AutoCloseable {

    private final ByteArrayOutputStream mResponseBodyStream;

    private BufferQueue(ByteArrayOutputStream mResponseBodyStream) {
        this.mResponseBodyStream = mResponseBodyStream;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void close() throws Exception {
        if (mResponseBodyStream != null) {
            mResponseBodyStream.close();
        }
    }

    /**
     * Returns the response body as a single contiguous buffer.
     */
    public ByteBuffer coalesceToBuffer() {
        return ByteBuffer.wrap(mResponseBodyStream.toByteArray());
    }


    /**
     * Use this class during a request, to combine streamed buffers of a response into a single final
     * buffer.
     *
     * <p>For example: {@code @Override public void onResponseStarted(UrlRequest request,
     * UrlResponseInfo info) { request.read(builder.getFirstBuffer(info)); } @Override public void
     * onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer buffer) {
     * request.read(builder.getNextBuffer(buffer)); } }
     */
    public static final class Builder {

        private static final int BYTE_BUFFER_CAPACITY = 32 * 1024;
        private static final String CONTENT_LENGTH_HEADER_NAME = "Content-Length";
        // See ArrayList.MAX_ARRAY_SIZE for reasoning.
        private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

        private ByteArrayOutputStream mResponseBodyStream;
        private WritableByteChannel mResponseBodyChannel;

        private Builder() {
        }

        /**
         * Returns the numerical value of the Content-Header length, or 32 if not set or invalid.
         */
        private static long getBodyLength(@NonNull UrlResponseInfo info) {
            List<String> contentLengthHeader = info.getAllHeaders().get(CONTENT_LENGTH_HEADER_NAME);
            if (contentLengthHeader == null || contentLengthHeader.size() != 1) {
                return 32;
            }
            try {
                return Long.parseLong(contentLengthHeader.get(0));
            } catch (NumberFormatException e) {
                return 32;
            }
        }

        /**
         * Returns the next buffer to write data into.
         */
        public ByteBuffer getNextBuffer(ByteBuffer lastBuffer) throws IOException {
            lastBuffer.flip();
            mResponseBodyChannel.write(lastBuffer);
            lastBuffer.clear();
            return lastBuffer;
        }

        /**
         * Returns a ByteBuffer heuristically sized to hold the whole response body.
         */
        public ByteBuffer getFirstBuffer(UrlResponseInfo info) throws IllegalArgumentException {
            // Security note - a malicious server could attempt to exhaust client memory by sending
            // down a Content-Length of a very large size, which we would eagerly allocate without
            // the server having to actually send those bytes. This isn't considered to be an
            // issue, because that same malicious server could use our transparent gzip to force us
            // to allocate 1032 bytes per byte sent by the server.
            long bodyLength = getBodyLength(info);
            if (bodyLength > MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException("The body is too large and wouldn't fit in a byte array!");
            }
            mResponseBodyStream = new ByteArrayOutputStream((int) bodyLength);
            mResponseBodyChannel = Channels.newChannel(mResponseBodyStream);
            return ByteBuffer.allocateDirect((int) Math.min(getBodyLength(info), BYTE_BUFFER_CAPACITY));
        }

        public BufferQueue build() {
            try {
                return new BufferQueue(mResponseBodyStream);
            } finally {
                try {
                    mResponseBodyChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}