package me.ag2s.coil

import coil3.network.NetworkClient
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resumeWithException


internal val executorService: Executor by lazy { Executors.newFixedThreadPool(6) }

internal class CronetNetworkClient(val cronet: CronetEngine) : NetworkClient {
    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (NetworkResponse) -> T
    ) = suspendCancellableCoroutine<T> { continuation ->
        val scope = CoroutineScope(Dispatchers.IO)
        var networkResponse: NetworkResponse =
            NetworkResponse(requestMillis = System.currentTimeMillis())
        val buffer = Buffer()
        val callback = object : UrlRequest.Callback() {
            override fun onRedirectReceived(
                request: UrlRequest,
                info: UrlResponseInfo,
                newLocationUrl: String
            ) {
                request.followRedirect()
            }

            override fun onResponseStarted(
                request: UrlRequest, info: UrlResponseInfo
            ) {
                networkResponse = networkResponse.copy(
                    code = info.httpStatusCode,
                    headers = info.toHeaders(),
                    responseMillis = System.currentTimeMillis(),
                    body = NetworkResponseBody(source = buffer)
                )

                request.read(ByteBuffer.allocateDirect(4 * 1024))

            }

            override fun onReadCompleted(
                request: UrlRequest,
                info: UrlResponseInfo,
                byteBuffer: ByteBuffer
            ) {
                byteBuffer.flip()

                buffer.write(byteBuffer)

                byteBuffer.clear()
                request.read(byteBuffer)
            }

            override fun onSucceeded(
                request: UrlRequest, info: UrlResponseInfo
            ) {


                scope.launch {

                    try {


                        val r = block(networkResponse)
                        continuation.resume(r) { cause, _, _ ->

                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }


                }


            }

            override fun onFailed(
                request: UrlRequest, info: UrlResponseInfo?, error: CronetException
            ) {
                continuation.resumeWithException(error)
            }

        }


        val urlRequest = cronet.newUrlRequestBuilder(request.url, callback, executorService)

            .apply {
                setHttpMethod(request.method)
                disableCache()
                allowDirectExecutor()
                setHeaders(request.headers)
                if (request.body != null) {
                    setUploadDataProvider(
                        CronetBodyUploadProvider(request.body!!, scope),
                        executorService
                    )
                }
            }


            .build()



        continuation.invokeOnCancellation {
            if (!urlRequest.isDone) {
                urlRequest.cancel()
            }


        }
        urlRequest.start()


    }
}

private fun UrlRequest.Builder.setHeaders(headers: NetworkHeaders): UrlRequest.Builder {

    headers.asMap().forEach {
        this.addHeader(it.key, headers[it.key])
    }
    this.addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
    return this
}

private fun UrlResponseInfo.toHeaders(): NetworkHeaders {
    val builder = NetworkHeaders.Builder()
    this.allHeaders.forEach {
        builder[it.key] = it.value
    }
    return builder.build()
}

