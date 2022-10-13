package me.ag2s.cronet.test.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun NetImage(
    model: Any?,
    modifier: Modifier = Modifier,
    imageOptions: ImageOptions = ImageOptions(contentScale = ContentScale.FillWidth),
) {

    GlideImage(
        imageModel = model,
        requestBuilder = { Glide.with(LocalContext.current.applicationContext).asDrawable() },
        imageOptions = imageOptions,
        modifier = modifier,
        loading = {
            Box(modifier = Modifier.matchParentSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        failure = {
            Box(modifier = Modifier.matchParentSize()) {
                Text(
                    text = "image request failed",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    )
}