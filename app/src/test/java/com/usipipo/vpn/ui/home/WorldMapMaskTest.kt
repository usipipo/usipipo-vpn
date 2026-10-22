package com.usipipo.vpn.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldMapMaskTest {

    @Test
    fun `la mascara tiene el tamano previsto`() {
        assertEquals(36, MAP_ROWS)
        assertEquals(72, MAP_COLUMNS)
        val filas = mapMaskRows()
        assertEquals(MAP_ROWS, filas.size)
        filas.forEach { fila ->
            assertEquals(MAP_COLUMNS / 4, fila.length)
            assertTrue("fila no hexadecimal: $fila", fila.all { it in "0123456789ABCDEF" })
        }
    }

    @Test
    fun `la mascara contiene tierra y mar`() {
        var tierra = 0
        var mar = 0
        for (row in 0 until MAP_ROWS) {
            for (column in 0 until MAP_COLUMNS) {
                if (isLand(column, row)) tierra++ else mar++
            }
        }
        assertTrue("no hay tierra", tierra > 0)
        assertTrue("no hay mar", mar > 0)
        assertTrue("la mascara esta vacia o llena", tierra in 400..1200)
    }

    @Test
    fun `las celdas fuera de la mascara se resuelven como mar`() {
        assertTrue(!isLand(-1, 0))
        assertTrue(!isLand(0, -1))
        assertTrue(!isLand(MAP_COLUMNS, 0))
        assertTrue(!isLand(0, MAP_ROWS))
    }
}
