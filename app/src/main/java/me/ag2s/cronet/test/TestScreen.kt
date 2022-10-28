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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun BenchmarkScreen() {
    val viewModel: BenchmarkViewModel = viewModel()
    val result by viewModel.result.collectAsState()

    Column() {

        Button(onClick = { viewModel.test1() }) { Text(text = "测试") }
        Button(onClick = { viewModel.test2() }) { Text(text = "测试2") }

        Text(text = result)

    }


}

class BenchmarkViewModel : ViewModel() {


    val result = MutableStateFlow<String>("")

    fun test1() {

        viewModelScope.launch(Dispatchers.IO) {
            OkhttpUtils.setOkhttpClent(Http.okHttpClient)
            val startTime = System.currentTimeMillis()
            (1..10).map {
                OkhttpUtils.httpGet(OkhttpUtils.getRandomImgLink())
            }
            result.emit("test1:${System.currentTimeMillis() - startTime}\n${result.value}")
        }
    }

    fun test2() {
        viewModelScope.launch(Dispatchers.IO) {
            OkhttpUtils.setOkhttpClent(Http.okHttpClient1)
            val startTime = System.currentTimeMillis()
            (1..10).map {
                OkhttpUtils.httpGet(OkhttpUtils.getRandomImgLink())
            }
            result.emit("test2:${System.currentTimeMillis() - startTime}\n${result.value}")
        }
    }

}