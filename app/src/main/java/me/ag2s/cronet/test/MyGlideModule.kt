package me.ag2s.cronet.test

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class MyGlideModule : AppGlideModule() {
    //private val libraryGlideModule by lazy { CronetLibraryGlideModule() }
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)

        builder.setMemoryCache(null).setLogRequestOrigins(true).setLogLevel(Log.VERBOSE)
    }

//    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
//        Log.e("Glide", "Callde")
//        registry.replace(
//            GlideUrl::class.java, InputStream::class.java, CronetUrlLoader.StreamFactory()
//        )
//        registry.prepend(
//            GlideUrl::class.java, ByteBuffer::class.java, CronetUrlLoader.ByteBufferFactory()
//        )
//    }
}