package com.usipipo.vpn.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nombres de entrada del zip y defensa contra el zip slip. */
class BackupArchiveTest {

    @Test
    fun `la entrada de un perfil lleva su id y el sufijo cifrado`() {
        assertEquals("perfil-1.conf.enc", BackupArchive.entryName("perfil-1"))
    }

    @Test
    fun `el id se recupera del nombre de su entrada`() {
        assertEquals("perfil-1", BackupArchive.configIdFromEntry("perfil-1.conf.enc"))
        assertEquals("abc", BackupArchive.configIdFromEntry("abc.conf.enc"))
    }

    @Test
    fun `un nombre que no es de perfil no devuelve id`() {
        assertNull(BackupArchive.configIdFromEntry(BackupArchive.SETTINGS_ENTRY))
        assertNull(BackupArchive.configIdFromEntry("perfil-1.conf"))
        assertNull(BackupArchive.configIdFromEntry("perfil-1"))
        assertNull(BackupArchive.configIdFromEntry("otra/perfil-1.conf.enc"))
    }

    @Test
    fun `los nombres normales de entrada son seguros`() {
        assertTrue(BackupArchive.isSafeEntryName("perfil-1.conf.enc"))
        assertTrue(BackupArchive.isSafeEntryName(BackupArchive.SETTINGS_ENTRY))
    }

    @Test
    fun `un nombre que se escapa del destino no es seguro`() {
        assertFalse(BackupArchive.isSafeEntryName("../evadir.conf.enc"))
        assertFalse(BackupArchive.isSafeEntryName("/absoluto.conf.enc"))
        assertFalse(BackupArchive.isSafeEntryName("sub/directorio.conf.enc"))
        assertFalse(BackupArchive.isSafeEntryName(".."))
        assertFalse(BackupArchive.isSafeEntryName(""))
    }
}
