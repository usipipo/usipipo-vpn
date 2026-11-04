package com.usipipo.vpn.ui.settings.security

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usipipo.vpn.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Subpantalla de Seguridad con su ViewModel.
 *
 * La autenticación biométrica se pide solo al activar el interruptor: si falla, el
 * ajuste vuelve a quedar apagado y se avisa. La pantalla de bloqueo al volver del
 * segundo plano queda fuera de esta fase (así lo marca el spec).
 */
@Composable
fun SecurityRoute(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context as? FragmentActivity }
    val aviso = context.getString(R.string.security_biometric_failed)

    // El prompt solo se lanza cuando el usuario toca el interruptor.
    var pedir by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    val info = remember(aviso) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(aviso)
            .setSubtitle(context.getString(R.string.security_biometric_prompt))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    )
                } else {
                    // En API 26..29 no se pueden combinar; se permite solo la credencial.
                    @Suppress("DEPRECATION")
                    setDeviceCredentialAllowed(true)
                }
            }
            .build()
    }

    LaunchedEffect(pedir) {
        if (!pedir) return@LaunchedEffect
        pedir = false
        val concedido = autenticar(activity, info)
        viewModel.setBiometricEnabled(concedido)
        if (!concedido) mensaje = aviso
    }

    SecurityScreen(
        biometricEnabled = state.biometricEnabled,
        // Siempre-on y bloqueo no se pueden leer: el sistema no expone isAlwaysOn() ni
        // isLockdownEnabled() a apps de terceros.
        alwaysOn = null,
        lockdown = null,
        excludedApps = state.excludedApps,
        mensaje = mensaje,
        onMensajeConsumido = { mensaje = null },
        onBiometricChange = { activar ->
            if (activar) pedir = true else viewModel.setBiometricEnabled(false)
        },
        onRemoveApp = { paquete ->
            viewModel.setExcludedApps(state.excludedApps - paquete)
        },
        onOpenVpnSettings = {
            context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        },
        onBack = onBack,
    )
}

/**
 * Suspende hasta que el usuario resuelve el prompt biométrico.
 *
 * Sin actividad [FragmentActivity] no hay dónde mostrar el prompt, así que se trata
 * como fallo: nunca se deja el ajuste activo sin haber comprobado la identidad.
 */
private suspend fun autenticar(
    activity: FragmentActivity?,
    info: BiometricPrompt.PromptInfo,
): Boolean {
    if (activity == null) return false
    return suspendCancellableCoroutine { cont ->
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (cont.isActive) cont.resume(false)
                }
            },
        )
        prompt.authenticate(info)
        cont.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}
