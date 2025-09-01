package me.ag2s.ktor

import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.callContext
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.sse.SSECapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.util.date.GMTDate
import io.ktor.util.flattenForEach
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.toByteArray
import io.ktor.utils.io.writeByteBuffer
import io.ktor.utils.io.writer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.chromium.net.CronetException
import org.chromium.net.UploadDataProvider
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import org.chromium.net.apihelpers.UploadDataProviders
import java.nio.ByteBuffer
import java.nio.channels.Pipe
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val METHODS_WITHOUT_BODY = listOf(HttpMethod.Get, HttpMethod.Head)

private val ENCODINGS_HANDLED_BY_CRONET = setOf("br", "deflate", "gzip", "x-gzip")


@OptIn(InternalAPI::class)
class CronetClientEngine(override val config: CronetConfig) :
    HttpClientEngineBase("ktor-cronet") {
    override val supportedCapabilities = hashSetOf(HttpTimeoutCapability, SSECapability)


    private val cronetEngine = config.preconfigured
    private val executor by lazy { dispatcher.asExecutor() }

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val callContext = callContext()
        return executeHttpRequest(callContext, data)
    }

    private suspend fun executeHttpRequest(
        callContext: CoroutineContext,
        data: HttpRequestData
    ): HttpResponseData {
        val requestTime = GMTDate()

        val pipe = Pipe.open()

        var job:Job?=null


        // All chunked response is written to this.
        val receiveChannel = pipe.sink().apply {
            configureBlocking(true)
        }

        val sourceChannel = pipe.source().apply {
            configureBlocking(true)
        }

        val uploadDataProvider =data.body.toUploadDataProvider()




        return suspendCancellableCoroutine { continuation ->
            val callback = object : UrlRequest.Callback() {
                override fun onRedirectReceived(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    newLocationUrl: String
                ) {
                    if (config.followRedirects) {
                        request.followRedirect()
                    } else {
                        request.cancel()
                        continuation.resume(
                            info.toHttpResponseData(
                                requestTime = requestTime,
                                callContext = callContext,
                            )
                        )
                    }
                }

                override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {

                    request.read(ByteBuffer.allocateDirect(config.responseBufferSize))

                    job=CoroutineScope(callContext).launch {
                        val buffer = ByteBuffer.allocate(config.responseBufferSize)
                        val response = writer {
                            while (sourceChannel.isOpen) {
                                if (!channel.isClosedForWrite) {
                                    buffer.clear()
                                    val bytesRead = sourceChannel.read(buffer)

                                    if (bytesRead < 0) {
                                        sourceChannel.close()
                                        channel.flushAndClose()
                                    }
                                    buffer.flip()
                                    channel.writeByteBuffer(buffer)
                                } else {
                                    break
                                }
                            }

                        }.channel

                        continuation.resume(
                            info.toHttpResponseData(
                                requestTime = requestTime,
                                callContext = callContext,
                                responseBody = response,
                            )
                        )
                    }


                }

                override fun onReadCompleted(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    byteBuffer: ByteBuffer
                ) {
                    // Write current received response data to responseCache
                    byteBuffer.flip()
                    receiveChannel.write(byteBuffer)
                    // Continue reading
                    byteBuffer.clear()

                    request.read(byteBuffer)
                }

                override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {

                    receiveChannel.close()
                }


                override fun onFailed(
                    request: UrlRequest,
                    info: UrlResponseInfo?,
                    error: CronetException
                ) {
                    receiveChannel.close()
                    continuation.resumeWithException(error)
                }

                override fun onCanceled(request: UrlRequest, info: UrlResponseInfo?) {
                    receiveChannel.close()
                    continuation.resumeWithException(CancellationException("Request was cancelled"))
                }
            }

            val request = cronetEngine.newUrlRequestBuilder(
                /* url = */ data.url.toString(),
                /* callback = */ callback,
                /* executor = */ executor,
            ).apply {
                setHttpMethod(data.method.value)

                data.headers.flattenForEach { key, value ->
                    addHeader(key, value)
                }


                    uploadDataProvider?.let {
                        setUploadDataProvider(it, executor)
                    }

                    data.body.contentType?.let {
                        addHeader(HttpHeaders.ContentType, "${it.contentType}/${it.contentSubtype}")
                    }



            }.build()

            continuation.invokeOnCancellation {
                job?.cancel()
                request.cancel()
            }

            request.start()
        }
    }


}

private fun UrlResponseInfo.toHttpResponseData(
    requestTime: GMTDate,
    callContext: CoroutineContext,
    responseBody: ByteReadChannel? = null,
): HttpResponseData {
    return HttpResponseData(
        statusCode = HttpStatusCode.fromValue(httpStatusCode),
        requestTime = requestTime,
        headers = Headers.build {

            val result: Boolean = keepEncodingAffectedHeaders()
            allHeaders.forEach { (key, value) ->
                if (key == HttpHeaders.ContentLength && result) {
                    append(HttpHeaders.ContentLength, "-1")
                } else {
                    appendAll(key, value)
                }
                //appendAll(key, value)
            }

        },
        version = when (negotiatedProtocol) {
            "h2" -> HttpProtocolVersion.HTTP_2_0
            "h3" -> HttpProtocolVersion.QUIC
            "quic/1+spdy/3" -> HttpProtocolVersion.SPDY_3
            else -> HttpProtocolVersion.HTTP_1_1
        },
        body = responseBody ?: ByteReadChannel.Empty,
        callContext = callContext,
    )
}

private fun UrlResponseInfo.keepEncodingAffectedHeaders(): Boolean {
    allHeaders[HttpHeaders.ContentEncoding]?.forEach {
        if (ENCODINGS_HANDLED_BY_CRONET.contains(it)) {
            return true
        }
    }
    return false
}

private suspend fun OutgoingContent.toUploadDataProvider(): UploadDataProvider? {
    return when (val outgoingContent = this) {
        is OutgoingContent.NoContent -> null

        is OutgoingContent.ContentWrapper -> outgoingContent.delegate().toUploadDataProvider()

        is OutgoingContent.ByteArrayContent -> {
            UploadDataProviders.create(outgoingContent.bytes())
        }

        is OutgoingContent.ReadChannelContent -> {
            UploadDataProviders.create(outgoingContent.readFrom().toByteArray())
        }

        is OutgoingContent.WriteChannelContent -> coroutineScope {
            UploadDataProviders.create(toReadChannel(outgoingContent).toByteArray())
        }

        is OutgoingContent.ProtocolUpgrade -> error("UnsupportedContentType $this")
    }
}


private fun CoroutineScope.toReadChannel(content: OutgoingContent.WriteChannelContent): ByteReadChannel {
    return writer(Dispatchers.IO) {
        content.writeTo(channel)
    }.channel
}

