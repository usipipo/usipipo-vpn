package com.usipipo.vpn.widget

import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.usipipo.vpn.R
import com.usipipo.vpn.UsipipoApp
import java.util.function.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mosaico de ajustes rápidos: conecta o desconecta el perfil activo sin abrir la UI.
 *
 * El toque llega a [onClick] (la API real del mosaico: `Tile` no admite `PendingIntent` hasta
 * la API 34 y solo para Activities). La UI que hay que abrir se lanza con
 * [TileService.startActivityAndCollapse] para que el panel se recoja y la app pueda arrancar
 * aunque esté en segundo plano.
 */
class UsipipoTileService : TileService() {

    companion object {
        private const val REQUEST_CODE: Int = 0

        /** Pide al sistema que vuelva a escuchar el mosaico para poder repintarlo. */
        fun requestRefresh(context: Context) {
            TileService.requestListeningState(
                context,
                ComponentName(context, UsipipoTileService::class.java),
            )
        }

        /** Pide al sistema que añada el mosaico al panel (API 33+). En versiones anteriores no hay API. */
        fun requestAdd(context: Context, onResult: (Boolean) -> Unit) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onResult(false)
                return
            }
            val statusBar = context.getSystemService(StatusBarManager::class.java) ?: run {
                onResult(false)
                return
            }
            statusBar.requestAddTileService(
                ComponentName(context, UsipipoTileService::class.java),
                context.getString(R.string.app_name),
                Icon.createWithResource(context, R.drawable.ic_shield_check),
                context.mainExecutor,
                Consumer { added -> onResult(added == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) },
            )
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        renderTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        renderTile()
    }

    /** Mismo flujo que el widget: conectar, desconectar o pedir el permiso de VPN. */
    override fun onClick() {
        super.onClick()
        val container = (applicationContext as UsipipoApp).container
        container.applicationScope.launch {
            QuickActionRouter(
                context = applicationContext,
                controller = container.quickConnectController,
                launchActivity = { _, intent -> collapseShadeAndLaunch(intent) },
            ).toggle()
            withContext(Dispatchers.Main) { renderTile() }
        }
    }

    private fun renderTile() {
        val tile: Tile? = qsTile
        if (tile == null) return

        val container = (applicationContext as UsipipoApp).container
        val presentation = WidgetStateMapper.map(container.quickConnectController.state.value)

        tile.state = if (presentation.status == WidgetStatus.Connected) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.app_name)
        tile.icon = Icon.createWithResource(this, iconFor(presentation.status))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.stateDescription = getString(presentation.statusText)
            tile.subtitle = presentation.profileName.ifEmpty { getString(presentation.statusText) }
        }
        tile.updateTile()
    }

    /**
     * El mosaico no tiene API de color ni admite iconos teñidos: el escudo de la app se
     * dibuja con el acento del estado en un drawable por estado.
     */
    private fun iconFor(status: WidgetStatus): Int = when (status) {
        WidgetStatus.Connected -> R.drawable.ic_tile_shield_connected
        WidgetStatus.Failed, WidgetStatus.NoResponse -> R.drawable.ic_tile_shield_failed
        WidgetStatus.Disconnected, WidgetStatus.Connecting, WidgetStatus.NoProfile ->
            R.drawable.ic_tile_shield_disconnected
    }

    /** Recoge el panel y abre la Activity con el mecanismo propio del mosaico. */
    private fun collapseShadeAndLaunch(intent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        ContextCompat.getMainExecutor(this).execute {
            startActivityAndCollapse(pendingIntent)
        }
    }
}
