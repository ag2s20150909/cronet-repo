package me.ag2s.cronet.test

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class MyGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)

        builder.setMemoryCache(LruResourceCache(1024 * 1024 * 50))
            .setLogRequestOrigins(true)
            .setLogLevel(Log.VERBOSE)
            .setImageDecoderEnabledForBitmaps(true)
            .setIsActiveResourceRetentionAllowed(true)
        //.setImageDecoderEnabledForBitmaps(true)

    }

}