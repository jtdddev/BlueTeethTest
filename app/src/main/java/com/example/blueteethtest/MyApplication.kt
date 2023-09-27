package com.example.blueteethtest

import android.app.Application
import java.util.UUID

class MyApplication : Application() {

    init {
        instance = this
    }

    companion object {
        lateinit var instance: Application
    }

    override fun onCreate() {
        super.onCreate()
    }


}