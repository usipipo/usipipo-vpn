package com.usipipo.vpn.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.usipipo.vpn.AppContainer
import com.usipipo.vpn.R
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.ui.components.UsipipoBottomBar
import com.usipipo.vpn.ui.configs.ConfigsScreen
import com.usipipo.vpn.ui.details.DetailsScreen
import com.usipipo.vpn.ui.details.StatsUiState
import com.usipipo.vpn.ui.details.StatsViewModel
import com.usipipo.vpn.ui.diagnostics.DiagnosticsScreen
import com.usipipo.vpn.ui.diagnostics.DiagnosticsViewModel
import com.usipipo.vpn.ui.home.HomeScreen
import com.usipipo.vpn.ui.home.HomeViewModel
import com.usipipo.vpn.ui.importer.ImportScreen
import com.usipipo.vpn.ui.importer.ImportViewModel
import com.usipipo.vpn.ui.nav.MainTab
import com.usipipo.vpn.ui.nav.backTarget
import com.usipipo.vpn.ui.theme.rememberReduceMotion
import com.usipipo.vpn.ui.update.UpdateDialog
import com.usipipo.vpn.update.UpdateStatus

/** Navegación por pestañas con barra inferior. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsipipoRoot(container: AppContainer) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val updateStatus by container.updateStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Versión cuya ventana ya se cerró (descarte o descarga): no reaparece.
    var handledUpdate by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = backTarget(tab) != null) {
        backTarget(tab)?.let { tab = it }
    }
    BackHandler(enabled = showDetails) { showDetails = false }

    val reduceMotion = rememberReduceMotion()

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    if (reduceMotion) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(animationSpec = tween(durationMillis = 250)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 250))
                    }
                },
                label = "tab",
            ) { target ->
                when (target) {
                    MainTab.Home -> {
                        val homeVm = homeViewModel(container)
                        val stats by statsViewModel(container).ui.collectAsStateWithLifecycle()
                        if (showDetails) {
                            val homeState by homeVm.uiState.collectAsStateWithLifecycle()
                            val activeId = (homeState.tunnel as? TunnelState.Connected)?.configId
                            DetailsScreen(
                                config = homeState.configs.firstOrNull { it.id == activeId },
                                tunnel = homeState.tunnel,
                                stats = stats,
                                onClose = { showDetails = false },
                                onDisconnect = { homeVm.disconnect() },
                                onRename = { name -> homeVm.rename(activeId.orEmpty(), name) },
                            )
                        } else {
                            HomeScreen(
                                vm = homeVm,
                                stats = stats,
                                onImport = { tab = MainTab.Import },
                                onOpenConfigs = { tab = MainTab.Configs },
                                onOpenDetails = { showDetails = true },
                            )
                        }
                    }

                    MainTab.Configs -> ConfigsScreen(
                        vm = homeViewModel(container),
                        onImport = { tab = MainTab.Import },
                    )

                    MainTab.Import -> ImportScreen(
                        vm = importViewModel(container),
                        onDone = { tab = MainTab.Home },
                    )

                    MainTab.Diagnostics -> DiagnosticsScreen(
                        vm = diagnosticsViewModel(container),
                        onCheckUpdates = {
                            // La comprobación manual siempre vuelve a mostrar la ventana si hay release.
                            handledUpdate = null
                            container.checkForUpdates(force = true)
                        },
                    )
                }
            }

            if (!WindowInsets.isImeVisible) {
                UsipipoBottomBar(
                    tab = tab,
                    onTabClick = {
                        tab = it
                        showDetails = false
                    },
                )
            }
        }
    }

    val available = updateStatus as? UpdateStatus.UpdateAvailable
    if (available != null && handledUpdate != available.info.version.toString()) {
        UpdateDialog(
            info = available.info,
            onDownload = {
                // Descargar también cierra la ventana: no se pospone la versión (R12 solo
                // pospone con «Más tarde»), pero al volver del navegador no reaparece.
                handledUpdate = available.info.version.toString()
                openRelease(context, available.info.htmlUrl)
            },
            onPostpone = { container.postponeUpdate(available.info.version) },
            onDismiss = { handledUpdate = available.info.version.toString() },
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
private fun statsViewModel(container: AppContainer): StatsViewModel = viewModel(
    factory = viewModelFactory {
        initializer { StatsViewModel(container.configStore, container.tunnelManager) }
    },
)

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
