package com.usipipo.vpn.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportBuilderTest {

    private val info = listOf("Aplicación" to "0.1.0 (1)", "Android" to "14 (API 34)")
    private val key = "K".repeat(43) + "="

    private fun build(
        log: String = "2026-10-11 10:00:00.000 I/Tunel: Estado: Conectado",
        logcat: String? = null,
        verbose: Boolean = false,
        maxChars: Int = ReportBuilder.DEFAULT_MAX_CHARS,
    ) = ReportBuilder.build("2026-10-11 10:05:00", info, log, logcat, verbose, maxChars)

    @Test
    fun `incluye informacion y registro`() {
        val report = build()

        assertTrue(report.contains("--- Información ---"))
        assertTrue(report.contains("Aplicación: 0.1.0 (1)"))
        assertTrue(report.contains("--- Registro de la app ---"))
        assertTrue(report.contains("I/Tunel: Estado: Conectado"))
        assertTrue(report.contains("Modo detallado: desactivado"))
    }

    @Test
    fun `sin modo detallado no hay seccion de logcat`() {
        assertFalse(build(logcat = "algo").contains("logcat"))
    }

    @Test
    fun `en modo detallado incluye logcat o avisa de que no esta disponible`() {
        val conLogcat = build(logcat = "linea de logcat", verbose = true)
        val sinLogcat = build(logcat = null, verbose = true)

        assertTrue(conLogcat.contains("--- logcat (proceso de la app) ---"))
        assertTrue(conLogcat.contains("linea de logcat"))
        assertTrue(sinLogcat.contains("(no disponible)"))
    }

    @Test
    fun `tapa claves en todas las secciones`() {
        val report = build(
            log = "peer $key",
            logcat = "PrivateKey = $key",
            verbose = true,
        )

        assertFalse(report.contains(key))
    }

    @Test
    fun `un registro vacio se indica`() {
        assertTrue(build(log = "").contains("(vacío)"))
    }

    @Test
    fun `si se pasa del limite conserva lo mas reciente y avisa del recorte`() {
        val log = (1..200).joinToString("\n") { "linea-%03d ".format(it) + "x".repeat(40) }

        val report = build(log = log, maxChars = 1500)

        assertTrue(report.contains("linea-200"))
        assertFalse(report.contains("linea-001"))
        assertTrue(report.contains("recortado"))
    }
}
