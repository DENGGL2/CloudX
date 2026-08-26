package com.denggl2.masonremote

import android.app.Application
import com.denggl2.masonremote.diagnostics.DiagnosticLog

class CloudXApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DiagnosticLog.initialize(this)
        DiagnosticLog.installUncaughtExceptionHandler()
    }
}
