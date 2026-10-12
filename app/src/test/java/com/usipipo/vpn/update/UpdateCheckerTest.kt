package com.usipipo.vpn.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    private class FakeSource(private val release: ReleaseInfo? = null) : ReleaseSource {
        var calls: Int = 0
            private set

        override suspend fun latest(): ReleaseInfo? {
            calls++
            return release
        }
    }

    /** Mismo contrato que PrefsUpdateStateStore, sin Android. */
    private class FakeStore(
        skipped: String? = null,
        checkAt: Long = 0L,
    ) : UpdateStateStore {
        override var skippedVersion: String? = skipped

        override var lastCheckAt: Long = checkAt
            set(value) {
                field = value
                lastCheckWrites += value
            }

        /** Las marcas que el checker ha escrito, en orden. */
        val lastCheckWrites = mutableListOf<Long>()
    }

    private val ahora = 1_700_000_000_000L
    private val nueva = ReleaseInfo(Version(0, 2, 0), "https://example.test/v0.2.0", "0.2.0")

    private fun checker(
        source: ReleaseSource = FakeSource(nueva),
        store: UpdateStateStore = FakeStore(),
        clock: () -> Long = { ahora },
    ) = UpdateChecker(source, store, clock)

    @Test
    fun `hay actualizacion cuando la remota es mas nueva`() = runTest {
        val store = FakeStore()

        val result = checker(store = store).check(currentVersion = "0.1.0")

        assertEquals(UpdateStatus.UpdateAvailable(nueva), result)
        assertEquals(listOf(ahora), store.lastCheckWrites)
    }

    @Test
    fun `sin actualizacion cuando las versiones son iguales`() = runTest {
        val source = FakeSource(ReleaseInfo(Version(0, 1, 0), "https://example.test/0.1.0", null))

        assertEquals(UpdateStatus.UpToDate, checker(source = source).check(currentVersion = "0.1.0"))
    }

    @Test
    fun `sin actualizacion cuando la instalada es mas nueva`() = runTest {
        val source = FakeSource(ReleaseInfo(Version(0, 0, 9), "https://example.test/0.0.9", null))

        assertEquals(UpdateStatus.UpToDate, checker(source = source).check(currentVersion = "0.1.0"))
    }

    @Test
    fun `la comparacion es numerica y no lexicografica`() = runTest {
        val remota = ReleaseInfo(Version(0, 10, 0), "https://example.test/0.10.0", null)
        val source = FakeSource(remota)

        assertEquals(UpdateStatus.UpdateAvailable(remota), checker(source = source).check(currentVersion = "0.9.0"))
    }

    @Test
    fun `una version pospuesta no vuelve a avisar`() = runTest {
        val store = FakeStore(skipped = "0.2.0")

        assertEquals(UpdateStatus.Postponed, checker(store = store).check(currentVersion = "0.1.0"))
    }

    @Test
    fun `una version superior a la pospuesta si avisa`() = runTest {
        val store = FakeStore(skipped = "0.1.5")
        val source = FakeSource(nueva)

        assertEquals(UpdateStatus.UpdateAvailable(nueva), checker(source = source, store = store).check(currentVersion = "0.1.0"))
    }

    @Test
    fun `el throttle de 24 horas corta la comprobacion`() = runTest {
        val source = FakeSource(nueva)
        val store = FakeStore(checkAt = ahora - 60 * 60 * 1000L)

        assertEquals(UpdateStatus.SkippedThrottled, checker(source = source, store = store).check(currentVersion = "0.1.0"))
        assertEquals(0, source.calls)
        assertTrue(store.lastCheckWrites.isEmpty())
    }

    @Test
    fun `pasado el throttle se vuelve a comprobar`() = runTest {
        val source = FakeSource(nueva)
        val store = FakeStore(checkAt = ahora - 25 * 60 * 60 * 1000L)

        assertEquals(UpdateStatus.UpdateAvailable(nueva), checker(source = source, store = store).check(currentVersion = "0.1.0"))
        assertEquals(1, source.calls)
    }

    @Test
    fun `force ignora el throttle y refresca la marca`() = runTest {
        val source = FakeSource(nueva)
        val store = FakeStore(checkAt = ahora)

        val result = checker(source = source, store = store).check(currentVersion = "0.1.0", force = true)

        assertEquals(UpdateStatus.UpdateAvailable(nueva), result)
        assertEquals(1, source.calls)
        assertEquals(listOf(ahora), store.lastCheckWrites)
    }

    @Test
    fun `la primera comprobacion nunca esta frenada`() = runTest {
        val store = FakeStore(checkAt = 0L)

        assertEquals(UpdateStatus.UpdateAvailable(nueva), checker(store = store).check(currentVersion = "0.1.0"))
    }

    @Test
    fun `si la fuente no devuelve release es un fallo`() = runTest {
        val source = FakeSource(null)
        val store = FakeStore()

        assertEquals(UpdateStatus.Failed, checker(source = source, store = store).check(currentVersion = "0.1.0"))
        assertTrue(store.lastCheckWrites.isEmpty())
    }

    @Test
    fun `si la fuente lanza excepcion es un fallo`() = runTest {
        val source = object : ReleaseSource {
            override suspend fun latest(): ReleaseInfo? = error("sin red")
        }

        assertEquals(UpdateStatus.Failed, checker(source = source).check(currentVersion = "0.1.0"))
    }

    @Test
    fun `una version instalada ilegible descarta la comprobacion`() = runTest {
        val source = FakeSource(nueva)

        val result = checker(source = source).check(currentVersion = "version desconocida")

        assertEquals(UpdateStatus.Failed, result)
        assertEquals(0, source.calls)
    }

    @Test
    fun `version instalada vacia`() = runTest {
        val source = FakeSource(nueva)

        assertEquals(UpdateStatus.Failed, checker(source = source).check(currentVersion = ""))
        assertEquals(0, source.calls)
    }
}
