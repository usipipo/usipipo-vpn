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
    fun `marca y modelo vacios usan desconocido`() {
        val parts = build(manufacturer = "", model = "")

        assertTrue(parts.subject.contains("desconocido desconocido"))
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
        val parts = build(manufacturer = "Samsung|", model = "SM-A256E")

        assertFalse(parts.subject.contains("|"))
    }

    @Test
    fun `cada campo del asunto se recorta a 40 caracteres`() {
        val largo = "A".repeat(60)
        val parts = build(model = largo)

        val devicePart = parts.subject.substringAfter("v0.4.0 | ").substringBefore(" | Android")
        assertTrue(devicePart.length <= 40)
    }

    @Test
    fun `la cabecera contiene solo los cinco campos`() {
        val parts = build()

        val lineas = parts.header.lines()
        assertEquals(5, lineas.size)
        assertTrue(lineas.first { it.startsWith("app:") }.startsWith("app: uSipipo VPN"))
        assertTrue(lineas.first { it.startsWith("version:") }.startsWith("version: 0.4.0"))
        assertTrue(lineas.first { it.startsWith("model:") }.startsWith("model: Samsung SM-A256E"))
        assertTrue(lineas.first { it.startsWith("android:") }.startsWith("android: 16"))
        assertTrue(lineas.first { it.startsWith("verbose:") }.startsWith("verbose: no"))
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

        assertTrue(parts.subject.startsWith("uSipipo-Diag | v | "))
    }
}
