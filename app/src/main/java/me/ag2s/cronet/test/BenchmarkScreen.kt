package me.ag2s.cronet.test

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow

@Composable
fun BenchmarkScreen() {
    val viewModel: BenchmarkViewModel = viewModel()

    val type by viewModel.type.collectAsState()

    Column() {

        Button(onClick = { viewModel.test1() }) { Text(text = "图片") }
        Button(onClick = {
            viewModel.test2()

        }) { Text(text = "并发") }

        when (type) {
            0 -> {
                Text(text = "默认值")
            }
            1 -> {
                val result by viewModel.result.collectAsState()
                Text(text = result)
            }
            2 -> {
                FF(200)
            }
        }


    }


}

@Composable
private fun FF(size: Int) {
    LazyColumn() {
        items(size) {
            NetImage(
                model = OkhttpUtils.getRandomImgLink(),
                modifier = Modifier
                    .size(360.dp)
            )
        }
    }

}

class BenchmarkViewModel : ViewModel() {

    val type = MutableStateFlow(0)


    val result = MutableStateFlow<String>("")

    fun test1() {

        viewModelScope.launch(Dispatchers.IO) {
            type.emit(2)
        }
    }


    fun test2() {
        viewModelScope.launch(Dispatchers.IO) {
            type.emit(1)
            OkhttpUtils.setOkhttpClent(Http.okHttpClient1)
            val startTime = System.currentTimeMillis()
            (1..10).pmap {
                try {
                    OkhttpUtils.httpGet("https://doh.local/test")
                }catch (e:Exception){
                    e.printStackTrace()
                   result.emit(e.stackTraceToString())
                }
            }
            result.emit("test2:${System.currentTimeMillis() - startTime}\n${result.value}")
            //{
//                try {
//                    OkhttpUtils.httpGet(OkhttpUtils.getRandomImgLink())
//                }catch (e:Exception){
//                    result.emit(e.stackTraceToString())
//                }
//
//            }
            //result.emit("test2:${System.currentTimeMillis() - startTime}\n${result.value}")
        }
    }

}

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): Unit = coroutineScope {
    map { async { f(it) } }.awaitAll()
}