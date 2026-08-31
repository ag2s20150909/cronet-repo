package me.ag2s.cronet.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage


@Composable
fun NetImage(
    model: Any?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.FillWidth,
) {

    AsyncImage(model, contentDescription = "", modifier = modifier,alignment=alignment, contentScale = contentScale)

//    GlideImage(
//        model = GlideUrl(model.toString()),
//        contentDescription = "",
//        modifier = modifier,
//        alignment = alignment,
//        contentScale = contentScale,
//
//        )


}