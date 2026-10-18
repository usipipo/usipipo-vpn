package com.usipipo.vpn.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EncryptedFileConfigStoreFavoritesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `el favorito se conserva al reabrir el almacen`() = runTest {
        val dir = tmp.newFolder("configs")
        val first = EncryptedFileConfigStore(dir, IdentityCrypto())
        val saved = first.add("Casa", "conf").getOrThrow()

        first.setFavorite(saved.id, true)

        val second = EncryptedFileConfigStore(dir, IdentityCrypto())
        assertTrue(second.configs.value.single().favorite)
    }

    @Test
    fun `un indice antiguo sin favorito carga como no favorito`() {
        val dir = tmp.newFolder("configs")
        File(dir, "abc.enc").writeBytes(byteArrayOf(1))
        File(dir, "index.json").writeText(
            """{"configs":[{"id":"abc","name":"Casa"}],"lastUsedId":"abc"}""",
        )

        val store = EncryptedFileConfigStore(dir, IdentityCrypto())

        assertEquals("Casa", store.configs.value.single().name)
        assertFalse(store.configs.value.single().favorite)
    }
}
