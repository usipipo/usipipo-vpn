package com.usipipo.vpn.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
