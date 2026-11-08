package com.usipipo.vpn.ui.settings.storage

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usipipo.vpn.R

/**
 * Subpantalla de Almacenamiento con su ViewModel.
 *
 * El texto del resultado se resuelve aquí para que el ViewModel no dependa de Android.
 */
@Composable
fun StorageRoute(
    viewModel: StorageViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    StorageScreen(
        profilesBytes = state.profilesBytes,
        logBytes = state.logBytes,
        tempBytes = state.tempBytes,
        busy = state.busy,
        wipeWord = state.wipeWord,
        wipeReady = state.wipeReady,
        message = messageText(context, state.message),
        onClearLog = viewModel::clearLog,
        onClearCache = viewModel::clearCache,
        onWipeWordChange = viewModel::onWipeWordChange,
        onWipeAll = viewModel::wipeAll,
        onMessageShown = viewModel::consumeMessage,
        onBack = onBack,
    )
}

/** Texto del mensaje de resultado: la UI no interpreta los tipos sellados. */
private fun messageText(context: Context, message: StorageMessage?): String? =
    when (message) {
        null -> null
        StorageMessage.LogCleared -> context.getString(R.string.storage_log_cleared)
        StorageMessage.CacheCleared -> context.getString(R.string.storage_cache_cleared)
        StorageMessage.Wiped -> context.getString(R.string.storage_wiped)
        StorageMessage.Failed -> context.getString(R.string.storage_failed)
    }
