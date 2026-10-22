package com.usipipo.vpn.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El set de iconos propio declara un estilo uniforme: trazo de 1.75, tapas redondeadas y
 * relleno transparente. Sin `fillColor` explicito Android rellena de negro opaco, y un
 * path abierto (el del boton de power, el de actualizar) se cerraria y saldria un disco
 * macizo en lugar de un contorno.
 *
 * El invariante se comprueba leyendo los vectores del disco, porque no hay forma de
 * assertar sobre el trazo de un `ImageVector` desde un test de JVM. El XML se lee con
 * expresiones regulares y no con el DOM: el parser de Java no resuelve los espacios de
 * nombres por defecto, asi que `getAttributeNS` devolveria la cadena vacia.
 */
class IconSetStyleTest {

    @Test
    fun `todos los iconos declaran su relleno y ninguno se rellena de negro opaco`() {
        iconosDelSet().forEach { nombre ->
            trazados(nombre).forEachIndexed { indice, bloque ->
                val relleno = atributo(bloque, "fillColor")
                assertTrue("$nombre: el trazado $indice no declara fillColor", relleno.isNotEmpty())
                assertTrue(
                    "$nombre: el trazado $indice se rellena de negro opaco",
                    relleno != "#FF000000",
                )
            }
        }
    }

    @Test
    fun `todo trazo del set usa el grosor uniforme y las tapas redondeadas`() {
        iconosDelSet().forEach { nombre ->
            trazados(nombre).forEachIndexed { indice, bloque ->
                if (atributo(bloque, "strokeColor").isEmpty()) return@forEachIndexed
                assertEquals(
                    "$nombre: el trazado $indice no usa el grosor del set",
                    "1.75",
                    atributo(bloque, "strokeWidth"),
                )
                assertEquals(
                    "$nombre: el trazado $indice no tiene las tapas redondeadas",
                    "round",
                    atributo(bloque, "strokeLineCap"),
                )
            }
        }
    }

    @Test
    fun `el set cubre los cuatro iconos de la barra y los de acciones`() {
        val nombres = iconosDelSet().map { it.removePrefix("ic_").removeSuffix(".xml") }
        listOf(
            "nav_home", "nav_configs", "nav_import", "nav_diagnostics",
            "arrow_right", "arrow_back", "add", "star", "star_filled",
            "edit", "delete", "qr", "clock", "download", "upload", "server",
        ).forEach {
            assertTrue("falta el icono $it en el set", it in nombres)
        }
    }

    private fun iconosDelSet(): List<String> =
        File(DRAWABLES).listFiles()
            .orEmpty()
            .filter { it.name.startsWith("ic_") && it.name.endsWith(".xml") }
            // El icono del launcher y el logo de la cabecera tienen su propia escala.
            .filterNot { it.name == "ic_launcher_foreground.xml" || it.name == "ic_logo_u.xml" }
            .map { it.name }

    private fun trazados(nombre: String): List<String> {
        val archivo = File(DRAWABLES, nombre)
        assertTrue("falta $nombre", archivo.exists())
        return PATH.findAll(archivo.readText()).map { it.value }.toList()
    }

    private fun atributo(bloque: String, nombre: String): String =
        Regex("android:${Regex.escape(nombre)}=\"([^\"]*)\"").find(bloque)?.groupValues?.get(1) ?: ""

    private companion object {
        const val DRAWABLES = "src/main/res/drawable"
        val PATH = Regex("<path\\s[^>]*?/>")
    }
}
