package com.usipipo.vpn.ui.settings.storage

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tamaños legibles para el resumen de almacenamiento.
 *
 * La clase entera corre con la locale del dispositivo en inglés a propósito: si
 * alguna aserción depende de la máquina que ejecuta los tests, aquí se ve.
 */
class StorageFormatTest {

    private val localeAnterior = Locale.getDefault()

    @Before
    fun dispositivoEnIngles() {
        Locale.setDefault(Locale.US)
    }

    @After
    fun restaurarLocale() {
        Locale.setDefault(localeAnterior)
    }

    @Test
    fun `los bytes se muestran tal cual`() {
        assertEquals("0 B", StorageFormat.readable(0))
        assertEquals("512 B", StorageFormat.readable(512))
    }

    @Test
    fun `los kilobytes se muestran con un decimal`() {
        assertEquals("1,0 KB", StorageFormat.readable(1_024))
        assertEquals("1,5 KB", StorageFormat.readable(1_536))
    }

    @Test
    fun `los megabytes se muestran con un decimal`() {
        assertEquals("1,0 MB", StorageFormat.readable(1_048_576))
        assertEquals("2,5 MB", StorageFormat.readable(2_621_440))
    }

    @Test
    fun `los gigabytes se muestran con un decimal`() {
        assertEquals("1,0 GB", StorageFormat.readable(1_073_741_824))
    }

    /** El separador decimal es español aunque el dispositivo esté en inglés. */
    @Test
    fun `el separador decimal es el espanol aunque el dispositivo este en otro idioma`() {
        assertEquals("2,5 MB", StorageFormat.readable(2_621_440))
    }

    @Test
    fun `el total es la suma de las partes`() {
        val total = StorageFormat.total(
            profilesBytes = 1_024,
            logBytes = 2_048,
            tempBytes = 512,
        )

        assertEquals(3_584, total)
    }

    @Test
    fun `un tamano negativo se muestra como cero`() {
        assertEquals("0 B", StorageFormat.readable(-1))
    }
}
