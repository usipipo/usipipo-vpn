package com.usipipo.vpn.ui.settings.bubble

import android.content.Context
import android.content.Intent
import android.net.Uri
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

/**
 * Subpantalla de Burbuja de tráfico con su ViewModel.
 *
 * El permiso de superposición se pide al encender el interruptor y se vuelve a leer
 * al regresar a la app, porque se concede en los ajustes del sistema.
 */
@Composable
fun TrafficBubbleRoute(
    viewModel: TrafficBubbleViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
        context.startActivity(overlayPermissionIntent(context))
    }

    TrafficBubbleScreen(
        bubbleEnabled = state.bubbleEnabled,
        overlayGranted = state.overlayGranted,
        onBubbleChange = { activar ->
            pedirPermiso = pedirPermiso || activar
            viewModel.setBubbleEnabled(activar)
        },
        onGrantOverlay = { context.startActivity(overlayPermissionIntent(context)) },
        onBack = onBack,
    )
}

/** Ajustes de superposición de la app; el permiso no se puede pedir en tiempo de ejecución. */
private fun overlayPermissionIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
