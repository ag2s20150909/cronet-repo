package me.ag2s.cronet.okhttp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import me.ag2s.cronet.CronetLoader
import okhttp3.*
import org.chromium.net.CronetEngine
import org.chromium.net.UrlRequest
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CronetCoroutineInterceptor(
    private val engine: CronetEngine,
    private val cookieJar: CookieJar = CookieJar.NO_COOKIES
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
        return runBlocking(Dispatchers.IO) {
            proceedWithCronet(engine = engine, copy, chain.call())
        }
    }

    private suspend fun proceedWithCronet(
        engine: CronetEngine,
        request: Request,
        call: Call
    ): Response =
        suspendCancellableCoroutine {
            val cb = object : AbsCronetCallback(request, call) {
                override fun waitForDone(urlRequest: UrlRequest): Response {
                    TODO("Not yet implemented")
                }

                override fun onSuccess(response: Response) {
                    it.resume(response)
                }

                override fun onError(error: IOException) {
                    it.resumeWithException(error)
                }

            }
            CronetHelper.buildRequest(engine, request, cb).start()


        }


}