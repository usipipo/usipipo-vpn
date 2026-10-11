package com.usipipo.vpn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.usipipo.vpn.AppContainer
import com.usipipo.vpn.ui.about.AboutScreen
import com.usipipo.vpn.ui.diagnostics.DiagnosticsScreen
import com.usipipo.vpn.ui.diagnostics.DiagnosticsViewModel
import com.usipipo.vpn.ui.home.HomeScreen
import com.usipipo.vpn.ui.home.HomeViewModel
import com.usipipo.vpn.ui.importer.ImportScreen
import com.usipipo.vpn.ui.importer.ImportViewModel

private enum class Screen { Home, Import, About, Diagnostics }

/** Navegación mínima por estado: tres pantallas no justifican una librería de navegación. */
@Composable
fun UsipipoRoot(container: AppContainer) {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    BackHandler(enabled = screen != Screen.Home) {
        screen = if (screen == Screen.Diagnostics) Screen.About else Screen.Home
    }

    when (screen) {
        Screen.Home -> HomeScreen(
            vm = homeViewModel(container),
            onImport = { screen = Screen.Import },
            onAbout = { screen = Screen.About },
        )

        Screen.Import -> ImportScreen(
            vm = importViewModel(container),
            onBack = { screen = Screen.Home },
        )

        Screen.About -> AboutScreen(
            onBack = { screen = Screen.Home },
            onDiagnostics = { screen = Screen.Diagnostics },
        )

        Screen.Diagnostics -> DiagnosticsScreen(
            vm = diagnosticsViewModel(container),
            onBack = { screen = Screen.About },
        )
    }
}

@Composable
private fun homeViewModel(container: AppContainer): HomeViewModel = viewModel(
    factory = viewModelFactory {
        initializer { HomeViewModel(container.configStore, container.tunnelManager) }
    },
)

@Composable
private fun importViewModel(container: AppContainer): ImportViewModel = viewModel(
    factory = viewModelFactory {
        initializer { ImportViewModel(container.configImporter, container.configStore) }
    },
)

@Composable
private fun diagnosticsViewModel(container: AppContainer): DiagnosticsViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            DiagnosticsViewModel(
                app = container.appContext,
                settings = container.diagnosticsSettings,
                tunnelManager = container.tunnelManager,
                store = container.configStore,
            )
        }
    },
)
