package me.ag2s.cronet.test

import android.app.Application
import android.content.Context

lateinit var appCtx: APP

class APP : Application() {
    override fun onCreate() {
        super.onCreate()
        appCtx = this
        //CronetLoader.getInstance().preDownload()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        appCtx = this
    }


}