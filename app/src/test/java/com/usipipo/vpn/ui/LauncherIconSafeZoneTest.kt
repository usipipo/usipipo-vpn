package com.usipipo.vpn.ui

import java.io.File
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

    private companion object {
        const val SAFE_MIN = 21.0
        const val SAFE_MAX = 87.0
    }
}
