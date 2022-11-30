package me.ag2s.cronet.okhttp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import me.ag2s.cronet.CronetHolder
import me.ag2s.cronet.CronetLoader
import okhttp3.*
import org.chromium.net.CronetEngine
import org.chromium.net.UrlRequest
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CronetCoroutineInterceptor(
    private val engine: CronetEngine = CronetHolder.getEngine(),
    private val cookieJar: CookieJar = CookieJar.NO_COOKIES,
    private val context: CoroutineContext = Dispatchers.IO
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        chain.connectTimeoutMillis()


        if (!CronetLoader.getInstance().install()) {
            return chain.proceed(request)
        }
        val builder: Request.Builder = request.newBuilder()
        builder.removeHeader("Keep-Alive")
        builder.removeHeader("Accept-Encoding")
        if (cookieJar != CookieJar.NO_COOKIES) {
            val cookieString = CronetHelper.getCookieString(cookieJar, request.url)
            if (cookieString.length > 4) {
                builder.header("Cookie", cookieString)
            }
        }
        val copy: Request = builder.build()
        return runBlocking(context) {
            proceedWithCronet(engine = engine, copy, chain.call())
        }
    }

    private suspend fun proceedWithCronet(
        engine: CronetEngine,
        request: Request,
        call: Call
    ): Response =
        suspendCancellableCoroutine { continuation ->

            val cb = object : AbsCronetMemoryCallback(request, call) {
                override fun waitForDone(urlRequest: UrlRequest): Response {
                    TODO("Not yet implemented")
                }

                override fun onSuccess(response: Response) {
                    continuation.resume(response)
                }

                override fun onError(error: IOException) {
                    continuation.resumeWithException(error)
                }

            }

            cb.use {
                val urlRequest = CronetHelper.buildRequest(engine, request, cb)
                continuation.invokeOnCancellation {
                    urlRequest.cancel()
                    call.cancel()
                }
                urlRequest.start()
            }


        }


}