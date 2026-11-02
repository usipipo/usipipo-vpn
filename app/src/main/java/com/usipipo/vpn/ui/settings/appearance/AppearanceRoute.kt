package com.usipipo.vpn.ui.settings.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Enruta la subpantalla de Apariencia con su ViewModel: mantiene el estado vivo
 * y avisa de cada cambio del usuario.
 */
@Composable
fun AppearanceRoute(
    viewModel: AppearanceViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppearanceScreen(
        themeMode = state.themeMode,
        animationsEnabled = state.animationsEnabled,
        palette = state.palette,
        onThemeModeChange = viewModel::setThemeMode,
        onAnimationsChange = viewModel::setAnimationsEnabled,
        onPaletteChange = viewModel::setPalette,
        onBack = onBack,
    )
}
