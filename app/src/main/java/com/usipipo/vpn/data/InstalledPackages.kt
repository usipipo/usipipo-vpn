package com.usipipo.vpn.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Paquetes instalados en el dispositivo, o `null` si la app no puede enumerarlos de
 * forma fiable.
 *
 * Sin `QUERY_ALL_PACKAGES` el sistema filtra la lista, así que apps que sí existen
 * podrían parecer desinstaladas y borrarse del ajuste del usuario: en ese caso se
 * devuelve `null` y no se poda nada.
 */
fun installedPackages(context: Context): Set<String>? {
    val sinPermiso = context.packageManager.checkPermission(
        android.Manifest.permission.QUERY_ALL_PACKAGES,
        context.packageName,
    ) != PackageManager.PERMISSION_GRANTED
    if (sinPermiso) return null

    val manager = context.packageManager
    val aplicaciones = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        manager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        manager.getInstalledApplications(0)
    }
    return aplicaciones.mapTo(mutableSetOf()) { it.packageName }
}
