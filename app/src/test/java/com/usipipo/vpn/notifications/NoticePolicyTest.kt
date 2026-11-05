package com.usipipo.vpn.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reglas de publicación de avisos propios: general, tipo y permiso. */
class NoticePolicyTest {

    @Test
    fun `con todo activo y permiso se publica el aviso`() {
        assertTrue(NoticePolicy.allowed(master = true, type = true, permissionGranted = true))
    }

    @Test
    fun `con el general apagado no se publica ningun aviso`() {
        assertFalse(NoticePolicy.allowed(master = false, type = true, permissionGranted = true))
    }

    @Test
    fun `con el aviso apagado no se publica ese aviso`() {
        assertFalse(NoticePolicy.allowed(master = true, type = false, permissionGranted = true))
    }

    @Test
    fun `sin permiso no se publica nada`() {
        assertFalse(NoticePolicy.allowed(master = true, type = true, permissionGranted = false))
    }

    @Test
    fun `con el general apagado y sin permiso tampoco`() {
        assertFalse(NoticePolicy.allowed(master = false, type = false, permissionGranted = false))
    }
}
