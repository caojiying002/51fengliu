package com.jiyingcao.a51fengliu

import android.app.Application

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        registerActivityLifecycleCallbacks(EdgeToEdgeWindowInsetsCallbacks)
    }

    companion object {
        @JvmStatic lateinit var INSTANCE: App
    }
}