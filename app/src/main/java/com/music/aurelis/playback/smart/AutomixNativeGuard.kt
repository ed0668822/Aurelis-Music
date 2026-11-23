package com.music.aurelis.playback.smart

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.music.aurelis.BuildConfig

/** Serializes native inference and remembers a model interrupted by a native process exit. */
internal class AutomixNativeGuard(context: Context) {
    enum class Model { BEAT, VOCALS }

    private val app = context.applicationContext
    private val state = app.getSharedPreferences("automix-native-inference", Context.MODE_PRIVATE)
    private var checkedExit = false

    fun <T> infer(model: Model, cancelled: () -> Boolean, diagnostics: () -> String, run: () -> T?): T? =
        synchronized(nativeLock) {
            if (cancelled()) return@synchronized null
            checkPreviousExit()
            if (state.getInt(model.name, -1) == BuildConfig.VERSION_CODE) return@synchronized null
            state.edit()
                .putString("active", model.name)
                .putInt("pid", Process.myPid())
                .putInt("version", BuildConfig.VERSION_CODE)
                .putLong("started", System.currentTimeMillis())
                .commit()
            Log.d(TAG, "ONNX ${model.name} begin ${diagnostics()}")
            try {
                run()
            } finally {
                state.edit().remove("active").commit()
                Log.d(TAG, "ONNX ${model.name} end")
            }
        }

    fun closeSessions(close: () -> Unit) {
        synchronized(nativeLock) { close() }
    }

    private fun checkPreviousExit() {
        if (checkedExit) return
        checkedExit = true
        val active = state.getString("active", null) ?: return
        val pid = state.getInt("pid", -1)
        val started = state.getLong("started", Long.MAX_VALUE)
        if (pid == Process.myPid()) return
        val editor = state.edit().remove("active")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            pid > 0 && state.getInt("version", -1) == BuildConfig.VERSION_CODE
        ) {
            val exits = runCatching {
                app.getSystemService(ActivityManager::class.java)?.getHistoricalProcessExitReasons(null, pid, 5).orEmpty()
            }.getOrDefault(emptyList())
            if (exits.any { it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE && it.timestamp >= started }) {
                editor.putInt(active, BuildConfig.VERSION_CODE)
                Log.w(TAG, "Previous native crash interrupted ${active}; using DSP fallback for this model")
            }
        }
        editor.commit()
    }

    private companion object {
        const val TAG = "AurelisAutoMixNative"
        val nativeLock = Any()
    }
}
