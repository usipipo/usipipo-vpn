package com.usipipo.vpn

import android.content.Context
import com.usipipo.vpn.data.ConfigImporter
import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.data.EncryptedFileConfigStore
import com.usipipo.vpn.data.KeystoreCrypto
import com.usipipo.vpn.diagnostics.DiagnosticsSettings
import com.usipipo.vpn.tunnel.TunnelManager
import com.usipipo.vpn.tunnel.WireGuardTunnelManager
import com.usipipo.vpn.update.GitHubReleaseSource
import com.usipipo.vpn.update.PrefsUpdateStateStore
import com.usipipo.vpn.update.UpdateChecker
import com.usipipo.vpn.update.UpdateStateStore
import com.usipipo.vpn.update.UpdateStatus
import com.usipipo.vpn.update.Version
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Contenedor de dependencias manual: suficiente para una app de tres pantallas. */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val configStore: ConfigStore by lazy {
        EncryptedFileConfigStore(
            dir = File(appContext.filesDir, "configs"),
            crypto = KeystoreCrypto(),
        )
    }

    val configImporter: ConfigImporter by lazy { ConfigImporter() }

    val diagnosticsSettings: DiagnosticsSettings by lazy { DiagnosticsSettings(appContext) }

    // Lazy: la librería nativa de WireGuard solo se carga cuando se necesita.
    val tunnelManager: TunnelManager by lazy { WireGuardTunnelManager(appContext, scope) }

    val updateStateStore: UpdateStateStore by lazy { PrefsUpdateStateStore.create(appContext) }

    val updateChecker: UpdateChecker by lazy {
        UpdateChecker(source = GitHubReleaseSource(), store = updateStateStore)
    }

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.UpToDate)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    /** La versión que ve el sistema, no una constante de compilación. */
    val currentVersion: String by lazy { readVersionName() }

    /** Comprobación automática del arranque: la frena el throttle de 24 h del store. */
    fun checkForUpdatesOnStartup() = launchUpdateCheck(force = false)

    fun launchUpdateCheck(force: Boolean = false) {
        scope.launch { checkForUpdates(force) }
    }

    /** Devuelve el resultado para que la pantalla que la pidió pueda mostrarlo. */
    suspend fun checkForUpdates(force: Boolean = false): UpdateStatus {
        val status = runCatching { updateChecker.check(currentVersion, force) }
            .getOrElse { UpdateStatus.Failed }
        _updateStatus.value = status
        return status
    }

    /** «Más tarde»: no se vuelve a avisar de esta versión concreta. */
    fun postponeUpdate(version: Version) {
        scope.launch { updateStateStore.markSkipped(version.toString()) }
        _updateStatus.value = UpdateStatus.Postponed
    }

    /**
     * AGP 9 no genera `BuildConfig` mientras no se active `buildConfig = true`, así que el
     * respaldo es quedarse sin versión: la comprobación se descarta sin crash.
     */
    @Suppress("DEPRECATION")
    private fun readVersionName(): String =
        runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: ""
}
