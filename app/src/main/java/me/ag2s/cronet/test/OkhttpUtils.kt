package me.ag2s.cronet.test

import android.util.Log
import android.webkit.URLUtil
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.chromium.net.impl.ImplVersion
import java.io.IOException
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.jvm.Throws

@Suppress("unused")
object OkhttpUtils {

    private val imageUrls = arrayOf(
        "https://storage.googleapis.com/cronet/sun.jpg",
        "https://storage.googleapis.com/cronet/flower.jpg",
        "https://storage.googleapis.com/cronet/chair.jpg",
        "https://storage.googleapis.com/cronet/white.jpg",
        "https://storage.googleapis.com/cronet/moka.jpg",
        "https://storage.googleapis.com/cronet/walnut.jpg"
    )
    private val r = Random(System.currentTimeMillis())

    fun getRandomImgLink(): String {
        return imageUrls[r.nextInt(imageUrls.size)] + "?_r=" + System.currentTimeMillis()
    }

    @Volatile
    private var okHttpClient: OkHttpClient? = null


    fun setOkhttpClent(client: OkHttpClient) {
        okHttpClient = client
    }


    private fun getOkhttpClient(): OkHttpClient {
        if (okHttpClient == null) {
            synchronized(OkhttpUtils) {
                if (okHttpClient == null) {
                    okHttpClient = Http.okHttpClient
                }
            }
        }
        return okHttpClient!!
    }


    @JvmField
    val executor: ExecutorService = Executors.newCachedThreadPool()
    private val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
    private val TAG = OkhttpUtils::class.java.simpleName
    private const val HTTP_ERROR = "error:"
    fun cancelAll() {
        Http.cancelAll()
    }

    fun httpGetPC(url: String?): String {
        val header = HashMap<String, String>()
        header["User-Agent"] = PcUserAgent
        header["Sec-Ch-Ua-Mobile"] = "?0"
        return httpGet(url, header)
    }

    fun httpCacheGetPC(url: String?): String {
        val header = HashMap<String, String>()
        header["User-Agent"] = PcUserAgent
        header["Sec-Ch-Ua-Mobile"] = "?0"
        return httpCacheGet(url, header)
    }

    fun httpHead(url: String): String {
        val client: OkHttpClient = getOkhttpClient()
        val requestBuilder: Request.Builder = Request.Builder().url(url)
        requestBuilder.method("HEAD", null)
        val refer = url.substring(0, url.lastIndexOf("/") + 1)
        requestBuilder.header("Referer", refer)
        requestBuilder.removeHeader("User-Agent")
        requestBuilder.header("User-Agent", UA)
        val request: Request = requestBuilder.build()
        return try {
            val response: Response = client.newCall(request).execute()
            Log.d(TAG, "get 302 url:" + response.request.url.toString())
            response.request.url.toString()
        } catch (e: Exception) {
            request.url.toString()
        }
    }

    fun getContentType(url: String): String? {
        val client: OkHttpClient = getOkhttpClient()
        val requestBuilder: Request.Builder = Request.Builder().url(url)
        requestBuilder.method("HEAD", null)
        val refer = url.substring(0, url.lastIndexOf("/") + 1)
        requestBuilder.header("Referer", refer)
        requestBuilder.removeHeader("User-Agent")
        requestBuilder.header("User-Agent", UA)
        val request: Request = requestBuilder.build()
        return try {
            val response: Response = client.newCall(request).execute()
            response.header("content-type", "text/html")
        } catch (e: Exception) {
            request.url.toString()
        }
    }

    @Throws(IOException::class)
    fun getResponse(
        url: String?,
        header: Map<String, String> = mapOf(),
        client: OkHttpClient = getOkhttpClient()
    ): Response {
        if (url == null || !URLUtil.isNetworkUrl(url)) {
            throw IllegalArgumentException("url is null or not NetworkUrl")
        }
        val requestBuilder: Request.Builder = Request.Builder().get().url(url)
        //requestBuilder.header("Referer", CommonTool.getReferer(url))
        requestBuilder.header("Dnt", "1")
        requestBuilder.removeHeader("User-Agent")
        requestBuilder.header("User-Agent", UA)
        requestBuilder.header("Sec-Ch-Ua-Mobile", "?1")
        requestBuilder.header("Sec-GPC", "1")
        requestBuilder.header("Upgrade-Insecure-Requests", "1")
        requestBuilder.header(
            "Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7,ru;q=0.6,ko;q=0.5"
        )
        for ((key, value) in header) {
            requestBuilder.removeHeader(key)
            requestBuilder.addHeader(key, value)
        }
        val request: Request = requestBuilder.build()
        return client.newCall(request).execute()
    }

