package com.usipipo.vpn.data.geo

import com.usipipo.vpn.data.geo.PingRepository
import com.usipipo.vpn.data.geo.PingRunner
import com.usipipo.vpn.data.geo.ReachabilityChecker
import com.usipipo.vpn.domain.PingResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingRepositoryTest {

    @Test
    fun `parsea time de salida de ping`() = runTest {
        val repo = PingRepository(
            PingRunner {
                "64 bytes from 10.5.0.1: icmp_seq=1 ttl=64 time=23.4 ms\n"
            },
            ReachabilityChecker { _, _ -> false },
        )
        val result = repo.ping("10.5.0.1")

        assertEquals(23L, result.ms)
        assertFalse(result.error)
    }

    @Test
    fun `parsea time menor que 1 ms como 0`() = runTest {
        val repo = PingRepository(
            PingRunner { "64 bytes from 10.5.0.1: icmp_seq=1 ttl=64 time<1 ms\n" },
            ReachabilityChecker { _, _ -> false },
        )
        val result = repo.ping("10.5.0.1")

        assertEquals(0L, result.ms)
        assertFalse(result.error)
    }

    @Test
    fun `ping nativo falla y isReachable responde true ms null sin error`() = runTest {
        val repo = PingRepository(
            PingRunner { null },
            ReachabilityChecker { _, _ -> true },
        )
        val result = repo.ping("10.5.0.1")

        assertNull(result.ms)
        assertFalse(result.error)
    }

    @Test
    fun `ping nativo falla y isReachable responde false error true`() = runTest {
        val repo = PingRepository(
            PingRunner { null },
            ReachabilityChecker { _, _ -> false },
        )
        val result = repo.ping("10.5.0.1")

        assertNull(result.ms)
        assertTrue(result.error)
    }

    @Test
    fun `target vacio o blanco retorna error sin llamar al ping`() = runTest {
        var runnerCalled = false
        val repo = PingRepository(
            PingRunner { runnerCalled = true; null },
            ReachabilityChecker { _, _ -> true },
        )

        assertTrue(repo.ping("").error)
        assertTrue(repo.ping("   ").error)
        assertFalse(runnerCalled)
    }

    @Test
    fun `salida de ping invalida usa fallback isReachable`() = runTest {
        val repo = PingRepository(
            PingRunner { "salida basura sin campo time" },
            ReachabilityChecker { _, _ -> true },
        )
        val result = repo.ping("10.5.0.1")

        assertNull(result.ms)
        assertFalse(result.error)
    }
}
