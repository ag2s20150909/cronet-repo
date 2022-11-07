package me.ag2s.cronet.test

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import me.ag2s.cronet.okhttp.CronetFileCallBack
import me.ag2s.cronet.okhttp.CronetHelper
import okhttp3.Request
import org.chromium.net.UrlResponseInfo
import java.io.File
import java.io.IOException

@Composable
fun DownloadFileScreen() {
    val viewModel: DownloadFileViewModel = viewModel()
    val result by viewModel.result.collectAsState()
    Column() {
        Button(onClick = { viewModel.speedTest() }) { Text(text = "测试") }

        Text(text = result)
    }
}

class DownloadFileViewModel : ViewModel() {
    val result = MutableStateFlow("")
    var speedTestJob: Job? = null
    fun speedTest() {
        speedTestJob?.cancel()
        speedTestJob = viewModelScope.launch {
            suspendCancellableCoroutine<Unit> {

                val requestBuilder = Request.Builder()
                    .url("http://test.ustc.edu.cn/backend/garbage.php?r${Math.random()}&ckSize=1024")
                    .get()
                requestBuilder.header("Dnt", "1")
                requestBuilder.removeHeader("User-Agent")
                requestBuilder.header("User-Agent", OkhttpUtils.PcUserAgent)
                requestBuilder.header("Sec-Ch-Ua-Mobile", "?1")
                requestBuilder.header("Sec-GPC", "1")
                requestBuilder.header("Upgrade-Insecure-Requests", "1");
                requestBuilder.header(
                    "Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7,ru;q=0.6,ko;q=0.5"
                )

                val outFile = File(appCtx.externalCacheDir, "test.bin")
                val cb = object : CronetFileCallBack(outFile) {
                    override fun onHeaders(urlResponseInfo: UrlResponseInfo?) {
                        result.tryEmit(urlResponseInfo?.allHeadersAsList.toString())
                    }

                    override fun onSuccess() {
                        result.tryEmit("下载完成")
                    }

                    override fun onProgress(write: Long, total: Long) {
                        result.tryEmit("${write}  ${total}")
                    }

                    override fun onError(error: IOException) {
                        result.tryEmit(error.stackTraceToString())
                    }
                }
                val urlRequest =
                    CronetHelper.buildRequest(Http.cronetEngine, requestBuilder.build(), cb)
                urlRequest.start()
                it.invokeOnCancellation {
                    urlRequest.cancel()
                }
                result.tryEmit("下载开始")
            }


        }
    }

}