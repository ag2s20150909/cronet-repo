package me.ag2s.ktor

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory

class Cronet(private val preconfigured: org.chromium.net.CronetEngine) : HttpClientEngineFactory<CronetConfig>{
    override fun create(block: CronetConfig.() -> Unit): HttpClientEngine {
        return CronetClientEngine(CronetConfig(preconfigured).apply(block))
    }
}