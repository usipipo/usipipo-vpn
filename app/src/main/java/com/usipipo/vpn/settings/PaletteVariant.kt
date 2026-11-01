package com.usipipo.vpn.settings

/**
 * Variante de paleta de color. Cada variante define TODOS los tokens de color
 * (esquema Material, estado del botón, ping, barra inferior, atmósfera) para
 * modo claro y oscuro, así nunca quedan huecos al cambiar de paleta.
 */
enum class PaletteVariant { AzulNoche, VerdeBosque, Ambar }

/** Todos los tokens de color de una paleta, en modo claro u oscuro. */
data class PaletteTokens(
    // Esquema Material 3
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val background: Int,
    val surface: Int,
    val surfaceVariant: Int,
    val outline: Int,
    val secondary: Int,
    val error: Int,
    // Estado del túnel (botones y badges)
    val statusIdle: Int,
    val statusConnected: Int,
    val statusError: Int,
    // Ping por latencia
    val pingGood: Int,
    val pingWarn: Int,
    val pingBad: Int,
    val pingUnknown: Int,
    // Barra inferior
    val bottomBarContainer: Int,
    val bottomBarActive: Int,
    val bottomBarInactive: Int,
    val bottomBarActiveLabel: Int,
    val bottomBarInactiveLabel: Int,
    // Degradados de atmósfera por fase (calm / drifting / flowing / alert)
    val calmTop: Int,
    val calmBottom: Int,
    val driftingTop: Int,
    val driftingBottom: Int,
    val flowingTop: Int,
    val flowingBottom: Int,
    val alertTop: Int,
    val alertBottom: Int,
)

/** Tokens de cada variante, para modo claro y oscuro. */
fun PaletteVariant.tokens(isDark: Boolean): PaletteTokens = when (this) {
    PaletteVariant.AzulNoche -> if (isDark) AzulNocheDark else AzulNocheLight
    PaletteVariant.VerdeBosque -> if (isDark) VerdeBosqueDark else VerdeBosqueLight
    PaletteVariant.Ambar -> if (isDark) AmbarDark else AmbarLight
}

private val AzulNocheLight = PaletteTokens(
    primary = 0xFFA8CCFF, onPrimary = 0xFF0B2545, primaryContainer = 0xFFD6E6FB,
    background = 0xFFF3F8FF, surface = 0xFFFFFFFF, surfaceVariant = 0xFFE8F1FD,
    outline = 0xFFD6E6FB, secondary = 0xFF4A6283, error = 0xFFF4A3A3,
    statusIdle = 0xFFA8CCFF, statusConnected = 0xFF6FCFC0, statusError = 0xFFF4A3A3,
    pingGood = 0xFF1B7F4B, pingWarn = 0xFF8A5A00, pingBad = 0xFFB3261E, pingUnknown = 0xFF4A6283,
    bottomBarContainer = 0xFFE8F1FD, bottomBarActive = 0xFF0B57C9, bottomBarInactive = 0xFF5B6E8C,
    bottomBarActiveLabel = 0xFF0B2545, bottomBarInactiveLabel = 0xFF5B6E8C,
    calmTop = 0xFFF3F8FF, calmBottom = 0xFFD6E6FB,
    driftingTop = 0xFFEDF7FD, driftingBottom = 0xFFC6E9E0,
    flowingTop = 0xFFE3F1FE, flowingBottom = 0xFFA9E5D8,
    alertTop = 0xFFFDF6F7, alertBottom = 0xFFF7DCE2,
)

private val AzulNocheDark = PaletteTokens(
    primary = 0xFF4C8DFF, onPrimary = 0xFF04122B, primaryContainer = 0xFF14386B,
    background = 0xFF061631, surface = 0xFF0E2447, surfaceVariant = 0xFF12305A,
    outline = 0xFF244A7C, secondary = 0xFF2DD4BF, error = 0xFFF4A3A3,
    statusIdle = 0xFF8DBBFF, statusConnected = 0xFF4FB3A6, statusError = 0xFFF4A3A3,
    pingGood = 0xFF5BD98A, pingWarn = 0xFFF5C451, pingBad = 0xFFF87171, pingUnknown = 0xFFB7C9E2,
    bottomBarContainer = 0xFF000A25, bottomBarActive = 0xFF06D3FE, bottomBarInactive = 0xFF6B87A0,
    bottomBarActiveLabel = 0xFFE6F0FF, bottomBarInactiveLabel = 0xFF6B87A0,
    calmTop = 0xFF04132D, calmBottom = 0xFF0B2147,
    driftingTop = 0xFF06182E, driftingBottom = 0xFF15403C,
    flowingTop = 0xFF071D33, flowingBottom = 0xFF114C40,
    alertTop = 0xFF231620, alertBottom = 0xFF32202B,
)

private val VerdeBosqueLight = PaletteTokens(
    primary = 0xFF2E7D5B, onPrimary = 0xFFFFFFFF, primaryContainer = 0xFFD8F3E4,
    background = 0xFFF1F8F4, surface = 0xFFFFFFFF, surfaceVariant = 0xFFE3F0E9,
    outline = 0xFFD8F3E4, secondary = 0xFF4A6283, error = 0xFFF4A3A3,
    statusIdle = 0xFF5BD98A, statusConnected = 0xFF2E7D5B, statusError = 0xFFF4A3A3,
    pingGood = 0xFF5BD98A, pingWarn = 0xFFF5C451, pingBad = 0xFFF87171, pingUnknown = 0xFFB7C9E2,
    bottomBarContainer = 0xFFE3F0E9, bottomBarActive = 0xFF2E7D5B, bottomBarInactive = 0xFF5B6E8C,
    bottomBarActiveLabel = 0xFF2E7D5B, bottomBarInactiveLabel = 0xFF5B6E8C,
    calmTop = 0xFFF1F8F4, calmBottom = 0xFFD8F3E4,
    driftingTop = 0xFFE9F4EC, driftingBottom = 0xFFBEE0CC,
    flowingTop = 0xFFE8F4ED, flowingBottom = 0xFFB9E8D0,
    alertTop = 0xFFFDF6F7, alertBottom = 0xFFF7DCE2,
)

private val VerdeBosqueDark = AzulNocheDark

private val AmbarLight = PaletteTokens(
    primary = 0xFFC8861E, onPrimary = 0xFFFFFFFF, primaryContainer = 0xFFFDEACB,
    background = 0xFFFFFBF0, surface = 0xFFFFFFFF, surfaceVariant = 0xFFFDf0D9,
    outline = 0xFFFDEACB, secondary = 0xFF4A6283, error = 0xFFF4A3A3,
    statusIdle = 0xFFF5C451, statusConnected = 0xFFC8861E, statusError = 0xFFF4A3A3,
    pingGood = 0xFFF5C451, pingWarn = 0xFFE8A53C, pingBad = 0xFFF87171, pingUnknown = 0xFFB7C9E2,
    bottomBarContainer = 0xFFFDF0D9, bottomBarActive = 0xFFC8861E, bottomBarInactive = 0xFF5B6E8C,
    bottomBarActiveLabel = 0xFFC8861E, bottomBarInactiveLabel = 0xFF5B6E8C,
    calmTop = 0xFFFFFBF0, calmBottom = 0xFFFDEACB,
    driftingTop = 0xFFF7E8D0, driftingBottom = 0xFFECD8A8,
    flowingTop = 0xFFFDF0D9, flowingBottom = 0xFFEBD8A8,
    alertTop = 0xFFFDF6F7, alertBottom = 0xFFF7DCE2,
)

private val AmbarDark = AzulNocheDark
