package com.usipipo.vpn.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EncryptedFileConfigStoreRenameTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `el nuevo nombre se conserva al reabrir el almacen`() = runTest {
        val dir = tmp.newFolder("configs")
        val first = EncryptedFileConfigStore(dir, IdentityCrypto())
        val saved = first.add("Casa", "conf").getOrThrow()

        val result = first.rename(saved.id, "  Oficina ")

        assertEquals("Oficina", result.getOrThrow().name)
        val second = EncryptedFileConfigStore(dir, IdentityCrypto())
        assertEquals("Oficina", second.configs.value.single().name)
    }

    @Test
    fun `usar el nombre de otro perfil falla por duplicado sin distinguir mayusculas`() = runTest {
        val store = EncryptedFileConfigStore(tmp.newFolder("configs"), IdentityCrypto())
        val casa = store.add("Casa", "conf").getOrThrow()
        store.add("Trabajo", "conf").getOrThrow()

        val result = store.rename(casa.id, "trabajo")

        assertTrue(result.exceptionOrNull() is DuplicateNameException)
        assertEquals("Casa", store.configs.value.first { it.id == casa.id }.name)
    }

    @Test
    fun `cambiar solo las mayusculas del propio nombre es valido`() = runTest {
        val store = EncryptedFileConfigStore(tmp.newFolder("configs"), IdentityCrypto())
        val casa = store.add("Casa", "conf").getOrThrow()

        assertEquals("CASA", store.rename(casa.id, "CASA").getOrThrow().name)
    }

    @Test
    fun `un nombre vacio falla`() = runTest {
        val store = EncryptedFileConfigStore(tmp.newFolder("configs"), IdentityCrypto())
        val casa = store.add("Casa", "conf").getOrThrow()

        assertTrue(store.rename(casa.id, "   ").exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `un id desconocido falla`() = runTest {
        val store = EncryptedFileConfigStore(tmp.newFolder("configs"), IdentityCrypto())

        assertTrue(store.rename("zzz", "Nuevo").isFailure)
    }
}
