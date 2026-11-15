package me.ag2s.coil


import coil3.network.NetworkRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.Buffer
import org.chromium.net.UploadDataProvider
import org.chromium.net.UploadDataSink
import java.io.IOException
import java.nio.ByteBuffer

internal class CronetBodyUploadProvider(val body: NetworkRequestBody,scope:CoroutineScope,val buffer: Buffer= Buffer()): UploadDataProvider() {
    init {

        scope.launch{
            buffer.clear()
            body.writeTo(buffer)
            buffer.flush()
        }

    }
    override fun getLength(): Long {
        return buffer.size
    }

    override fun read(
        uploadDataSink: UploadDataSink,
        byteBuffer: ByteBuffer
    ) {

        check(!byteBuffer.hasRemaining()){ IllegalArgumentException("Cronet passed a buffer with no bytes remaining") }
        var read = 0;
        while (read<=0){
            read = buffer.read(byteBuffer);
        }
        uploadDataSink.onReadSucceeded(false);
    }

    override fun rewind(uploadDataSink: UploadDataSink) {
        uploadDataSink.onRewindError(IOException("body is oneShot"));
    }
}