package com.usipipo.vpn.ui

import java.io.File
import kotlin.math.hypot
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El icono adaptativo se recorta fuera de la zona segura: el lienzo del
 * foreground es 108x108 y solo los 66x66 centrales (21..87) sobreviven a las
 * máscaras del sistema. Este test lee el vector y comprueba que nada se sale.
 *
 * Se miran solo las coordenadas de `pathData`: el resto de numeros del archivo
 * (el `1.0` de la declaracion XML, el `108` del tamano del lienzo, el grosor
 * de trazo) no son coordenadas y no dicen nada de la zona segura.
 *
 * Ademas del cuadrado, la mascara circular solo deja ver un disco de radio 36
 * centrado en (54,54), y la zona segura que garantiza Android es el disco de
 * radio 33. Un icono que se sale de ese disco sale shaveado en los lanzadores
 * que usan mascara circular, aunque quepa en el cuadrado.
 */
class LauncherIconSafeZoneTest {

    private val foreground = File("src/main/res/drawable/ic_launcher_foreground.xml")

    @Test
    fun `el icono del launcher existe y dibuja dentro de la zona segura`() {
        assertTrue("falta ${foreground.path}", foreground.exists())
        val contenido = foreground.readText()
        val trazados = Regex("android:pathData=\"([^\"]*)\"")
            .findAll(contenido)
            .map { it.groupValues[1] }
            .toList()
        assertTrue("el vector no dibuja ninguna ruta", trazados.isNotEmpty())
        val numeros = Regex("-?\\d+(?:\\.\\d+)?")
            .findAll(trazados.joinToString(" "))
            .map { it.value.toDouble() }
            .toList()
        assertTrue("el vector no tiene coordenadas", numeros.isNotEmpty())
        val fuera = numeros.filter { it < SAFE_MIN || it > SAFE_MAX }
        assertTrue("coordenadas fuera de la zona segura: $fuera", fuera.isEmpty())
    }

    @Test
    fun `la tinta del launcher no se sale de la mascara circular`() {
        val contenido = foreground.readText()
        val bloques = Regex("<path\\s[^>]*?/>").findAll(contenido).map { it.value }.toList()
        assertTrue("el vector no dibuja ninguna ruta", bloques.isNotEmpty())
        val radioMaximo = bloques.maxOf { radioMaximoDeTinta(it) }
        assertTrue(
            "la tinta llega a $radioMaximo y la mascara circular solo deja ver $RADIO_MASK",
            radioMaximo <= RADIO_SAFE,
        )
    }

    /**
     * Radio maximo de la tinta de un trazado, medido desde el centro del lienzo.
     * En un trazado con trazo, la tinta llega a la distancia del punto mas
     * lejano mas medio grosor; en uno relleno, la distancia maxima la marca un
     * vertice de la poligono.
     */
    private fun radioMaximoDeTinta(bloque: String): Double {
        val datos = Regex("android:pathData=\"([^\"]*)\"").find(bloque)?.groupValues?.get(1).orEmpty()
        val puntos = Regex("-?\\d+(?:\\.\\d+)?")
            .findAll(datos)
            .map { it.value.toDouble() }
            .toList()
            .chunked(2)
            .map { (x, y) -> x to y }
        assertTrue("un trazado no tiene coordenadas", puntos.isNotEmpty())
        val relleno = Regex("android:fillColor=\"([^\"]*)\"").find(bloque)?.groupValues?.get(1)
        val medioTrazo = if (relleno == "#00000000") {
            Regex("android:strokeWidth=\"([^\"]*)\"").find(bloque)?.groupValues?.get(1)?.toDouble()?.div(2) ?: 0.0
        } else {
            0.0
        }
        val distancia = puntos.maxOf { (x, y) ->
            hypot(x - CENTRO, y - CENTRO)
        }
        return distancia + medioTrazo
    }

    private companion object {
        const val SAFE_MIN = 21.0
        const val SAFE_MAX = 87.0
        const val CENTRO = 54.0
        const val RADIO_MASK = 36.0
        const val RADIO_SAFE = 33.0
    }
}
