package com.usipipo.vpn.widget

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import com.usipipo.vpn.AppContainer
import com.usipipo.vpn.R
import com.usipipo.vpn.UsipipoApp
import kotlinx.coroutines.launch

/** Mosaico de ajustes rápidos: conecta o desconecta el perfil activo sin abrir la UI. */
class UsipipoTileService : TileService() {

    companion object {
        private const val REQUEST_CODE: Int = 0

        fun requestRefresh(context: Context) {
            TileService.requestListeningState(
                context,
                ComponentName(context, UsipipoTileService::class.java),
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        val intent = Intent(this, UsipipoTileService::class.java).apply {
            action = WidgetIntents.ACTION_TOGGLE
        }
        qsTile.setTilePendingIntent(
            PendingIntent.getService(
                this,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == WidgetIntents.ACTION_TOGGLE) {
            val container = (applicationContext as UsipipoApp).container
            container.applicationScope.launch {
                QuickActionRouter(applicationContext, container.quickConnectController).toggle()
                renderTile()
            }
        }
        return START_NOT_STICKY
    }

    override fun onTileAdded() {
        super.onTileAdded()
        renderTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        renderTile()
    }

    private fun renderTile() {
        val container = (applicationContext as UsipipoApp).container
        val presentation = WidgetStateMapper.map(container.quickConnectController.state.value)
        val tile = qsTile

        tile.state =
            if (presentation.status == WidgetStatus.Connected) Tile.STATE_ACTIVE
            else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_shield_check)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.stateDescription = getString(presentation.statusText)
            tile.subtitle =
                if (presentation.profileName.isNotEmpty()) presentation.profileName
                else getString(presentation.statusText)
        }
        tile.color = colorFor(presentation.status)
        tile.updateTile()
    }

    private fun colorFor(status: WidgetStatus): Int = when (status) {
        WidgetStatus.Connected -> R.color.widget_accent_connected
        WidgetStatus.Failed, WidgetStatus.NoResponse -> R.color.widget_accent_failed
        WidgetStatus.Disconnected, WidgetStatus.Connecting, WidgetStatus.NoProfile ->
            R.color.widget_accent_disconnected
    }
}
