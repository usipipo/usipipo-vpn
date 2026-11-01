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

// Los literales ARGB con el canal alfa a 1 no caben en un Int (Kotlin los infiere como
// Long), así que se convierten explícitamente: Compose espera colores Int.
private val AzulNocheLight = PaletteTokens(
    primary = 0xFFA8CCFF.toInt(), onPrimary = 0xFF0B2545.toInt(), primaryContainer = 0xFFD6E6FB.toInt(),
    background = 0xFFF3F8FF.toInt(), surface = 0xFFFFFFFF.toInt(), surfaceVariant = 0xFFE8F1FD.toInt(),
    outline = 0xFFD6E6FB.toInt(), secondary = 0xFF4A6283.toInt(), error = 0xFFF4A3A3.toInt(),
    statusIdle = 0xFFA8CCFF.toInt(), statusConnected = 0xFF6FCFC0.toInt(), statusError = 0xFFF4A3A3.toInt(),
    pingGood = 0xFF1B7F4B.toInt(), pingWarn = 0xFF8A5A00.toInt(), pingBad = 0xFFB3261E.toInt(),
    pingUnknown = 0xFF4A6283.toInt(),
    bottomBarContainer = 0xFFE8F1FD.toInt(), bottomBarActive = 0xFF0B57C9.toInt(),
    bottomBarInactive = 0xFF5B6E8C.toInt(), bottomBarActiveLabel = 0xFF0B2545.toInt(),
    bottomBarInactiveLabel = 0xFF5B6E8C.toInt(),
    calmTop = 0xFFF3F8FF.toInt(), calmBottom = 0xFFD6E6FB.toInt(),
    driftingTop = 0xFFEDF7FD.toInt(), driftingBottom = 0xFFC6E9E0.toInt(),
    flowingTop = 0xFFE3F1FE.toInt(), flowingBottom = 0xFFA9E5D8.toInt(),
    alertTop = 0xFFFDF6F7.toInt(), alertBottom = 0xFFF7DCE2.toInt(),
)

private val AzulNocheDark = PaletteTokens(
    primary = 0xFF4C8DFF.toInt(), onPrimary = 0xFF04122B.toInt(), primaryContainer = 0xFF14386B.toInt(),
    background = 0xFF061631.toInt(), surface = 0xFF0E2447.toInt(), surfaceVariant = 0xFF12305A.toInt(),
    outline = 0xFF244A7C.toInt(), secondary = 0xFF2DD4BF.toInt(), error = 0xFFF4A3A3.toInt(),
    statusIdle = 0xFF8DBBFF.toInt(), statusConnected = 0xFF4FB3A6.toInt(), statusError = 0xFFF4A3A3.toInt(),
    pingGood = 0xFF5BD98A.toInt(), pingWarn = 0xFFF5C451.toInt(), pingBad = 0xFFF87171.toInt(),
    pingUnknown = 0xFFB7C9E2.toInt(),
    bottomBarContainer = 0xFF000A25.toInt(), bottomBarActive = 0xFF06D3FE.toInt(),
    bottomBarInactive = 0xFF6B87A0.toInt(), bottomBarActiveLabel = 0xFFE6F0FF.toInt(),
    bottomBarInactiveLabel = 0xFF6B87A0.toInt(),
    calmTop = 0xFF04132D.toInt(), calmBottom = 0xFF0B2147.toInt(),
    driftingTop = 0xFF06182E.toInt(), driftingBottom = 0xFF15403C.toInt(),
    flowingTop = 0xFF071D33.toInt(), flowingBottom = 0xFF114C40.toInt(),
    alertTop = 0xFF231620.toInt(), alertBottom = 0xFF32202B.toInt(),
)

private val VerdeBosqueLight = PaletteTokens(
    primary = 0xFF2E7D5B.toInt(), onPrimary = 0xFFFFFFFF.toInt(), primaryContainer = 0xFFD8F3E4.toInt(),
    background = 0xFFF1F8F4.toInt(), surface = 0xFFFFFFFF.toInt(), surfaceVariant = 0xFFE3F0E9.toInt(),
    outline = 0xFFD8F3E4.toInt(), secondary = 0xFF4A6283.toInt(), error = 0xFFF4A3A3.toInt(),
    statusIdle = 0xFF5BD98A.toInt(), statusConnected = 0xFF2E7D5B.toInt(), statusError = 0xFFF4A3A3.toInt(),
    pingGood = 0xFF5BD98A.toInt(), pingWarn = 0xFFF5C451.toInt(), pingBad = 0xFFF87171.toInt(),
    pingUnknown = 0xFFB7C9E2.toInt(),
    bottomBarContainer = 0xFFE3F0E9.toInt(), bottomBarActive = 0xFF2E7D5B.toInt(),
    bottomBarInactive = 0xFF5B6E8C.toInt(), bottomBarActiveLabel = 0xFF2E7D5B.toInt(),
    bottomBarInactiveLabel = 0xFF5B6E8C.toInt(),
    calmTop = 0xFFF1F8F4.toInt(), calmBottom = 0xFFD8F3E4.toInt(),
    driftingTop = 0xFFE9F4EC.toInt(), driftingBottom = 0xFFBEE0CC.toInt(),
    flowingTop = 0xFFE8F4ED.toInt(), flowingBottom = 0xFFB9E8D0.toInt(),
    alertTop = 0xFFFDF6F7.toInt(), alertBottom = 0xFFF7DCE2.toInt(),
)

private val VerdeBosqueDark = AzulNocheDark

private val AmbarLight = PaletteTokens(
    primary = 0xFFC8861E.toInt(), onPrimary = 0xFFFFFFFF.toInt(), primaryContainer = 0xFFFDEACB.toInt(),
    background = 0xFFFFFBF0.toInt(), surface = 0xFFFFFFFF.toInt(), surfaceVariant = 0xFFFDF0D9.toInt(),
    outline = 0xFFFDEACB.toInt(), secondary = 0xFF4A6283.toInt(), error = 0xFFF4A3A3.toInt(),
    statusIdle = 0xFFF5C451.toInt(), statusConnected = 0xFFC8861E.toInt(), statusError = 0xFFF4A3A3.toInt(),
    pingGood = 0xFFF5C451.toInt(), pingWarn = 0xFFE8A53C.toInt(), pingBad = 0xFFF87171.toInt(),
    pingUnknown = 0xFFB7C9E2.toInt(),
    bottomBarContainer = 0xFFFDF0D9.toInt(), bottomBarActive = 0xFFC8861E.toInt(),
    bottomBarInactive = 0xFF5B6E8C.toInt(), bottomBarActiveLabel = 0xFFC8861E.toInt(),
    bottomBarInactiveLabel = 0xFF5B6E8C.toInt(),
    calmTop = 0xFFFFFBF0.toInt(), calmBottom = 0xFFFDEACB.toInt(),
    driftingTop = 0xFFF7E8D0.toInt(), driftingBottom = 0xFFECD8A8.toInt(),
    flowingTop = 0xFFFDF0D9.toInt(), flowingBottom = 0xFFEBD8A8.toInt(),
    alertTop = 0xFFFDF6F7.toInt(), alertBottom = 0xFFF7DCE2.toInt(),
)

private val AmbarDark = AzulNocheDark
