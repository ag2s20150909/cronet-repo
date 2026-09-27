package me.ag2s.cronet.test

import android.app.Application
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath

lateinit var appCtx: APP

class APP : Application(),SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        appCtx = this
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        appCtx = this
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.externalCacheDir!!.resolve("ktor").toOkioPath())
                    .maxSizePercent(0.02)
                    .build()
            }
            .components {
                if (SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }


}