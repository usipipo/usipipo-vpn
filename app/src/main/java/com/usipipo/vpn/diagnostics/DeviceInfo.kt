package com.usipipo.vpn.diagnostics

import android.content.Context
import android.os.Build

/** Datos del dispositivo y de la app. Nada personal: ni cuentas, ni IPs, ni identificadores. */
data class DeviceInfo(
    val versionName: String,
    val versionCode: Long,
    val androidRelease: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
    val abi: String,
) {
    val appText: String get() = "$versionName ($versionCode)"
    val androidText: String get() = "$androidRelease (API $sdkInt)"
    val deviceText: String get() = "$manufacturer $model"

    /** Una línea para el registro de arranque. */
    val summary: String
        get() = "app $appText · Android $androidText · $deviceText · $abi"

    companion object {
        @Suppress("DEPRECATION")
        fun collect(context: Context): DeviceInfo {
            val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
            val code = when {
                info == null -> 0L
                Build.VERSION.SDK_INT >= 28 -> info.longVersionCode
                else -> info.versionCode.toLong()
            }
            return DeviceInfo(
                versionName = info?.versionName ?: "?",
                versionCode = code,
                androidRelease = Build.VERSION.RELEASE ?: "?",
                sdkInt = Build.VERSION.SDK_INT,
                manufacturer = Build.MANUFACTURER ?: "?",
                model = Build.MODEL ?: "?",
                abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "?",
            )
        }
    }
}
