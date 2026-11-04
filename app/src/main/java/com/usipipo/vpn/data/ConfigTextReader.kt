package com.usipipo.vpn.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.usipipo.vpn.settings.AppSettings

/**
 * Lee un `.conf` guardado y lo deja listo para el túnel.
 *
 * Es el único punto donde se inyectan las apps excluidas: los sitios que leen el
 * `.conf` antes de pasarlo al túnel usan este lector en lugar de `ConfigStore`.
 */
class ConfigTextReader(
    private val store: ConfigStore,
    private val appSettings: AppSettings,
    /**
     * Paquetes instalados en el dispositivo. `null` significa que no se pueden
     * consultar: en ese caso no se poda nada de la lista guardada.
     */
    private val installedPackages: (() -> Set<String>?)? = null,
) {

    /**
     * Texto del `.conf` con las apps excluidas inyectadas en su [Interface].
     *
     * Devuelve `null` si el `.conf` no existe. Los paquetes de la lista que ya no
     * estén instalados se quitan de la lista guardada, para no dejarbasura.
     */
    suspend fun readForTunnel(configId: String): String? {
        val text = store.readText(configId) ?: return null
        var excluidas = ExcludedAppsConfig.decode(appSettings.excludedApps)
        installedPackages?.invoke()?.let { instalados ->
            val vivas = ExcludedAppsConfig.keepInstalled(excluidas, instalados)
            if (vivas != excluidas) {
                appSettings.excludedApps = ExcludedAppsConfig.encode(vivas)
                excluidas = vivas
            }
        }
        return ExcludedAppsConfig.inject(text, excluidas)
    }
}

/**
 * Paquetes instalados en el dispositivo.
 *
 * Necesita `QUERY_ALL_PACKAGES` para verlos todos; sin ese permiso el sistema
 * filtra la lista, así que algunas apps podrían parecer desinstaladas.
 */
fun installedPackages(context: Context): Set<String> {
    val manager = context.packageManager
    val aplicaciones = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        manager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        manager.getInstalledApplications(0)
    }
    return aplicaciones.mapTo(mutableSetOf()) { it.packageName }
}
