package com.example.cameratoggle

import android.app.Application
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class PrivacyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                File(filesDir, "crash_log.txt").writeText(sw.toString())
            } catch (_: Exception) {
                // если даже запись лога не удалась — просто идём дальше
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
