package com.aria.app

import android.app.Application
import com.aria.app.data.Supa
import com.aria.app.notif.ReminderReceiver

class AriaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Supa.init(this)
        ReminderReceiver.ensureChannel(this)
    }
}
