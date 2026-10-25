package com.usipipo.vpn.widget

import com.usipipo.vpn.R
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.quickconnect.QuickConnectFailure
import com.usipipo.vpn.quickconnect.QuickConnectState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStateMapperTest {

    private val casa = SavedConfig("a", "Casa")

    @Test
    fun `desconectado muestra power y el fondo pastel`() {
        val p = WidgetStateMapper.map(QuickConnectState(selectedConfig = casa))

        assertEquals(WidgetStatus.Disconnected, p.status)
        assertEquals(R.string.widget_status_disconnected, p.statusText)
        assertEquals(R.drawable.widget_bg_disconnected, p.background)
        assertEquals(R.drawable.ic_widget_power, p.actionIcon)
        assertEquals(R.drawable.widget_button_disconnected, p.buttonBackground)
        assertFalse(p.showProgress)
    }

    @Test
    fun `conectando muestra el progreso y no el escudo`() {
        val p = WidgetStateMapper.map(
            QuickConnectState(tunnel = TunnelState.Connecting("a"), selectedConfig = casa),
        )

        assertEquals(WidgetStatus.Connecting, p.status)
        assertTrue(p.showProgress)
        assertEquals(R.drawable.ic_widget_power, p.actionIcon)
    }

    @Test
    fun `conectado muestra el escudo y el fondo degradado`() {
        val p = WidgetStateMapper.map(
            QuickConnectState(tunnel = TunnelState.Connected("a"), selectedConfig = casa),
        )

        assertEquals(WidgetStatus.Connected, p.status)
        assertEquals(R.string.widget_status_active, p.statusText)
        assertEquals(R.drawable.widget_bg_connected, p.background)
        assertEquals(R.drawable.ic_widget_shield_check, p.actionIcon)
        assertEquals(R.drawable.widget_button_connected, p.buttonBackground)
    }

    @Test
    fun `sin respuesta y fallo usan el fondo rosado y el anillo ambar`() {
        listOf(TunnelState.NoResponse("a"), TunnelState.Failed("a")).forEach { tunnel ->
            val p = WidgetStateMapper.map(QuickConnectState(tunnel = tunnel, selectedConfig = casa))
            assertEquals(R.drawable.widget_bg_failed, p.background)
            assertEquals(R.drawable.ic_widget_power_failed, p.actionIcon)
        }
    }

    @Test
    fun `un fallo local se presenta como fallo`() {
        val p = WidgetStateMapper.map(
            QuickConnectState(
                selectedConfig = casa,
                failure = QuickConnectFailure.ConfigUnreadable("a"),
            ),
        )

        assertEquals(WidgetStatus.Failed, p.status)
        assertEquals(R.string.widget_status_failed, p.statusText)
    }

    @Test
    fun `sin perfiles presenta importar perfil`() {
        val p = WidgetStateMapper.map(QuickConnectState())

        assertEquals(WidgetStatus.NoProfile, p.status)
        assertEquals(R.string.widget_import_profile, p.statusText)
        assertEquals(R.drawable.ic_widget_add, p.actionIcon)
        assertEquals("", p.profileName)
        assertEquals("", p.countryLabel)
    }

    @Test
    fun `config de Alemania genera etiqueta de pais con bandera`() {
        val p = WidgetStateMapper.map(
            QuickConnectState(
                selectedConfig = SavedConfig(
                    "de",
                    "Germany",
                    countryCode = "DE",
                    countryName = "Germany",
                ),
            ),
        )

        assertEquals("\uD83C\uDDE9\uD83C\uDDEA Germany", p.countryLabel)
    }

    @Test
    fun `sin cache no muestra etiqueta de pais`() {
        val p = WidgetStateMapper.map(
            QuickConnectState(selectedConfig = SavedConfig("sin", "Sin pais")),
        )

        assertEquals("", p.countryLabel)
    }

    @Test
    fun `pais con codigo pero sin nombre no muestra etiqueta`() {
        val p = WidgetStateMapper.map(
            QuickConnectState(
                selectedConfig = SavedConfig("de", "Alemania", countryCode = "DE"),
            ),
        )

        assertEquals("", p.countryLabel)
    }
}
