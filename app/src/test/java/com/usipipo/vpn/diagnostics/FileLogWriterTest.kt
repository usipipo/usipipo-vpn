package com.usipipo.vpn.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileLogWriterTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `guarda las lineas en orden`() {
        val writer = FileLogWriter(tmp.newFolder("logs"))

        writer.append("primera")
        writer.append("segunda")

        assertEquals("primera\nsegunda\n", writer.readAll())
    }

    @Test
    fun `rota al superar el tamano y descarta lo mas antiguo`() {
        val writer = FileLogWriter(tmp.newFolder("logs"), maxBytes = 50)

        (1..20).forEach { writer.append("linea-%02d".format(it)) }

        val all = writer.readAll()
        assertFalse(all.contains("linea-01"))
        assertTrue(all.contains("linea-20"))
        // Nunca pasa de dos archivos de ~maxBytes cada uno.
        assertTrue(writer.sizeBytes() <= 100)
    }

    @Test
    fun `readLastLines devuelve las ultimas en orden`() {
        val writer = FileLogWriter(tmp.newFolder("logs"))
        (1..10).forEach { writer.append("l$it") }

        assertEquals(listOf("l8", "l9", "l10"), writer.readLastLines(3))
    }

    @Test
    fun `sin archivos todo esta vacio`() {
        val writer = FileLogWriter(tmp.newFolder("logs"))

        assertEquals("", writer.readAll())
        assertEquals(emptyList<String>(), writer.readLastLines(5))
        assertEquals(0L, writer.sizeBytes())
    }

    @Test
    fun `clear borra el registro actual y el anterior`() {
        val writer = FileLogWriter(tmp.newFolder("logs"), maxBytes = 30)
        (1..10).forEach { writer.append("linea-%02d".format(it)) }

        writer.clear()

        assertEquals("", writer.readAll())
        assertEquals(0L, writer.sizeBytes())
    }
}
