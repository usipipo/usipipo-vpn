package com.usipipo.vpn.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.usipipo.vpn.ui.theme.AtmospherePalette
import kotlin.math.PI
import kotlin.math.sin

/** Columnas de la máscara del mapa (5° de longitud por celda). */
internal const val MAP_COLUMNS = 72

/** Filas de la máscara del mapa. */
internal const val MAP_ROWS = 36

/** Fracción del ancho que ocupa la malla de puntos. */
private const val MAP_WIDTH_FRACTION = 0.98f

/** Altura (fracción) donde queda el centro vertical del mapa. */
private const val MAP_VERTICAL_CENTER = 0.34f

/** Opacidad base de los puntos sobre el degradado. */
private const val DOT_ALPHA_DARK = 0.30f
private const val DOT_ALPHA_LIGHT = 0.12f

/**
 * Máscara de tierra en hexadecimal: una fila por línea, 18 dígitos por fila
 * (4 celdas por dígito; el bit 3 es la columna 0 de cada grupo).
 *
 * Costa rasterizada sobre Natural Earth 50m land (dominio público), de 87.5°N
 * a 57.5°S. 776 de 2592 celdas son tierra.
 *
 * La primera y la última fila comparten línea con las comillas para que el
 * literal sea una constante de compilación: `trimIndent()` no vale en un
 * `const val`, y así las 36 filas salen limpias sin sangrar.
 */
private const val MAP_MASK_HEX = """000000000000000000
00003EFF4001000000
000431FF0180008000
0018F87F00000FB480
1F77D63E01E0BFFFFF
5FFFE670836FFFFFFF
1FFF8C2006FFFFFFE6
087FC70012FFFFFF18
007FFF8037FFFFFF90
001FFD800FFFFFFF80
001FFF000DDFFFFE00
001FF80030EFFFFC00
000FF80010FFFFFC00
0007F0003C1FFFF200
000390003FFFFFF000
000380007FFC7FF000
0001A8007FEF1B8000
0000C0007FF6118000
000020007FF810C000
000017007FFC100000
000007C027FC019000
00000FC003F800B800
00000FF003F000A100
00000FF801F0006080
00000FF801F0000080
000007F003F4000680
000003F001E4000F80
000003E001E4001FC0
000003C001E0001FE0
000003C000C0001FE0
0000038000000000C0
000007000000000000
000006000000000002
000006000000000000
000004000000000000
000000000000000000"""

/** Las 36 filas de la máscara ya troceadas. */
private val MASK_ROWS: List<String> = MAP_MASK_HEX.lines()

/** Las 36 filas de la máscara, en hexadecimal. */
internal fun mapMaskRows(): List<String> = MASK_ROWS

/** true si la celda (columna, fila) es tierra. Fuera de la máscara, mar. */
internal fun isLand(column: Int, row: Int): Boolean {
    if (column !in 0 until MAP_COLUMNS || row !in 0 until MAP_ROWS) return false
    val digito = MASK_ROWS[row][column / 4]
    val bit = 3 - (column % 4)
    return (digito.digitToInt(16) shr bit) and 1 == 1
}

/**
 * Capa de puntos del mapa del mundo, dibujada encima del degradado del Inicio
 * y por detrás del contenido. Estática: se calcula una vez por tamaño de pantalla.
 */
@Composable
internal fun WorldMapLayer(
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (dark) {
        Color(AtmospherePalette.MAP_DOT_DARK)
    } else {
        Color(AtmospherePalette.MAP_DOT_LIGHT)
    }
    val alpha = if (dark) DOT_ALPHA_DARK else DOT_ALPHA_LIGHT
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val cell = size.width * MAP_WIDTH_FRACTION / MAP_COLUMNS
                val radius = cell * 0.22f
                val mapWidth = cell * MAP_COLUMNS
                val mapHeight = cell * MAP_ROWS
                val left = (size.width - mapWidth) / 2f
                val top = size.height * MAP_VERTICAL_CENTER - mapHeight / 2f
                onDrawBehind {
                    for (row in 0 until MAP_ROWS) {
                        for (column in 0 until MAP_COLUMNS) {
                            if (!isLand(column, row)) continue
                            val cx = left + cell * (column + 0.5f)
                            val cy = top + cell * (row + 0.5f)
                            // Desvanecido hacia los bordes (seno) y hacia abajo.
                            val nx = (cx - left) / mapWidth
                            val ny = (cy - top) / mapHeight
                            val edge = sin(PI * nx.toDouble()).toFloat()
                            val down = 1f - ny * 0.85f
                            drawCircle(
                                color = color.copy(alpha = alpha * edge * down),
                                radius = radius,
                                center = Offset(cx, cy),
                            )
                        }
                    }
                }
            },
    ) {
        // Los puntos se dibujan en el bloque drawWithCache; el lienzo no lleva contenido.
    }
}
