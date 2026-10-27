package com.usipipo.vpn

import android.content.Context
import com.usipipo.vpn.data.ConfigImporter
import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.data.EncryptedFileConfigStore
import com.usipipo.vpn.data.KeystoreCrypto
import com.usipipo.vpn.data.geo.CountryRepository
import com.usipipo.vpn.data.geo.IpApiClient
import com.usipipo.vpn.data.geo.PingRepository
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.diagnostics.DeviceInfo
import com.usipipo.vpn.diagnostics.DiagnosticsSettings
import com.usipipo.vpn.quickconnect.AndroidVpnPermissionChecker
import com.usipipo.vpn.quickconnect.QuickConnectController
import com.usipipo.vpn.tunnel.TunnelManager
import com.usipipo.vpn.tunnel.WireGuardTunnelManager
import com.usipipo.vpn.update.GitHubReleaseSource
import com.usipipo.vpn.update.PrefsUpdateStateStore
import com.usipipo.vpn.update.UpdateChecker
import com.usipipo.vpn.update.UpdateStateStore
import com.usipipo.vpn.update.UpdateStatus
import com.usipipo.vpn.update.Version
import com.usipipo.vpn.widget.SharedPreferencesWidgetStateStore
import com.usipipo.vpn.widget.UsipipoTileService
import com.usipipo.vpn.widget.UsipipoWidgetRenderer
import com.usipipo.vpn.widget.WidgetUpdater
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Contenedor de dependencias manual: suficiente para una app de tres pantallas. */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext
    internal val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val configStore: ConfigStore by lazy {
        EncryptedFileConfigStore(
            dir = File(appContext.filesDir, "configs"),
            crypto = KeystoreCrypto(),
        )
    }

    val configImporter: ConfigImporter by lazy { ConfigImporter() }

    val diagnosticsSettings: DiagnosticsSettings by lazy { DiagnosticsSettings(appContext) }

    // Lazy: la librería nativa de WireGuard solo se carga cuando se necesita.
    val tunnelManager: TunnelManager by lazy { WireGuardTunnelManager(appContext, applicationScope) }

    val quickConnectController: QuickConnectController by lazy {
        QuickConnectController(
            store = configStore,
            tunnelManager = tunnelManager,
            permissionChecker = AndroidVpnPermissionChecker(appContext),
            scope = applicationScope,
        )
    }

    /**
     * Mantiene el widget y el mosaico al día: redibuja solo cuando cambia el estado del túnel.
     */
    val widgetUpdater: WidgetUpdater by lazy {
        WidgetUpdater(
            scope = applicationScope,
            states = quickConnectController.state,
            renderWidget = { UsipipoWidgetRenderer(appContext).render(it) },
            refreshTile = { UsipipoTileService.requestRefresh(appContext) },
            store = SharedPreferencesWidgetStateStore(appContext),
        )
    }

    /** Repositorio de geolocalización de país con caché 24 h (ipapi.co). */
    val countryRepository: CountryRepository by lazy {
        CountryRepository(configStore, IpApiClient())
    }

    /** Repositorio de ping nativo con fallback a isReachable. */
    val pingRepository: PingRepository by lazy { PingRepository() }

    val updateStateStore: UpdateStateStore by lazy { PrefsUpdateStateStore.create(appContext) }

    val updateChecker: UpdateChecker by lazy {
        UpdateChecker(source = GitHubReleaseSource(), store = updateStateStore)
    }

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.UpToDate)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    /** La versión que ve el sistema; si falta o está mal formada, la de la compilación. */
    val currentVersion: String by lazy { readVersionName() }

    /** Comprobación automática del arranque: la frena el throttle de 24 h del store. */
    fun checkForUpdatesOnStartup() {
        applicationScope.launch { checkForUpdates(force = false) }
    }

    /** Arranca el actualizador del widget y del mosaico (idempotente). */
    fun startWidgetUpdater() {
        widgetUpdater.start()
    }

    /** Devuelve el resultado para que la pantalla que la pidió pueda mostrarlo. */
    suspend fun checkForUpdates(force: Boolean = false): UpdateStatus {
        val status = try {
            updateChecker.check(currentVersion, force)
        } catch (e: CancellationException) {
            // Cancelar el job no es un fallo: se propaga sin tocar el estado.
            throw e
        } catch (e: Exception) {
            AppLog.w(TAG_ACTUALIZAR, "La comprobación de actualizaciones falló", e)
            UpdateStatus.Failed
        }
        _updateStatus.value = status
        return status
    }

    /** «Más tarde»: no se vuelve a avisar de esta versión concreta. */
    fun postponeUpdate(version: Version) {
        applicationScope.launch { updateStateStore.skippedVersion = version.toString() }
        _updateStatus.value = UpdateStatus.Postponed
    }

    /**
     * La versión que ve el sistema, la misma lectura que publica [DeviceInfo] en el registro
     * de arranque; si falta o está mal formada, se usa la versión con la que se compiló la
     * app (R9 y §6). Si tampoco es utilizable, lo descarta el checker sin crash.
     */
    private fun readVersionName(): String {
        val delSistema = DeviceInfo.collect(appContext).versionName
        return delSistema.takeIf { Version.parse(it) != null } ?: BuildConfig.VERSION_NAME
    }

    private companion object {
        const val TAG_ACTUALIZAR = "Actualizar"
    }
}
