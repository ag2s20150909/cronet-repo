package me.ag2s.cronet.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.model.GlideUrl


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun NetImage(
    model: Any?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.FillWidth,
) {

    GlideImage(
        model = GlideUrl(model.toString()),
        contentDescription = "",
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,

        )


}