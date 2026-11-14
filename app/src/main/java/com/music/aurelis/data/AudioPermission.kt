// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.app.Activity
import android.content.ContextWrapper

/**
 * Utilidad para verificar el permiso de lectura de audio local.
 *
 * En Android 13+ (API 33): READ_MEDIA_AUDIO
 */
object AudioPermission {
    fun needsSettings(context: Context, requestedBefore: Boolean): Boolean {
        if (!requestedBefore || isGranted(context)) return false
        var current = context
        while (current is ContextWrapper && current !is Activity) current = current.baseContext
        val activity = current as? Activity ?: return false
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, requiredPermission())
    }

    /**
     * Devuelve el permiso necesario (READ_MEDIA_AUDIO para API 33+).
     */
    fun requiredPermission(): String = Manifest.permission.READ_MEDIA_AUDIO

    /**
     * Comprueba si el permiso ya está concedido.
     */
    fun isGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            requiredPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }
}
