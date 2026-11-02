package com.usipipo.vpn.ui.nav

import com.usipipo.vpn.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainTabTest {

    @Test
    fun `una clave de pestaña válida se convierte en pestaña`() {
        assertEquals(MainTab.Import, MainTab.fromKey("Import"))
    }

    @Test
    fun `la clave de pestaña no depende de las mayúsculas`() {
        assertEquals(MainTab.Import, MainTab.fromKey("import"))
        assertEquals(MainTab.Import, MainTab.fromKey("IMPORT"))
        assertEquals(MainTab.Settings, MainTab.fromKey("diagnostics"))
    }

    @Test
    fun `la clave de la antigua pestaña Configuraciones abre Perfiles`() {
        assertEquals(MainTab.Profiles, MainTab.fromKey("Configs"))
        assertEquals(MainTab.Profiles, MainTab.fromKey("configs"))
    }

    @Test
    fun `la clave de la antigua pestaña Diagnóstico abre Ajustes`() {
        assertEquals(MainTab.Settings, MainTab.fromKey("Diagnostics"))
        assertEquals(MainTab.Settings, MainTab.fromKey("diagnostics"))
    }

    @Test
    fun `una clave desconocida abre Inicio`() {
        assertEquals(MainTab.Home, MainTab.fromKey("desconocida"))
        assertEquals(MainTab.Home, MainTab.fromKey(null))
    }

    @Test
    fun `atrás desde Inicio sale de la app`() {
        assertNull(backTarget(MainTab.Home))
    }

    @Test
    fun `atrás desde cualquier otra pestaña vuelve a Inicio`() {
        listOf(MainTab.Profiles, MainTab.Import, MainTab.Settings).forEach {
            assertEquals(MainTab.Home, backTarget(it))
        }
    }

    @Test
    fun `cada pestaña tiene su propio icono del set propio`() {
        val iconos = MainTab.entries.map { it.iconRes }
        assertEquals(MainTab.entries.size, iconos.toSet().size)
        assertEquals(R.drawable.ic_nav_home, MainTab.Home.iconRes)
        assertEquals(R.drawable.ic_nav_profiles, MainTab.Profiles.iconRes)
        assertEquals(R.drawable.ic_nav_import, MainTab.Import.iconRes)
        assertEquals(R.drawable.ic_nav_settings, MainTab.Settings.iconRes)
    }

    @Test
    fun `cada pestaña tiene su propia etiqueta`() {
        val etiquetas = MainTab.entries.map { it.labelRes }
        assertEquals(MainTab.entries.size, etiquetas.toSet().size)
        assertEquals(R.string.nav_home, MainTab.Home.labelRes)
        assertEquals(R.string.nav_profiles, MainTab.Profiles.labelRes)
        assertEquals(R.string.nav_import, MainTab.Import.labelRes)
        assertEquals(R.string.nav_settings, MainTab.Settings.labelRes)
    }
}
