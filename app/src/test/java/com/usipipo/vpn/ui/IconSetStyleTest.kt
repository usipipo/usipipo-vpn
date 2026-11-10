package com.usipipo.vpn.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El set de iconos propio declara un estilo uniforme. Desde el paso a relleno hay dos
 * invariantes: todo `<path>` declara su `fillColor` y ninguno vale `#FF000000`, y todo
 * `<path>` que declara trazo usa el grosor del set (1.75) con tapas redondeadas. Sin
 * `fillColor` explicito Android rellena de negro opaco, y un path abierto (el del boton
 * de power, el de actualizar) se cerraria y saldria un disco macizo en lugar de un
 * contorno: el relleno se declara tambien transparente, no se omite.
 *
 * Los tres tests siguientes fijan lo que el estilo no puede expresar y ningun otro sitio
 * comprueba: que no quede ninguna primitiva SVG sin convertir (`VectorDrawable` solo
 * tiene `<path>`), que cada `pathData` sea legible (Android lo parsea al inflar el vector,
 * no al compilar, asi que un numero de menos tumba la app y ni el build lo ve), y que los
 * pares que deben leerse como el mismo glifo en dos estados compartan geometria.
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
    fun `el set de iconos no esta vacio`() {
        // Si el directorio dejara de existir, el resto de tests comprobarian cero
        // iconos y pasarian en falso.
        assertTrue("no se encuentra ningun icono del set", iconosDelSet().isNotEmpty())
    }

    @Test
    fun `el set cubre los cuatro iconos de la barra y los de acciones`() {
        val nombres = iconosDelSet().map { it.removePrefix("ic_").removeSuffix(".xml") }
        listOf(
            "nav_home", "nav_profiles", "nav_import", "nav_settings",
            "arrow_right", "arrow_back", "add", "star", "star_filled",
            "edit", "delete", "qr", "clock", "download", "upload", "server",
        ).forEach {
            assertTrue("falta el icono $it en el set", it in nombres)
        }
    }

    @Test
    fun `ningun icono del set usa primitivas que VectorDrawable no tiene`() {
        iconosDelSet().forEach { nombre ->
            val contenido = File(DRAWABLES, nombre).readText()
            PRIMITIVAS_SVG.forEach { primitiva ->
                assertFalse(
                    "$nombre: <$primitiva> no existe en VectorDrawable, hay que convertirlo a pathData",
                    contenido.contains("<$primitiva"),
                )
            }
        }
    }

    @Test
    fun `todo pathData del set declara comandos validos y argumentos completos`() {
        iconosDelSet().forEach { nombre ->
            trazados(nombre).forEachIndexed { indice, bloque ->
                val pathData = atributo(bloque, "pathData")
                assertTrue("$nombre: el trazado $indice no declara pathData", pathData.isNotEmpty())
                assertTrue(
                    "$nombre: el trazado $indice tiene un pathData mal formado: \"$pathData\"",
                    pathDataValido(pathData),
                )
            }
        }
    }

    @Test
    fun `los iconos con subpaths negativos declaran el fillType par`() {
        CON_SUBPATHS_NEGATIVOS.forEach { nombre ->
            assertTrue(
                "$nombre: sin android:fillType=\"evenOdd\" los subpaths negativos se rellenan",
                File(DRAWABLES, nombre).readText().contains("android:fillType=\"evenOdd\""),
            )
        }
    }

    @Test
    fun `la estrella vacia y la llena comparten pathData`() {
        val vacia = trazados("ic_star.xml").single()
        val llena = trazados("ic_star_filled.xml").single()
        assertEquals(
            "el par de la estrella debe compartir geometria para leerse como el mismo glifo",
            atributo(llena, "pathData"),
            atributo(vacia, "pathData"),
        )
    }

    @Test
    fun `los cuatro iconos mixtos declaran su trazo`() {
        ICONOS_MIXTOS.forEach { nombre ->
            val delIcono = trazados(nombre)
            assertTrue(
                "$nombre deberia mezclar relleno y trazo, y no declara ningun trazo",
                delIcono.any { atributo(it, "strokeColor").isNotEmpty() },
            )
        }
    }

    /**
     * Comprueba que un `pathData` sea legible. No dibuja nada: revisa la gramatica, que es
     * donde se cuela el error de transcripcion. Android parsea el `pathData` al inflar el
     * vector y no al compilar, asi que un numero de menos tumba la app sin que ni el build
     * ni los tests de estilo lo vean.
     */
    private fun pathDataValido(pathData: String): Boolean {
        if (pathData.isEmpty()) return false
        val tokens = TOKEN.findAll(pathData).map { it.value }.toList()
        if (tokens.isEmpty()) return false
        // Lo que no es separador tenia que casar con algun token: si no, sobra un caracter.
        if (tokens.joinToString("") != pathData.filterNot { it == ' ' || it == ',' }) return false
        // Ningun comando fuera del juego que usa el set.
        if (tokens.any { it.length == 1 && it[0].isLetter() && it[0] !in ARIDADES }) return false

        var indice = 0
        while (indice < tokens.size) {
            val comando = tokens[indice][0]
            val aridad = ARIDADES.getValue(comando)
            indice++
            if (aridad == 0) continue
            val grupo = ArrayList<String>()
            while (indice < tokens.size && !(tokens[indice].length == 1 && tokens[indice][0].isLetter())) {
                grupo.add(tokens[indice])
                indice++
            }
            // Multiplo exacto de la aridad: SVG permite repetir el comando de forma
            // implicita y cinco drawables del set lo usan. Un numero suelto sigue cayendo.
            if (grupo.isEmpty() || grupo.size % aridad != 0) return false
            if (comando.uppercaseChar() == 'A') {
                var k = 3
                while (k + 1 < grupo.size) {
                    if (grupo[k] !in BANDERAS || grupo[k + 1] !in BANDERAS) return false
                    k += 7
                }
            }
        }
        return true
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
        val trazados = PATH.findAll(archivo.readText()).map { it.value }.toList()
        assertTrue("$nombre no dibuja ninguna ruta", trazados.isNotEmpty())
        return trazados
    }

    private fun atributo(bloque: String, nombre: String): String =
        Regex("android:${Regex.escape(nombre)}=\"([^\"]*)\"").find(bloque)?.groupValues?.get(1) ?: ""

    private companion object {
        const val DRAWABLES = "src/main/res/drawable"
        val PATH = Regex("<path\\s[^>]*?/>")

        /** Comando de `pathData` y cuantos numeros consume. */
        val ARIDADES = mapOf(
            'M' to 2, 'L' to 2, 'C' to 6, 'A' to 7, 'V' to 1, 'H' to 1, 'Z' to 0,
            'm' to 2, 'l' to 2, 'c' to 6, 'a' to 7, 'v' to 1, 'h' to 1, 'z' to 0,
        )

        /** Los dos flags de un arco, que solo pueden ser 0 o 1. */
        val BANDERAS = setOf("0", "1")

        /** Una letra de comando o un numero. Si algo no casa, es un caracter inventado. */
        val TOKEN = Regex("[MLCQAVHSTZmlcqavhstz]|[-+]?(?:\\d+\\.?\\d*|\\.\\d+)")

        /** Primitivas SVG que `VectorDrawable` no tiene: hay que pasarlas a `pathData`. */
        val PRIMITIVAS_SVG = listOf("rect", "circle", "line")

        /** Los dos glifos cuyo interior sale de un subpath en negativo. */
        val CON_SUBPATHS_NEGATIVOS = listOf("ic_nav_home.xml", "ic_shield_check.xml")

        /** Los cuatro que mezclan silueta rellena y detalle a trazo. */
        val ICONOS_MIXTOS = listOf(
            "ic_settings_notifications.xml", "ic_nav_profiles.xml",
            "ic_shield_check.xml", "ic_star.xml",
        )
    }
}
