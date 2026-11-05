package com.usipipo.vpn.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.usipipo.vpn.ui.settings.SettingsRoute
import com.usipipo.vpn.ui.settings.SettingsScreen
import com.usipipo.vpn.ui.settings.SettingsViewModel
import com.usipipo.vpn.ui.settings.appearance.AppearanceRoute
import com.usipipo.vpn.ui.settings.appearance.AppearanceViewModel
import com.usipipo.vpn.ui.settings.notifications.NotificationsRoute
import com.usipipo.vpn.ui.settings.notifications.NotificationsViewModel
import com.usipipo.vpn.ui.settings.security.SecurityRoute
import com.usipipo.vpn.ui.settings.security.SecurityViewModel
import com.usipipo.vpn.ui.details.CountryPingViewModel
import com.usipipo.vpn.ui.details.DetailsScreen
import com.usipipo.vpn.ui.details.StatsViewModel
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
fun UsipipoRoot(container: AppContainer, initialTab: MainTab = MainTab.Home) {
    var tab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val updateStatus by container.updateStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Vive a nivel raiz: la consulta de pais ocurre al conectar, no solo al abrir Detalles.
    val countryPingVm = countryPingViewModel(container)

    // El ping solo corre con la app en primer plano: en segundo plano consume bateria
    // y consulta un tunel que puede no estar activo. La geolocalizacion del endpoint
    // tambien: solo se consulta en primer plano (y si el tunel ya estaba conectado,
    // la consulta espera a que la app vuelva a primer plano).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    countryPingVm.setAppForeground(true)
                    countryPingVm.setDetailsVisible(showDetails)
                }
                Lifecycle.Event.ON_STOP -> {
                    countryPingVm.setAppForeground(false)
                    countryPingVm.setDetailsVisible(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Versión cuya ventana ya se cerró (descarte o descarga): no reaparece.
    var handledUpdate by rememberSaveable { mutableStateOf<String?>(null) }

    val settingsVm = remember { SettingsViewModel(container.appSettings) }

    // Subpantalla de Ajustes abierta (null = se está en la lista de secciones).
    var settingsRoute by rememberSaveable { mutableStateOf<SettingsRoute?>(null) }

    BackHandler(enabled = backTarget(tab) != null) {
        backTarget(tab)?.let { tab = it }
    }
    BackHandler(enabled = showDetails) { showDetails = false }
    BackHandler(enabled = settingsRoute != null) { settingsRoute = null }

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
                // Sin weight(1f) cada pantalla, que usa fillMaxSize(), se come todo el
                // alto de la columna y la barra inferior se queda con altura cero.
                modifier = Modifier.weight(1f),
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
                        val countryPing by countryPingVm.ui.collectAsStateWithLifecycle()
                        val homeState by homeVm.uiState.collectAsStateWithLifecycle()
                        val activeId = (homeState.tunnel as? TunnelState.Connected)?.configId
                        val activeConfig = homeState.configs.firstOrNull { it.id == activeId }
                        // El ping solo corre mientras Detalles esta visible.
                        LaunchedEffect(showDetails) { countryPingVm.setDetailsVisible(showDetails) }
                        DisposableEffect(Unit) {
                            onDispose { countryPingVm.setDetailsVisible(false) }
                        }
                        if (showDetails) {
                            DetailsScreen(
                                config = activeConfig,
                                tunnel = homeState.tunnel,
                                stats = stats,
                                countryPing = countryPing,
                                onClose = { showDetails = false },
                                onDisconnect = { homeVm.disconnect() },
                                onRename = { name -> homeVm.rename(activeId.orEmpty(), name) },
                            )
                        } else {
                            HomeScreen(
                                vm = homeVm,
                                stats = stats,
                                onImport = { tab = MainTab.Import },
                                onOpenProfiles = { tab = MainTab.Profiles },
                                onOpenDetails = { showDetails = true },
                            )
                        }
                    }

                    MainTab.Settings -> {
                        val route = settingsRoute
                        if (route == null) {
                            SettingsScreen(
                                viewModel = settingsVm,
                                // Cada sección se activa en la fase que la implementa:
                                // hasta entonces tocar su fila no lleva a ninguna parte.
                                onSectionClick = { clicked ->
                                    if (clicked == SettingsRoute.Appearance ||
                                        clicked == SettingsRoute.Security ||
                                        clicked == SettingsRoute.Notifications
                                    ) {
                                        settingsRoute = clicked
                                    }
                                },
                            )
                        } else if (route == SettingsRoute.Appearance) {
                            AppearanceRoute(
                                viewModel = appearanceViewModel(container),
                                onBack = { settingsRoute = null },
                            )
                        } else if (route == SettingsRoute.Security) {
                            SecurityRoute(
                                viewModel = securityViewModel(container),
                                onBack = { settingsRoute = null },
                            )
                        } else if (route == SettingsRoute.Notifications) {
                            NotificationsRoute(
                                viewModel = notificationsViewModel(container),
                                onBack = { settingsRoute = null },
                            )
                        }
                    }

                    MainTab.Profiles -> ConfigsScreen(
                        vm = homeViewModel(container),
                        onImport = { tab = MainTab.Import },
                    )

                    MainTab.Import -> ImportScreen(
                        vm = importViewModel(container),
                        onDone = { tab = MainTab.Home },
                    )
                }
            }

            // En una subpantalla de Ajustes se oculta la barra inferior: es el patrón
            // de "más opciones" de los ajustes del sistema.
            if (!WindowInsets.isImeVisible && settingsRoute == null) {
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

    // Aviso de actualización disponible, además del diálogo. Al tocar el aviso se abre
    // la app; el diálogo aparece solo porque el estado de actualización sigue vigente.
    LaunchedEffect(available) {
        val aviso = available ?: return@LaunchedEffect
        container.notificationsManager.publishUpdateAvailable(aviso.info.version.toString())
    }

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
private fun appearanceViewModel(container: AppContainer): AppearanceViewModel = viewModel(
    factory = viewModelFactory {
        initializer { AppearanceViewModel(container.appSettings) }
    },
)

@Composable
private fun securityViewModel(container: AppContainer): SecurityViewModel = viewModel(
    factory = viewModelFactory {
        initializer { SecurityViewModel(container.appSettings) }
    },
)

/**
 * El permiso de notificaciones lo decide el sistema: se lee al construir el estado
 * y al volver a la app, no se guarda en ajustes.
 */
@Composable
private fun notificationsViewModel(container: AppContainer): NotificationsViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            NotificationsViewModel(container.appSettings) {
                ContextCompat.checkSelfPermission(
                    container.appContext,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    },
)

@Composable
private fun homeViewModel(container: AppContainer): HomeViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            HomeViewModel(
                container.configStore,
                container.tunnelManager,
                container.tunnelConnector,
                container.configTextReader,
            )
        }
    },
)

@Composable
private fun countryPingViewModel(container: AppContainer): CountryPingViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            CountryPingViewModel(
                store = container.configStore,
                tunnelManager = container.tunnelManager,
                countryRepository = container.countryRepository,
                pingRepository = container.pingRepository,
            )
        }
    },
)

@Composable
private fun importViewModel(container: AppContainer): ImportViewModel = viewModel(
    factory = viewModelFactory {
        initializer { ImportViewModel(container.configImporter, container.configStore) }
    },
)
