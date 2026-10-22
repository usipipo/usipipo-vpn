package com.usipipo.vpn.ui.theme

import com.usipipo.vpn.domain.TunnelState

/**
 * Paleta de «Atmósfera» en ARGB (sin tipos de Compose, para poder testearla en JVM).
 * El texto del diseño exige: claro = degradado azul pastel → turquesa/menta, oscuro = azul
 * noche → verde azulado profundo (no una inversión de la paleta clara).
 */
object AtmospherePalette {
    // Fondos (claro) — sin cambios
    const val LIGHT_CALM_TOP: Long = 0xFFF3F8FF
    const val LIGHT_CALM_BOTTOM: Long = 0xFFD6E6FB
    const val LIGHT_DRIFTING_TOP: Long = 0xFFEDF7FD
    const val LIGHT_DRIFTING_BOTTOM: Long = 0xFFC6E9E0
    const val LIGHT_FLOWING_TOP: Long = 0xFFE3F1FE
    const val LIGHT_FLOWING_BOTTOM: Long = 0xFFA9E5D8
    const val LIGHT_ALERT_TOP: Long = 0xFFFDF6F7
    const val LIGHT_ALERT_BOTTOM: Long = 0xFFF7DCE2
    // Fondos (oscuro): azul noche del mockup
    const val DARK_CALM_TOP: Long = 0xFF04132D
    const val DARK_CALM_BOTTOM: Long = 0xFF0B2147
    const val DARK_DRIFTING_TOP: Long = 0xFF06182E
    const val DARK_DRIFTING_BOTTOM: Long = 0xFF15403C
    const val DARK_FLOWING_TOP: Long = 0xFF071D33
    const val DARK_FLOWING_BOTTOM: Long = 0xFF114C40
    const val DARK_ALERT_TOP: Long = 0xFF231620
    const val DARK_ALERT_BOTTOM: Long = 0xFF32202B
    // Esquema oscuro (pantallas sin degradado y tarjetas)
    const val DARK_BACKGROUND: Long = 0xFF061631
    const val DARK_SURFACE: Long = 0xFF0E2447
    const val DARK_SURFACE_VARIANT: Long = 0xFF12305A
    const val DARK_PRIMARY: Long = 0xFF4C8DFF
    const val DARK_ON_PRIMARY: Long = 0xFF04122B
    const val DARK_SECONDARY: Long = 0xFF2DD4BF
    const val DARK_ON_SECONDARY: Long = 0xFF042A26
    // Barra inferior: azul noche del mockup en oscuro, superficie clara equivalente en claro.
    const val BOTTOM_BAR_DARK: Long = 0xFF000A25
    const val BOTTOM_BAR_LIGHT: Long = 0xFFE8F1FD
    // La pestaña activa usa el cian del mockup; la inactiva, un gris azulado mas apagado
    // para que el estado se lea de un vistazo.
    const val BOTTOM_BAR_ACTIVE_DARK: Long = 0xFF06D3FE
    const val BOTTOM_BAR_ACTIVE_LABEL_DARK: Long = 0xFF2DA3D3
    const val BOTTOM_BAR_INACTIVE_DARK: Long = 0xFF627E93
    const val BOTTOM_BAR_ACTIVE_LIGHT: Long = 0xFF0B57C9
    const val BOTTOM_BAR_ACTIVE_LABEL_LIGHT: Long = 0xFF0B57C9
    const val BOTTOM_BAR_INACTIVE_LIGHT: Long = 0xFF5B6E8C
    // Texto e iconos
    const val TEXT_LIGHT: Long = 0xFF0B2545 // Navy
    const val TEXT_DARK: Long = 0xFFE6F0FF
    const val TEXT_LIGHT_SECONDARY: Long = 0xFF4A6283 // onSurfaceVariant claro
    const val TEXT_DARK_SECONDARY: Long = 0xFFB7C9E2 // onSurfaceVariant oscuro
    // Botón según estado
    const val BUTTON_LIGHT_IDLE: Long = 0xFFA8CCFF
    const val BUTTON_LIGHT_CONNECTED: Long = 0xFF6FCFC0 // turquesa pastel
    const val BUTTON_LIGHT_ERROR: Long = 0xFFF4A3A3
    const val BUTTON_DARK_IDLE: Long = 0xFF8DBBFF
    const val BUTTON_DARK_CONNECTED: Long = 0xFF4FB3A6 // turquesa profundo
    const val BUTTON_DARK_ERROR: Long = 0xFFF4A3A3
}

/** Fase visual de la pantalla; los estados de error comparten «Alert». */
enum class AtmospherePhase { Calm, Drifting, Flowing, Alert }

fun atmospherePhase(state: TunnelState): AtmospherePhase = when (state) {
    TunnelState.Disconnected -> AtmospherePhase.Calm
    is TunnelState.Connecting -> AtmospherePhase.Drifting
    is TunnelState.Connected -> AtmospherePhase.Flowing
    is TunnelState.NoResponse -> AtmospherePhase.Alert
    is TunnelState.Failed -> AtmospherePhase.Alert
}
