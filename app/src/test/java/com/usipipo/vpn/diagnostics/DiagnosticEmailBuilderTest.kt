package com.usipipo.vpn.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticEmailBuilderTest {

    private val builder = DiagnosticEmailBuilder

    private fun build(
        appName: String = "uSipipo VPN",
        version: String = "0.4.0",
        manufacturer: String = "Samsung",
        model: String = "SM-A256E",
        androidRelease: String = "16",
        verbose: Boolean = false,
    ) = builder.build(
        appName = appName,
        version = version,
        manufacturer = manufacturer,
        model = model,
        androidRelease = androidRelease,
        verbose = verbose,
    )

    @Test
    fun `asunto y cabecera con datos normales`() {
        val parts = build()

        assertEquals("uSipipo-Diag | v0.4.0 | Samsung SM-A256E | Android 16", parts.subject)

        val expectedHeader = """app: uSipipo VPN
version: 0.4.0
model: Samsung SM-A256E
android: 16
verbose: no
"""
        assertEquals(expectedHeader, parts.header)
    }

    @Test
    fun `el asunto tiene exactamente cuatro campos separados por pipe`() {
        val campos = build().subject.split(" | ")

        assertEquals(4, campos.size)
        assertEquals("uSipipo-Diag", campos[0])
        assertEquals("v0.4.0", campos[1])
        assertEquals("Samsung SM-A256E", campos[2])
        assertEquals("Android 16", campos[3])
    }

    @Test
    fun `cabecera con modo detallado activado dice si`() {
        val parts = build(verbose = true)

        assertTrue(parts.header.contains("verbose: si"))
        assertFalse(parts.header.contains("verbose: no"))
    }

    @Test
    fun `marca vacia usa desconocido en el asunto`() {
        val parts = build(manufacturer = "")

        assertTrue(parts.subject.contains("desconocido SM-A256E"))
    }

    @Test
    fun `modelo vacio usa desconocido en el asunto`() {
        val parts = build(model = "")

        assertTrue(parts.subject.contains("Samsung desconocido"))
    }

    @Test
    fun `marca y modelo vacios colapsan en un solo desconocido`() {
        // El plan aplica en orden: vacío -> desconocido, y luego marca repetida
        // dentro del modelo -> sin duplicar. "desconocido desconocido" sería redundante.
        val parts = build(manufacturer = "", model = "")

        assertEquals("uSipipo-Diag | v0.4.0 | desconocido | Android 16", parts.subject)
        assertFalse(parts.subject.contains("desconocido desconocido"))
    }

    @Test
    fun `marca repetida dentro del modelo no se duplica`() {
        val parts = build(manufacturer = "Samsung", model = "Samsung SM-A256E")

        assertEquals("uSipipo-Diag | v0.4.0 | Samsung SM-A256E | Android 16", parts.subject)
        assertFalse(parts.subject.contains("Samsung Samsung"))
    }

    @Test
    fun `se eliminan saltos de linea del texto del dispositivo`() {
        val parts = build(manufacturer = "Samsung\nGalaxy", model = "SM-A256E")

        assertFalse(parts.subject.contains("\n"))
        assertTrue(parts.subject.contains("Samsung Galaxy SM-A256E"))
    }

    @Test
    fun `se elimina el caracter pipe del texto del dispositivo`() {
        // El separador de campos del asunto es " | ": el pipe se elimina del texto
        // del dispositivo, no del asunto completo.
        val campos = build(manufacturer = "Samsung|", model = "SM-A256E").subject.split(" | ")

        assertEquals(4, campos.size)
        assertEquals("Samsung SM-A256E", campos[2])
    }

    @Test
    fun `cada campo del asunto se recorta a 40 caracteres`() {
        val largo = "A".repeat(60)
        val campos = build(model = largo).subject.split(" | ")

        assertEquals(4, campos.size)
        assertTrue(campos[2].length <= 40)
    }

    @Test
    fun `la version del asunto se recorta a 40 caracteres`() {
        val campos = build(version = "B".repeat(60)).subject.split(" | ")

        assertEquals(4, campos.size)
        assertEquals("B".repeat(40), campos[1].removePrefix("v"))
    }

    @Test
    fun `la version de android del asunto se recorta a 40 caracteres`() {
        val campos = build(androidRelease = "C".repeat(60)).subject.split(" | ")

        assertEquals(4, campos.size)
        assertEquals("Android " + "C".repeat(40), campos[3])
    }

    @Test
    fun `la cabecera contiene solo los cinco campos`() {
        val lineas = build().header.lines().filter { it.isNotBlank() }

        assertEquals(5, lineas.size)
        assertEquals(
            listOf("app", "version", "model", "android", "verbose"),
            lineas.map { it.substringBefore(":") },
        )
        assertEquals("app: uSipipo VPN", lineas[0])
        assertEquals("version: 0.4.0", lineas[1])
        assertEquals("model: Samsung SM-A256E", lineas[2])
        assertEquals("android: 16", lineas[3])
        assertEquals("verbose: no", lineas[4])
    }

    @Test
    fun `la cabecera no contiene campos adicionales`() {
        val parts = build()

        assertFalse(parts.header.contains("manufacturer:"))
        assertFalse(parts.header.contains("abi:"))
        assertFalse(parts.header.contains("versionCode:"))
    }

    @Test
    fun `version vacia se mantiene en el asunto`() {
        val parts = build(version = "")

        assertEquals("uSipipo-Diag | v | Samsung SM-A256E | Android 16", parts.subject)
    }
}
