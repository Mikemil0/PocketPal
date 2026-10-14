package com.pocketpal.app.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object AppPermissions {
    /** POST_NOTIFICATIONS solo existe desde Android 13; antes no hay nada que pedir (null). */
    val notifications: String? =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
    const val camera = Manifest.permission.CAMERA
    const val microphone = Manifest.permission.RECORD_AUDIO

    private const val PREFS = "pocketpal_prefs"

    /** ¿Ya le mostramos esta pantalla de permiso al usuario EN ESTE teléfono? (los permisos son por dispositivo) */
    fun wasPrompted(context: Context, key: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("prompted_$key", false)

    fun markPrompted(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("prompted_$key", true).apply()
    }

    fun isGranted(context: Context, permission: String?): Boolean =
        permission == null ||
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
