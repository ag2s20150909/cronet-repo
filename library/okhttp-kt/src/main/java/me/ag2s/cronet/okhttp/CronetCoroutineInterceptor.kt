package me.ag2s.cronet.okhttp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import me.ag2s.cronet.CronetHolder
import me.ag2s.cronet.CronetPreloader
import okhttp3.*
import org.chromium.net.CronetEngine
import org.chromium.net.UrlRequest
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class CronetCoroutineInterceptor(
    private val engine: CronetEngine = CronetHolder.getEngine(),
    private val cookieJar: CookieJar = CookieJar.NO_COOKIES,
    private val context: CoroutineContext = Dispatchers.IO
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (CronetPreloader.getInstance().isJavaImplement) {
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


        return runBlocking(context) {
            val copy: Request = builder.build()
            val timeout = chain.call().timeout().timeoutNanos().toDuration(DurationUnit.NANOSECONDS)
            if (timeout.isPositive()) {
                withTimeout(timeout) {
                    proceedWithCronet(engine = engine, copy, chain)
                }
            } else {
                proceedWithCronet(engine = engine, copy, chain)
            }

        }
    }

    @Throws(IOException::class)
    private suspend fun proceedWithCronet(
        engine: CronetEngine,
        request: Request,
        chain: Interceptor.Chain
    ): Response =
        suspendCancellableCoroutine { continuation ->

            val cb = object : AbsStreamCallback(chain) {
                override fun waitForDone(urlRequest: UrlRequest): Response {
                    TODO("Not yet implemented")
                }

                override fun onSuccess(response: Response) {
                    continuation.resume(response)
                }

                override fun onError(error: IOException) {
                    error.printStackTrace()
                    continuation.resumeWithException(error)
                    //row error
                }

            }


                   val urlRequest = CronetHelper.buildRequest(engine, request, cb)
                   continuation.invokeOnCancellation {
                       urlRequest.cancel()
                       chain.call().cancel()
                   }
                   cb.start(urlRequest)







        }


}