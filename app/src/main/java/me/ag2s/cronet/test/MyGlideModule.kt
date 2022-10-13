package me.ag2s.cronet.test

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import me.ag2s.cronet.glide.CronetUrlLoader
import java.io.InputStream
import java.nio.ByteBuffer

@GlideModule
class MyGlideModule : AppGlideModule() {
    //private val libraryGlideModule by lazy { CronetLibraryGlideModule() }
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)

        builder.setMemoryCache(null).setLogRequestOrigins(true).setLogLevel(Log.VERBOSE)
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        Log.e("Glide", "Callde")
        registry.replace(
            GlideUrl::class.java, InputStream::class.java, CronetUrlLoader.StreamFactory()
        )
        registry.prepend(
            GlideUrl::class.java, ByteBuffer::class.java, CronetUrlLoader.ByteBufferFactory()
        )
    }
}