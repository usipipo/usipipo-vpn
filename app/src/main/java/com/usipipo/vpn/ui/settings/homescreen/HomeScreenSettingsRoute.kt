package com.usipipo.vpn.ui.settings.homescreen

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usipipo.vpn.LauncherActivity
import com.usipipo.vpn.R
import com.usipipo.vpn.UsipipoApp
import com.usipipo.vpn.widget.UsipipoTileService
import com.usipipo.vpn.widget.UsipipoWidgetProvider

/**
 * Subpantalla de pantalla inicial y panel con su ViewModel.
 *
 * Patrón de [com.usipipo.vpn.ui.settings.storage.StorageRoute]: el ViewModel no
 * depende de Android, así que el texto de los mensajes se resuelve aquí.
 */
@Composable
fun HomeScreenSettingsRoute(
    viewModel: HomeScreenSettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val lifecycleOwner = LocalLifecycleOwner.current

    var mensaje by remember { mutableStateOf<String?>(null) }
    var widgetAdded by remember { mutableStateOf(widgetPresente(context)) }

    // Al montar y al volver a primer plano: si el usuario acaba de añadir el widget
    // desde el selector del lanzador, la tarjeta tiene que decirlo al regresar.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                widgetAdded = widgetPresente(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeScreenSettingsScreen(
        widgetAdded = widgetAdded,
        pinSupported = appWidgetManager.isRequestPinAppWidgetSupported,
        showCountry = state.showCountry,
        message = mensaje,
        onShowCountryChange = { valor ->
            viewModel.setShowCountry(valor)
            repintar(context)
        },
        onPinWidget = {
            runCatching {
                appWidgetManager.requestPinAppWidget(
                    ComponentName(context, UsipipoWidgetProvider::class.java),
                    null,
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, LauncherActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
        },
        onAddTile = {
            UsipipoTileService.requestAdd(context) { added ->
                mensaje = context.getString(
                    if (added) R.string.diag_tile_added else R.string.diag_tile_denied,
                )
            }
        },
        onOpenQuickSettings = { context.startActivity(quickSettingsIntent(context)) },
        onMessageShown = { mensaje = null },
        onBack = onBack,
    )
}

/** Fuerza un repintado del widget para que el cambio se vea al momento. */
private fun repintar(context: Context) {
    (context.applicationContext as UsipipoApp).container.widgetUpdater.refreshNow()
}

private fun widgetPresente(context: Context): Boolean {
    val ids = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, UsipipoWidgetProvider::class.java))
    return ids.isNotEmpty()
}

/** La acción de Ajustes rápidos no está en el SDK público: va como string. */
private const val ACCION_AJUSTES_RAPIDOS = "android.settings.QUICK_SETTINGS_SETTINGS"

private fun quickSettingsIntent(context: Context): Intent {
    val directo = Intent(ACCION_AJUSTES_RAPIDOS).addCategory(Intent.CATEGORY_DEFAULT)
    return if (directo.resolveActivity(context.packageManager) != null) {
        directo
    } else {
        Intent(Settings.ACTION_SETTINGS)
    }
}
