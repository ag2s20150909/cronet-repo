package me.ag2s.cronet.test

import androidx.compose.runtime.mutableStateOf

object RequestState {
    val httpMethodS = listOf(
        "GET",
        "POST",
        "HEAD",
        "PUT",
        "DELETE",
        "PATCH"
    )
    val httpMethod = mutableStateOf(httpMethodS.first())

    val protocols = listOf("https://", "http://")

    val protocol = mutableStateOf(protocols.first())

    val url = mutableStateOf("http3.is")


}