    @Throws(IOException::class)
    fun postResponse(
        url: String?,
        headers: Map<String, String> = mapOf<String, String>(),
        body: Map<String, String>,
        client: OkHttpClient = getOkhttpClient()
    ): Response {
        if (url == null || !URLUtil.isNetworkUrl(url)) {
            throw IllegalArgumentException("url is null or not NetworkUrl")
        }
        //构建Body
        val params = FormBody.Builder()
        for ((key, value) in body) {

            params.add(key, value)
        }

        //构建headers
        val refer = url.substring(0, url.lastIndexOf("/") + 1)
        val rbuilder: Request.Builder = Request.Builder()
            .header("Sec-Ch-Ua-Mobile", "?0")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Referer", refer)
            .header("User-Agent", PcUserAgent)
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7,ru;q=0.6,ko;q=0.5")
            .url(url)
            .post(params.build())
        for ((key, value) in headers) {
            rbuilder.removeHeader(key)
            rbuilder.addHeader(key, value)
        }
        val request: Request = rbuilder.build()
        return client.newCall(request).execute()
    }

    @Throws(IOException::class)
    fun postJsonResponse(
        url: String?,
        data: String,
        headers: Map<String, String> = mapOf<String, String>(),
        client: OkHttpClient = getOkhttpClient()
    ): Response {
        if (url == null || !URLUtil.isNetworkUrl(url)) {
            throw IllegalArgumentException("url is null or not NetworkUrl")
        }

        val body: RequestBody = data.toRequestBody(JSON)

//构建headers
        val refer = url.substring(0, url.lastIndexOf("/") + 1)
        val rbuilder: Request.Builder = Request.Builder()
            .header("Sec-Ch-Ua-Mobile", "?0")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Referer", refer)
            .header("User-Agent", PcUserAgent)
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7,ru;q=0.6,ko;q=0.5")
            .url(url)
            .post(body)
        if (headers != null) {
            for ((key, value) in headers) {
                if (key == null) {
                    continue
                }
                rbuilder.removeHeader(key)
                if (!value.isNullOrBlank()) {
                    rbuilder.addHeader(key, value)
                }

            }
        }
        val request: Request = rbuilder.build()
        return client.newCall(request).execute()
    }


    fun httpGet(url: String?, header: Map<String, String> = mapOf()): String {

        return try {
            val response: Response = getResponse(url, header, getOkhttpClient())
            if (response.isSuccessful) {
                response.body!!.string()
            } else {
                HTTP_ERROR + response.message + " errorcode:" + response.code
            }
        } catch (e: Exception) {
            e.printStackTrace()
            HTTP_ERROR + Log.getStackTraceString(e)
        }
    }

    fun httpCacheGet(url: String?, header: Map<String, String> = mapOf()): String {

        return try {
            val response: Response = getResponse(url, header, getOkhttpClient())
            if (response.isSuccessful) {
                Objects.requireNonNull(response.body!!.string())
            } else {
                HTTP_ERROR + response.message + " errorcode:" + response.code
            }
        } catch (e: Exception) {
            e.printStackTrace()
            HTTP_ERROR + Log.getStackTraceString(e)
        }
    }


    fun httpPost(
        url: String,
        headers: Map<String, String> = mapOf<String, String>(),
        body: Map<String, String>
    ): String {

        return try {
            val response: Response = postResponse(url, headers, body)
            if (response.isSuccessful) {
                Objects.requireNonNull(response.body!!.string())
            } else {
                HTTP_ERROR + response.message + " errorcode:" + response.code
            }
        } catch (e: Exception) {

            HTTP_ERROR + Log.getStackTraceString(e)
        }
    }


    fun httpPostJson(
        url: String,
        data: String,
        headers: Map<String, String> = mapOf<String, String>()
    ): String {
        val client: OkHttpClient = getOkhttpClient()

        return try {
            val response: Response = postJsonResponse(url, data,headers)
            if (response.isSuccessful) {
                Objects.requireNonNull(response.body!!.string())
            } else {
                HTTP_ERROR + response.message + " errorcode:" + response.code
            }
        } catch (e: Exception) {
            HTTP_ERROR + Log.getStackTraceString(e)
        }
    }


    val PcUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + ImplVersion.getCronetVersion()
            .toString() + " Safari / 537.36"
    val UA =
        "Mozilla/5.0 (Linux; Android 12; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + ImplVersion.getCronetVersion()
            .toString() + " Mobile Safari/537.36"
}