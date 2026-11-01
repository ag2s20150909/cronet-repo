package me.ag2s.cronet.test

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.request.url
import io.ktor.client.statement.HttpStatement

object KtorUtils {

    suspend fun get(url: String): String= Http.client.get {
        url(url)
    }.body()


    suspend fun getSteam(url: String): HttpStatement= Http.client.prepareGet {
        url(url)
    }

}