package me.ag2s.cronet.test

import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TestViewModel : ViewModel() {

    val httpMethod = MutableStateFlow<String>(RequestState.httpMethodS.first())
    val protocol = MutableStateFlow<String>(RequestState.protocols.first())
    val url = MutableStateFlow<String>("crypto.cloudflare.com/cdn-cgi/trace")

    var txt = MutableStateFlow<String>("")

    fun changeMethod(int: Int) {
        viewModelScope.launch {
            httpMethod.emit(RequestState.httpMethodS[int])
        }

    }

    fun changeProtocol(host: String) {
        viewModelScope.launch {
            protocol.emit(host)
        }
    }


    fun changeHost(host: String) {
        viewModelScope.launch {
            url.emit(host)
        }
    }


    fun getHtml() {
        viewModelScope.launch(Dispatchers.IO) {
            if (URLUtil.isNetworkUrl(protocol.value + url.value)) {
                kotlin.runCatching {
                    txt.emit(OkhttpUtils.httpGet(protocol.value + url.value))
                }.onFailure {
                    txt.emit(it.stackTraceToString())
                }

            }


        }


    }
}