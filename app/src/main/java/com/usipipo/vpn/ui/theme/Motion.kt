package com.usipipo.vpn.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * `null` = leer el ajuste del sistema; un valor fijo permite previsualizar ambos modos
 * en @Preview y probar el camino reducido sin cambiar ajustes del teléfono.
 */
val LocalReduceMotion = staticCompositionLocalOf<Boolean?> { null }

@Composable
fun rememberReduceMotion(): Boolean {
    LocalReduceMotion.current?.let { return it }
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
