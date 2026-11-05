package com.usipipo.vpn.ui.settings.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Código de la petición del permiso de notificaciones. */
private const val REQUEST_POST_NOTIFICATIONS = 7

/**
 * Subpantalla de Notificaciones con su ViewModel.
 *
 * El permiso de notificaciones se pide al activar el primer aviso (API 33+) y se
 * vuelve a leer al regresar a la app, porque se concede en los ajustes del sistema.
 */
@Composable
fun NotificationsRoute(
    viewModel: NotificationsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    // El permiso se concede fuera de la app: hay que releerlo al volver.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var pedirPermiso by remember { mutableStateOf(false) }
    LaunchedEffect(pedirPermiso) {
        if (!pedirPermiso) return@LaunchedEffect
        pedirPermiso = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity?.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_POST_NOTIFICATIONS)
        }
    }

    NotificationsScreen(
        notificationsEnabled = state.notificationsEnabled,
        trafficEnabled = state.trafficEnabled,
        disconnectEnabled = state.disconnectEnabled,
        updatesEnabled = state.updatesEnabled,
        notificationsGranted = state.notificationsGranted,
        onNotificationsChange = { activar ->
            pedirPermiso = pedirPermiso || activar
            viewModel.setNotificationsEnabled(activar)
        },
        onTrafficChange = viewModel::setTrafficEnabled,
        onDisconnectChange = viewModel::setDisconnectEnabled,
        onUpdatesChange = viewModel::setUpdatesEnabled,
        onOpenNotificationSettings = { openNotificationSettings(context) },
        onBack = onBack,
    )
}

/** Ajustes de notificaciones de la app (API 26+). */
private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
}
