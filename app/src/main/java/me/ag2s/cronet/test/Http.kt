package me.ag2s.cronet.test

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import me.ag2s.cronet.CronetHolder
import me.ag2s.cronet.okhttp.CronetCoroutineInterceptor
import me.ag2s.cronet.okhttp.CronetInterceptor
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import org.chromium.net.CronetEngine
import org.chromium.net.CronetEngine.Builder.HTTP_CACHE_DISK
import org.chromium.net.MyCronetEngine
import org.chromium.net.NetworkQualityObservationSource
import org.chromium.net.NetworkQualityRttListener
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Http {

    private val OkhttpDispatcher: CoroutineDispatcher by lazy { bootClient.dispatcher.executorService.asCoroutineDispatcher() }
    fun cancelAll() {
        bootClient.dispatcher.cancelAll()
    }

    private val bootClient: OkHttpClient by lazy {
        val specs = arrayListOf(
            ConnectionSpec.MODERN_TLS,
            ConnectionSpec.COMPATIBLE_TLS,
            ConnectionSpec.CLEARTEXT
        )
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionSpecs(specs)
            .followRedirects(true)
            .followSslRedirects(true)
            .build().also {
                CronetHolder.setExecutor(it.dispatcher.executorService)
            }
    }


    val cronetEngine: CronetEngine by lazy {
        val builder = MyCronetEngine.Builder(appCtx).apply {
            setStoragePath(appCtx.externalCacheDir?.absolutePath)//设置缓存路径
            enableHttpCache(HTTP_CACHE_DISK, (1024 * 1024 * 50).toLong())//设置50M的磁盘缓存
            enableQuic(true)//设置支持http/3
            enableHttp2(true)  //设置支持http/2
            enableBrotli(true)//Brotli压缩
            addQuicHint("storage.googleapis.com", 443, 443)
            addQuicHint("http3.is", 443, 443)
            setExperimentalOptions(options)
            enableNetworkQualityEstimator(true)
            setUserAgent(OkhttpUtils.PcUserAgent)
        }
        builder.build().also {
            it.addRttListener(object :
                NetworkQualityRttListener(Executors.newSingleThreadExecutor()) {
                override fun onRttObservation(rttMs: Int, whenMs: Long, source: Int) {
                    Log.e("RTT", "rtt:${rttMs} time:${whenMs} source:${source2String(source)}")
                }

            })
            CronetHolder.setEngine(it)
        }
    }

    private val options by lazy {
        val options = JSONObject()

        //设置域名映射规则
        //MAP hostname ip,MAP hostname ip
//    val host = JSONObject()
//    host.put("host_resolver_rules","")
//    options.put("HostResolverRules", host)

        //启用DnsHttpsSvcb更容易迁移到http3
        val dnsSvcb = JSONObject()
        dnsSvcb.put("enable", true)
        dnsSvcb.put("enable_insecure", true)
        dnsSvcb.put("use_alpn", true)
        options.put("UseDnsHttpsSvcb", dnsSvcb)

        options.put("AsyncDNS", JSONObject("{'enable':true}"))
        options.put("SSLMinVersionAtLeastTLS12", JSONObject("{'enable':false}"))
        options.put("EncryptedClientHello", JSONObject("{'enable':true}"))

        Log.e("Cronet", options.toString(4))


        options.toString()
    }

    private val cronetInterceptor: CronetInterceptor by lazy {
        CronetInterceptor(cronetEngine)
    }
    private val coroutineInterceptor: CronetCoroutineInterceptor by lazy {
        CronetCoroutineInterceptor(cronetEngine, context = OkhttpDispatcher)
    }

    val okHttpClient: OkHttpClient by lazy {

        val builder = bootClient.newBuilder()
            .addInterceptor(coroutineInterceptor)

        builder.build()
    }

    val okHttpClient1: OkHttpClient by lazy {

        val builder = bootClient.newBuilder()
            .addInterceptor(cronetInterceptor)

        builder.build()
    }


    fun source2String(@NetworkQualityObservationSource source: Int): String {
        return when (source) {
            NetworkQualityObservationSource.HTTP -> {
                "HTTP"
            }
            NetworkQualityObservationSource.TCP -> {
                "TCP"
            }
            NetworkQualityObservationSource.QUIC -> {
                "QUIC"
            }
            NetworkQualityObservationSource.HTTP_CACHED_ESTIMATE -> {
                "HTTP_CACHED_ESTIMATE"
            }
            NetworkQualityObservationSource.DEFAULT_HTTP_FROM_PLATFORM -> {
                "DEFAULT_HTTP_FROM_PLATFORM"
            }
            NetworkQualityObservationSource.DEPRECATED_HTTP_EXTERNAL_ESTIMATE -> {
                "DEPRECATED_HTTP_EXTERNAL_ESTIMATE"
            }
            NetworkQualityObservationSource.TRANSPORT_CACHED_ESTIMATE -> {
                "TRANSPORT_CACHED_ESTIMATE"
            }
            NetworkQualityObservationSource.DEFAULT_TRANSPORT_FROM_PLATFORM -> {
                "DEFAULT_TRANSPORT_FROM_PLATFORM"
            }
            NetworkQualityObservationSource.H2_PINGS -> {
                "H2_PINGS"
            }
            NetworkQualityObservationSource.MAX -> {
                "MAX"
            }
            else -> {
                "未知"
            }
        }
    }

}