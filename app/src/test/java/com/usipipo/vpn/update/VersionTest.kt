package com.usipipo.vpn.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {

    @Test
    fun `parsea una version completa`() {
        assertEquals(Version(0, 1, 0), Version.parse("0.1.0"))
    }

    @Test
    fun `parsea el prefijo v del tag`() {
        assertEquals(Version(0, 2, 3), Version.parse("v0.2.3"))
    }

    @Test
    fun `parsea con espacios alrededor`() {
        assertEquals(Version(1, 2, 3), Version.parse("  v1.2.3  "))
        assertEquals(Version(1, 2, 3), Version.parse("1.2.3\t"))
    }

    @Test
    fun `componentes ausentes equivalen a cero`() {
        assertEquals(Version(1, 2, 0), Version.parse("1.2"))
        assertEquals(Version(1, 2, 0), Version.parse("v1.2"))
        assertEquals(Version(1, 0, 0), Version.parse("1"))
    }

    @Test
    fun `no parsea lo que no es una version`() {
        assertNull(Version.parse(null))
        assertNull(Version.parse(""))
        assertNull(Version.parse("   "))
        assertNull(Version.parse("v"))
        assertNull(Version.parse("hola"))
        assertNull(Version.parse("a.b.c"))
        assertNull(Version.parse("1.2.3.4"))
        assertNull(Version.parse("1..2"))
        assertNull(Version.parse("1.2-beta"))
        assertNull(Version.parse("1 . 2"))
        assertNull(Version.parse("1.2 3"))
    }

    @Test
    fun `compara por componentes numericos`() {
        assertTrue(Version(0, 2, 0) > Version(0, 1, 9))
        assertTrue(Version(1, 0, 0) > Version(0, 9, 9))
        assertTrue(Version(0, 1, 2) > Version(0, 1, 1))
        assertTrue(Version(0, 1, 1) < Version(0, 1, 2))
        assertTrue(Version(0, 10, 0) > Version(0, 9, 9))
    }

    @Test
    fun `versiones iguales comparan igual`() {
        assertEquals(0, Version(1, 2, 3).compareTo(Version(1, 2, 3)))
        assertEquals(Version(1, 2, 3), Version.parse("v1.2.3"))
    }

    @Test
    fun `toString devuelve el formato completo`() {
        assertEquals("1.2.3", Version(1, 2, 3).toString())
    }
}
