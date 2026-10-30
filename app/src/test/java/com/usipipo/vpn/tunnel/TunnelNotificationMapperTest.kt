package com.usipipo.vpn.tunnel

import com.usipipo.vpn.R
import com.usipipo.vpn.domain.TunnelState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TunnelNotificationMapperTest {

    @Test
    fun `conectando mapea a texto de conectando y notificacion viva`() {
        val n = TunnelNotificationMapper.map(TunnelState.Connecting("a"))
        assertEquals(R.string.widget_status_connecting, n.textRes)
        assertTrue(n.ongoing)
    }

    @Test
    fun `conectado mapea a texto de activo y notificacion viva`() {
        val n = TunnelNotificationMapper.map(TunnelState.Connected("a"))
        assertEquals(R.string.widget_status_active, n.textRes)
        assertTrue(n.ongoing)
    }

    @Test
    fun `sin respuesta mantiene la notificacion viva`() {
        val n = TunnelNotificationMapper.map(TunnelState.NoResponse("a"))
        assertEquals(R.string.widget_status_no_response, n.textRes)
        assertTrue(n.ongoing)
    }

    @Test
    fun `desconectado y fallo retiran la notificacion`() {
        assertFalse(TunnelNotificationMapper.map(TunnelState.Disconnected).ongoing)
        assertFalse(TunnelNotificationMapper.map(TunnelState.Failed(null)).ongoing)
    }

    @Test
    fun `desconectado y fallo usan su propio texto`() {
        assertEquals(
            R.string.widget_status_disconnected,
            TunnelNotificationMapper.map(TunnelState.Disconnected).textRes,
        )
        assertEquals(
            R.string.widget_status_failed,
            TunnelNotificationMapper.map(TunnelState.Failed("a")).textRes,
        )
    }

    @Test
    fun `el titulo es siempre el nombre de la app`() {
        listOf(
            TunnelState.Disconnected,
            TunnelState.Connecting("a"),
            TunnelState.Connected("a"),
            TunnelState.NoResponse("a"),
            TunnelState.Failed(null),
        ).forEach { state ->
            assertEquals(R.string.app_name, TunnelNotificationMapper.map(state).titleRes)
        }
    }
}
