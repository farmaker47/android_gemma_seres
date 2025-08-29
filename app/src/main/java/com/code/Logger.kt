package com.code

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private const val TAG = "MyApp"
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        logFile = File(dir, "myapp_log.txt")
        d("Logger initialized at: ${logFile?.absolutePath}")
    }

    fun d(message: String) {
        Log.d(TAG, message)
        write("DEBUG", message)
    }

    fun e(message: String, tr: Throwable? = null) {
        Log.e(TAG, message, tr)
        write("ERROR", "$message${tr?.let { " (${it.message})" } ?: ""}")
    }

    private fun write(level: String, msg: String) {
        try {
            val f = logFile ?: return
            val now = dateFormat.format(Date())
            FileWriter(f, true).use { it.write("[$now][$level] $msg\n") }
        } catch (t: Throwable) {
            Log.e(TAG, "Could not write log", t)
        }
    }
}

