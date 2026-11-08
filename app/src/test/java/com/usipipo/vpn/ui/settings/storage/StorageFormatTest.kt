package com.usipipo.vpn.ui.settings.storage

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

private val ESPANOL = Locale.forLanguageTag("es-ES")

/** Tamaños legibles para el resumen de almacenamiento. */
class StorageFormatTest {

    @Test
    fun `los bytes se muestran tal cual`() {
        assertEquals("0 B", StorageFormat.readable(0, ESPANOL))
        assertEquals("512 B", StorageFormat.readable(512, ESPANOL))
    }

    @Test
    fun `los kilobytes se muestran con un decimal`() {
        assertEquals("1,0 KB", StorageFormat.readable(1_024, ESPANOL))
        assertEquals("1,5 KB", StorageFormat.readable(1_536, ESPANOL))
    }

    @Test
    fun `los megabytes se muestran con un decimal`() {
        assertEquals("1,0 MB", StorageFormat.readable(1_048_576, ESPANOL))
        assertEquals("2,5 MB", StorageFormat.readable(2_621_440, ESPANOL))
    }

    @Test
    fun `los gigabytes se muestran con un decimal`() {
        assertEquals("1,0 GB", StorageFormat.readable(1_073_741_824, ESPANOL))
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
        assertEquals("0 B", StorageFormat.readable(-1, ESPANOL))
    }

    /** El separador lo decide la locale: en inglés el mismo tamaño lleva punto. */
    @Test
    fun `la locale decide el separador decimal`() {
        assertEquals("1.0 KB", StorageFormat.readable(1_024, Locale.US))
    }
}
