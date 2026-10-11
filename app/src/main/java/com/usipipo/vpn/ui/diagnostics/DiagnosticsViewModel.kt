package com.usipipo.vpn.ui.diagnostics

import android.content.Context
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.R
import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.diagnostics.DeviceInfo
import com.usipipo.vpn.diagnostics.DiagnosticsSettings
import com.usipipo.vpn.diagnostics.ReportSender
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.tunnel.TunnelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DiagnosticsUiState(
    val infoRows: List<Pair<String, String>> = emptyList(),
    val verbose: Boolean = false,
    val logLines: List<String> = emptyList(),
    val logSizeBytes: Long = 0L,
    val crashPending: Boolean = false,
    val busy: Boolean = false,
)

sealed interface DiagnosticsEvent {
    data class SendReport(val file: File, val infoRows: List<Pair<String, String>>) : DiagnosticsEvent
    data object ReportFailed : DiagnosticsEvent
}

class DiagnosticsViewModel(
    private val app: Context,
    private val settings: DiagnosticsSettings,
    private val tunnelManager: TunnelManager,
    private val store: ConfigStore,
) : ViewModel() {

    private val _ui = MutableStateFlow(DiagnosticsUiState())
    val ui: StateFlow<DiagnosticsUiState> = _ui.asStateFlow()

    private val _events = Channel<DiagnosticsEvent>(Channel.BUFFERED)
    val events: Flow<DiagnosticsEvent> = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val lines = withContext(Dispatchers.IO) { AppLog.readLastLines(MAX_VISIBLE_LINES) }
            val size = withContext(Dispatchers.IO) { AppLog.sizeBytes() }
            _ui.update {
                it.copy(
                    infoRows = infoRows(),
                    verbose = settings.verbose,
                    logLines = lines,
                    logSizeBytes = size,
                    crashPending = settings.crashPending,
                )
            }
        }
    }

    fun setVerbose(enabled: Boolean) {
        settings.verbose = enabled
        AppLog.setVerbose(enabled)
        AppLog.i("Diagnostico", "Modo detallado ${if (enabled) "activado" else "desactivado"}")
        refresh()
    }

    fun dismissCrashNotice() {
        settings.clearCrashPending()
        _ui.update { it.copy(crashPending = false) }
    }

    fun clearLog() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppLog.clear()
                AppLog.i("Diagnostico", "Registro borrado por el usuario")
            }
            refresh()
        }
    }

    fun sendReport() {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            val rows = infoRows()
            val result = withContext(Dispatchers.IO) {
                runCatching { ReportSender.buildFile(app, rows, settings.verbose) }
            }
            _ui.update { it.copy(busy = false) }
            result.fold(
                onSuccess = { file ->
                    AppLog.i("Diagnostico", "Informe preparado (${file.length()} bytes)")
                    _events.send(DiagnosticsEvent.SendReport(file, rows))
                },
                onFailure = { e ->
                    AppLog.e("Diagnostico", "No se pudo preparar el informe", e)
                    _events.send(DiagnosticsEvent.ReportFailed)
                },
            )
        }
    }

    /** Solo metadatos: nada de nombres de configuraciones, IPs, claves ni identificadores. */
    private fun infoRows(): List<Pair<String, String>> {
        val device = DeviceInfo.collect(app)
        val permission = if (VpnService.prepare(app) == null) R.string.perm_granted else R.string.perm_pending
        return listOf(
            app.getString(R.string.info_app) to device.appText,
            app.getString(R.string.info_android) to device.androidText,
            app.getString(R.string.info_device) to device.deviceText,
            app.getString(R.string.info_abi) to device.abi,
            app.getString(R.string.info_tunnel) to app.getString(tunnelLabel(tunnelManager.state.value)),
            app.getString(R.string.info_vpn_permission) to app.getString(permission),
            app.getString(R.string.info_configs) to store.configs.value.size.toString(),
        )
    }

    private fun tunnelLabel(state: TunnelState): Int = when (state) {
        TunnelState.Disconnected -> R.string.status_disconnected
        is TunnelState.Connecting -> R.string.status_connecting
        is TunnelState.Connected -> R.string.status_connected
        is TunnelState.NoResponse -> R.string.status_no_response
        is TunnelState.Failed -> R.string.status_failed
    }

    private companion object {
        const val MAX_VISIBLE_LINES = 300
    }
}
