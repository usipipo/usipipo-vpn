package com.usipipo.vpn.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.usipipo.vpn.AppContainer
import com.usipipo.vpn.R
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.ui.about.AboutScreen
import com.usipipo.vpn.ui.diagnostics.DiagnosticsScreen
import com.usipipo.vpn.ui.diagnostics.DiagnosticsViewModel
import com.usipipo.vpn.ui.home.HomeScreen
import com.usipipo.vpn.ui.home.HomeViewModel
import com.usipipo.vpn.ui.importer.ImportScreen
import com.usipipo.vpn.ui.importer.ImportViewModel
import com.usipipo.vpn.ui.update.UpdateDialog
import com.usipipo.vpn.update.UpdateStatus

private enum class Screen { Home, Import, About, Diagnostics }

/** Navegación mínima por estado: tres pantallas no justifican una librería de navegación. */
@Composable
fun UsipipoRoot(container: AppContainer) {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    val updateStatus by container.updateStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Versión cuya ventana se cerró con atrás o toque fuera: no queda pospuesta.
    var dismissedUpdate by rememberSaveable { mutableStateOf<String?>(null) }

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
            onCheckUpdates = {
                // La comprobación manual siempre vuelve a mostrar la ventana si hay release.
                dismissedUpdate = null
                container.checkForUpdates(force = true)
            },
        )

        Screen.Diagnostics -> DiagnosticsScreen(
            vm = diagnosticsViewModel(container),
            onBack = { screen = Screen.About },
        )
    }

    val available = updateStatus as? UpdateStatus.UpdateAvailable
    if (available != null && dismissedUpdate != available.info.version.toString()) {
        UpdateDialog(
            info = available.info,
            onDownload = { openRelease(context, available.info.htmlUrl) },
            onPostpone = { container.postponeUpdate(available.info.version) },
            onDismiss = { dismissedUpdate = available.info.version.toString() },
        )
    }
}

/** «Descargar»: abre la release en el navegador; la app no instala nada. */
private fun openRelease(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        AppLog.w("Actualizar", context.getString(R.string.update_no_browser))
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
