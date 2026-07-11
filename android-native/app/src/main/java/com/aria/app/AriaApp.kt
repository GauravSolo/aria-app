package com.aria.app

import android.app.Application
import com.aria.app.data.Supa

class AriaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Supa.init(this)
    }
}
