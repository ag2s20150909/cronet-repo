package me.ag2s.ktor

import io.ktor.client.engine.HttpClientEngineConfig
import org.chromium.net.CronetEngine


class CronetConfig(val preconfigured: CronetEngine): HttpClientEngineConfig() {

    var followRedirects: Boolean = true
    var responseBufferSize: Int = 1024
}