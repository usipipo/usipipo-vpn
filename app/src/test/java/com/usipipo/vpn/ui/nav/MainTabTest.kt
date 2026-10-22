package com.usipipo.vpn.ui.nav

import com.usipipo.vpn.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabTest {

    @Test
    fun `atras desde Inicio sale de la app`() {
        assertNull(backTarget(MainTab.Home))
    }

    @Test
    fun `atras desde cualquier otra pestana vuelve a Inicio`() {
        listOf(MainTab.Configs, MainTab.Import, MainTab.Diagnostics).forEach {
            assertEquals(MainTab.Home, backTarget(it))
        }
    }

    @Test
    fun `cada pestana tiene su propio icono del set propio`() {
        val iconos = MainTab.entries.map { it.iconRes }
        assertEquals(MainTab.entries.size, iconos.toSet().size)
        assertTrue("alguna pestana apunta a un recurso inexistente", iconos.all { it != 0 })
    }

    @Test
    fun `cada pestana tiene su propia etiqueta`() {
        val etiquetas = MainTab.entries.map { it.labelRes }
        assertEquals(MainTab.entries.size, etiquetas.toSet().size)
        assertEquals(R.string.nav_home, MainTab.Home.labelRes)
        assertEquals(R.string.nav_configs, MainTab.Configs.labelRes)
        assertEquals(R.string.nav_import, MainTab.Import.labelRes)
        assertEquals(R.string.nav_diagnostics, MainTab.Diagnostics.labelRes)
    }
}
