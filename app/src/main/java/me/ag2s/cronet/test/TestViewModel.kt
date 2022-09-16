package me.ag2s.cronet.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TestViewModel: ViewModel() {

    fun getHtml(){
        viewModelScope.launch {
            OkhttpUtils.httpGet("https://http3.is")
        }


    }
}