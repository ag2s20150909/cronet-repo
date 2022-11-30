package me.ag2s.cronet.test

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import me.ag2s.cronet.okhttp.CronetFileCallBack
import me.ag2s.cronet.okhttp.CronetHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.chromium.net.UrlResponseInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


@Composable
fun DownloadFileScreen() {
    val viewModel: DownloadFileViewModel = viewModel()
    val result by viewModel.result.collectAsState()
    val context = LocalContext.current

    val chooseFile = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { fileUri ->
        if (fileUri != null) {
            val file = DocumentFile.fromSingleUri(context, fileUri)
            if (file != null) {
                val name = file.name
                viewModel.uploadFile(
                    context.contentResolver.openInputStream(fileUri)!!,
                    name.toString()
                )
            }

        }
    }


    Column() {

        Button(onClick = { viewModel.speedTest() }) { Text(text = "Download") }
        Button(onClick = { chooseFile.launch("*/*") }) { Text(text = "Upload") }




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
                    .url("http://test.ustc.edu.cn/backend/garbage.php?r${Math.random()}&ckSize=100")
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

    var uploadFileJob: Job? = null

    fun uploadFile(ins: InputStream, fileName: String) {
        uploadFileJob?.cancel()
        uploadFileJob = viewModelScope.launch(Dispatchers.IO) {

            val testUpFile = File(appCtx.externalCacheDir, "testUp.bin")
            val outputStream = FileOutputStream(testUpFile)

            ins.copyTo(outputStream)

            ins.close()
            outputStream.flush()
            outputStream.close()


            val outFile = File(appCtx.externalCacheDir, "temp.bin")
            val cb = object : CronetFileCallBack(outFile) {
                override fun onHeaders(urlResponseInfo: UrlResponseInfo?) {
                    result.tryEmit(urlResponseInfo?.allHeadersAsList.toString())
                }

                override fun onSuccess() {
                    result.tryEmit("上传完成")
                }

                override fun onProgress(write: Long, total: Long) {
                    result.tryEmit("$write  $total")
                }

                override fun onError(error: IOException) {
                    result.tryEmit(error.stackTraceToString())
                }
            }

            val requestBody: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username", "test")
                .addFormDataPart("password", "test")
                .addFormDataPart(
                    "file", fileName,
                    testUpFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
                .build()
            val request: Request = Request.Builder()
                .url("https://192.168.1.4:8888/upload")
                .post(requestBody)
                .build()

            val urlRequest = CronetHelper.buildRequest(Http.cronetEngine, request, cb)
            result.tryEmit("上传开始")
            urlRequest.start()


        }
    }

}