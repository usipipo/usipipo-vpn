package com.usipipo.vpn.ui.settings.storage

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tamaños legibles para el resumen de almacenamiento. */
class StorageFormatTest {

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

    @Test
    fun `el total es la suma de las partes`() {
        val total = StorageFormat.total(
            profilesBytes = 1_024,
            logBytes = 2_048,
            geoCacheBytes = 0,
            pingBytes = 512,
            tempBytes = 0,
        )

        assertEquals(3_584, total)
    }

    @Test
    fun `un tamano negativo se muestra como cero`() {
        assertEquals("0 B", StorageFormat.readable(-1))
    }
}
