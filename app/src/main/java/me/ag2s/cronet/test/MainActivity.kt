package me.ag2s.cronet.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import me.ag2s.cronet.test.ui.theme.CronetTheme
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.chromium.net.MyCronetEngine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    val viewModel by viewModels<TestViewModel>()


    private val executor: ExecutorService =Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CronetTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                   DemoScreen()
                }
            }
        }
        executor.submit {
            val response=Http.okHttpClient.newCall(Request("https://http3.is/".toHttpUrl())).execute().body.string()
            //Log.e("ss",response)
        }

    }
}