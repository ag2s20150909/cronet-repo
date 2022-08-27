package me.ag2s.cronet.test

import android.util.Log
import me.ag2s.cronet.okhttp.CronetInterceptor
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import org.chromium.net.CronetEngine
import org.chromium.net.CronetEngine.Builder.HTTP_CACHE_DISK
import org.chromium.net.MyCronetEngine
import org.chromium.net.NetworkQualityRttListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Http {
    fun cancelAll() {
       okHttpClient.dispatcher.cancelAll()
    }


    private val cronetEngine: CronetEngine by lazy {
        val builder = MyCronetEngine.Builder(appCtx).apply {
            setStoragePath(appCtx.externalCacheDir?.absolutePath)//设置缓存路径
            enableHttpCache(HTTP_CACHE_DISK, (1024 * 1024 * 50).toLong())//设置50M的磁盘缓存
            enableQuic(true)//设置支持http/3
            enableHttp2(true)  //设置支持http/2
            enableBrotli(true)//Brotli压缩
            enableNetworkQualityEstimator(true)
        }
        builder.build().also {
            it.addRttListener(object :
                NetworkQualityRttListener(Executors.newSingleThreadExecutor()) {
                override fun onRttObservation(rttMs: Int, whenMs: Long, source: Int) {
                    Log.e("RTT", "rtt:${rttMs} time:${whenMs} source:${source}")
                }

            })
        }
    }

    private val cronetInterceptor: me.ag2s.cronet.okhttp.CronetInterceptor by lazy {
        me.ag2s.cronet.okhttp.CronetInterceptor(cronetEngine)
    }

    val okHttpClient: OkHttpClient by lazy {
        val specs = arrayListOf(
            ConnectionSpec.MODERN_TLS,
            ConnectionSpec.COMPATIBLE_TLS,
            ConnectionSpec.CLEARTEXT
        )


        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionSpecs(specs)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(cronetInterceptor)

        builder.build()
    }
}