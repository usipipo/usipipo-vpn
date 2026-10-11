package com.usipipo.vpn

import android.content.Context
import com.usipipo.vpn.data.ConfigImporter
import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.data.EncryptedFileConfigStore
import com.usipipo.vpn.data.KeystoreCrypto
import com.usipipo.vpn.diagnostics.DiagnosticsSettings
import com.usipipo.vpn.tunnel.TunnelManager
import com.usipipo.vpn.tunnel.WireGuardTunnelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
}
