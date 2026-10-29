package com.usipipo.vpn.quickconnect

import com.usipipo.vpn.tunnel.BackgroundStartDeniedException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTunnelConnectorTest {

    @Test
    fun `arranca el servicio con el configId y confirma la conexion`() = runTest {
        var arrancado: String? = null
        val connector = AndroidTunnelConnector { configId -> arrancado = configId }

        assertTrue(connector.connect("abc-123"))
        assertEquals("abc-123", arrancado)
    }

    @Test
    fun `el sistema deniega el arranque en segundo plano y el conector lo cuenta`() = runTest {
        val connector = AndroidTunnelConnector { throw BackgroundStartDeniedException("denegado") }

        assertFalse(connector.connect("abc-123"))
    }
}
