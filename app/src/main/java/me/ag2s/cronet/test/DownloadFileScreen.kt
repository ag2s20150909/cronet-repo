package me.ag2s.cronet.test

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
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
import me.ag2s.cronet.okhttp.CronetHelper
import me.ag2s.cronet.okhttp.CronetOutputStreamCallBack
import me.ag2s.cronet.okhttp.CronetParcelFileDescriptorCallback
import me.ag2s.cronet.okhttp.FileDescriptorRequestBody
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.chromium.net.UrlResponseInfo
import java.io.File
import java.io.IOException


@Composable
fun DownloadFileScreen() {
    val viewModel: DownloadFileViewModel = viewModel()
    val result by viewModel.result.collectAsState()
    val context = LocalContext.current

//    val requestSinglePermission =
//        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
//            if (permissionGranted) {
//                viewModel.setMessage("Single permission is granted.")
//            } else {
//                viewModel.setMessage("Single permission is denied.")
//            }
//        }
//
//    LaunchedEffect(Unit){
//        requestSinglePermission.launch("android.permission.WRITE_EXTERNAL_STORAGE")
//    }


    val createFile =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            Log.e("SS", "created file URI ${activityResult.data?.data}")
            if (activityResult.resultCode != RESULT_OK) {
                return@rememberLauncherForActivityResult
            }
            activityResult.data?.data?.let { uri ->

                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
// Check for the freshest data.
                appCtx.contentResolver.takePersistableUriPermission(uri, takeFlags)
                viewModel.downloadFile(uri)


            }
        }

    val chooseFile = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { fileUri ->
        if (fileUri != null) {
            val file = DocumentFile.fromSingleUri(context, fileUri)
            if (file != null) {
                val name = file.name
                viewModel.uploadFile(
                    fileUri, name.toString()
                )
            }

        }
    }


    Column() {

        Button(onClick = { viewModel.speedTest() }) { Text(text = "Download") }
        Button(onClick = {

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_TITLE, "test.bin")

            }
            createFile.launch(intent)


        }) { Text(text = "ChooseFileDownload") }
        Button(onClick = { chooseFile.launch("*/*") }) { Text(text = "Upload") }




        Text(text = result)
    }
}

class DownloadFileViewModel : ViewModel() {

    val result = MutableStateFlow("")


    fun setMessage(msg: String?) {
        viewModelScope.launch {
            msg?.let { result.emit(it) }
        }
    }

    var speedTestJob: Job? = null
    fun speedTest() {
        speedTestJob?.cancel()
        speedTestJob = viewModelScope.launch {
            suspendCancellableCoroutine<Unit> {

                val requestBuilder = Request.Builder()
                    .url("https://doh.local/test.mp4?r${Math.random()}&ckSize=100")
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
                val cb = object : CronetOutputStreamCallBack(outFile) {
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

    var downloadFileJob: Job? = null

    @SuppressLint("Recycle")
    fun downloadFile(uri: Uri) {
        downloadFileJob?.cancel()
        downloadFileJob = viewModelScope.launch {
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



                appCtx.contentResolver.openFileDescriptor(uri, "w")?.let { pfd ->




                    Log.e("SS",android.system.Os.fstat(pfd.fileDescriptor).toString())
                    val cb = object : CronetParcelFileDescriptorCallback(pfd) {
                        override fun onHeaders(urlResponseInfo: UrlResponseInfo?) {
                            result.tryEmit(urlResponseInfo?.allHeadersAsList.toString())
                        }


                        override fun onSuccess() {
                            result.tryEmit("下载完成")
                            pfd.close()
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

    var uploadFileJob: Job? = null

    fun uploadFile(uri: Uri, fileName: String) {
        uploadFileJob?.cancel()
        uploadFileJob = viewModelScope.launch(Dispatchers.IO) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val pfd: ParcelFileDescriptor? = appCtx.contentResolver.openFileDescriptor(uri, "r")
                if (pfd == null) {
                    continuation.cancel()
                    return@suspendCancellableCoroutine
                }

                val outFile = File(appCtx.externalCacheDir, "temp.bin")
                val cb = object : CronetOutputStreamCallBack(outFile) {
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
                    .addFormDataPart(
                        "myFile", fileName,
                        FileDescriptorRequestBody(pfd)
                    )
                    .build()
                val request: Request = Request.Builder()
                    .url("http://192.168.1.4:8080/upload")
                    .post(requestBody)
                    .build()

                val urlRequest = CronetHelper.buildRequest(Http.cronetEngine, request, cb)
                result.tryEmit("上传开始")
                urlRequest.start()

                continuation.invokeOnCancellation {
                    pfd.close()
                }


            }

        }
    }

}