package com.usipipo.vpn.ui.configs

import com.usipipo.vpn.domain.SavedConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigsFilterTest {

    private val casa = SavedConfig("a", "Casa")
    private val trabajo = SavedConfig("b", "Trabajo", favorite = true)

    @Test
    fun `Mis perfiles muestra todos`() {
        assertEquals(listOf(casa, trabajo), listOf(casa, trabajo).forTab(ConfigsTab.Mine))
    }

    @Test
    fun `Favoritos muestra solo los marcados`() {
        assertEquals(listOf(trabajo), listOf(casa, trabajo).forTab(ConfigsTab.Favorites))
    }

    @Test
    fun `Favoritos sin ninguno marcado queda vacio`() {
        assertEquals(emptyList<SavedConfig>(), listOf(casa).forTab(ConfigsTab.Favorites))
    }
}
