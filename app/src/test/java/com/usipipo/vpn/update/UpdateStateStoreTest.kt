package com.usipipo.vpn.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateStateStoreTest {

    /** Misma semántica que PrefsUpdateStateStore, sin depender de Android. */
    private class InMemoryStore : UpdateStateStore {
        private var skipped: String? = null
        private var checkedAt: Long = 0L

        override var skippedVersion: String?
            get() = skipped?.takeIf { it.isNotBlank() }
            set(value) {
                skipped = value
            }

        override var lastCheckAt: Long
            get() = checkedAt
            set(value) {
                checkedAt = value
            }

        override fun markSkipped(version: String) {
            skippedVersion = version
        }

        override fun markChecked(at: Long) {
            lastCheckAt = at
        }

        override fun clear() {
            skipped = null
            checkedAt = 0L
        }
    }

    private val store = InMemoryStore()

    @Test
    fun `empieza sin version pospuesta ni comprobacion previa`() {
        assertNull(store.skippedVersion)
        assertEquals(0L, store.lastCheckAt)
    }

    @Test
    fun `guarda la version pospuesta`() {
        store.markSkipped("0.2.0")

        assertEquals("0.2.0", store.skippedVersion)
    }

    @Test
    fun `guarda la marca de la ultima comprobacion`() {
        store.markChecked(1_700_000_000_000L)

        assertEquals(1_700_000_000_000L, store.lastCheckAt)
    }

    @Test
    fun `una version pospuesta en blanco se lee como ausente`() {
        store.skippedVersion = "   "

        assertNull(store.skippedVersion)
    }

    @Test
    fun `se puede escribir directamente`() {
        store.skippedVersion = "1.0.0"
        store.lastCheckAt = 42L

        assertEquals("1.0.0", store.skippedVersion)
        assertEquals(42L, store.lastCheckAt)
    }

    @Test
    fun `clear borra pospuesta y marca`() {
        store.markSkipped("0.2.0")
        store.markChecked(7L)

        store.clear()

        assertNull(store.skippedVersion)
        assertEquals(0L, store.lastCheckAt)
    }
}
