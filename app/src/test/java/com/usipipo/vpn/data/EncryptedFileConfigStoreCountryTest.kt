package com.usipipo.vpn.data

import com.usipipo.vpn.domain.CountryInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EncryptedFileConfigStoreCountryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `guarda y lee campos de pais de un perfil`() = runTest {
        val dir = tmp.newFolder("configs")
        val store = EncryptedFileConfigStore(dir, IdentityCrypto())
        val saved = store.add("Casa", "conf").getOrThrow()

        store.updateCountry(saved.id, CountryInfo("1.2.3.4", "DE", "Germany", 1000L, error = false))

        val cfg = store.configs.value.single { it.id == saved.id }
        assertEquals("DE", cfg.countryCode)
        assertEquals("Germany", cfg.countryName)
        assertEquals("1.2.3.4", cfg.countryIp)
        assertEquals(1000L, cfg.countryFetchedAt)
    }

    @Test
    fun `un indice antiguo sin campos de pais carga como null`() {
        val dir = tmp.newFolder("configs")
        File(dir, "abc.enc").writeBytes(byteArrayOf(1))
        File(dir, "index.json").writeText(
            """{"configs":[{"id":"abc","name":"Casa"}],"lastUsedId":"abc"}""",
        )

        val store = EncryptedFileConfigStore(dir, IdentityCrypto())

        val cfg = store.configs.value.single()
        assertNull(cfg.countryCode)
        assertNull(cfg.countryName)
        assertNull(cfg.countryIp)
        assertNull(cfg.countryFetchedAt)
    }

    @Test
    fun `updateCountry ignora id que no existe`() = runTest {
        val dir = tmp.newFolder("configs")
        val store = EncryptedFileConfigStore(dir, IdentityCrypto())

        store.updateCountry("no-existe", CountryInfo("1.2.3.4", "DE", "Germany", 1000L, error = false))

        assertEquals(0, store.configs.value.size)
    }
}
