package com.usipipo.vpn.data.geo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IpValidatorTest {

    @Test
    fun `una ipv4 completa es valida`() {
        assertTrue(IpValidator.isValid("8.8.8.8"))
        assertTrue(IpValidator.isValid("165.140.1.1"))
        assertTrue(IpValidator.isValid("0.0.0.0"))
        assertTrue(IpValidator.isValid("255.255.255.255"))
    }

    @Test
    fun `una ipv4 con un octeto fuera de rango es invalida`() {
        assertFalse(IpValidator.isValid("256.1.1.1"))
        assertFalse(IpValidator.isValid("1.1.1.300"))
    }

    @Test
    fun `una ipv4 cortada es invalida`() {
        assertFalse(IpValidator.isValid("165.140"))
        assertFalse(IpValidator.isValid("1.2.3"))
    }

    @Test
    fun `una ipv4 con ceros a la izquierda es invalida`() {
        assertFalse(IpValidator.isValid("01.2.3.4"))
    }

    @Test
    fun `una ipv6 completa es valida`() {
        assertTrue(IpValidator.isValid("2001:db8::1"))
        assertTrue(IpValidator.isValid("::1"))
        assertTrue(IpValidator.isValid("fe80::1"))
        assertTrue(IpValidator.isValid("2607:f2d8:6063:13d::d8ab"))
        assertTrue(IpValidator.isValid("2001:0db8:0000:0000:0000:ff00:0042:8329"))
    }

    @Test
    fun `una ipv6 con zona o con cidr es invalida`() {
        assertFalse(IpValidator.isValid("fe80::1%eth0"))
        assertFalse(IpValidator.isValid("2001:db8::1/64"))
    }

    @Test
    fun `una ipv6 incompleta es invalida`() {
        assertFalse(IpValidator.isValid("2001:db8:::1"))
        assertFalse(IpValidator.isValid("2001:db8:1:2:3:4:5:6:7"))
    }

    @Test
    fun `una ip con puerto es invalida`() {
        assertFalse(IpValidator.isValid("1.2.3.4:51820"))
    }

    @Test
    fun `una ip con corchetes es invalida`() {
        assertFalse(IpValidator.isValid("[1.2.3.4]"))
        assertFalse(IpValidator.isValid("[2001:db8::1]"))
    }

    @Test
    fun `un dominio es invalido`() {
        assertFalse(IpValidator.isValid("example.com"))
        assertFalse(IpValidator.isValid("vpn.midominio.org"))
        assertFalse(IpValidator.isValid("example.com:51820"))
    }

    @Test
    fun `una cadena vacia o con espacios es invalida`() {
        assertFalse(IpValidator.isValid(""))
        assertFalse(IpValidator.isValid("   "))
        assertFalse(IpValidator.isValid("1.2.3.4 "))
    }

    @Test
    fun `una ip valida no tiene motivo de rechazo`() {
        assertNull(IpValidator.invalidReason("8.8.8.8"))
    }

    @Test
    fun `una ip invalida explica por que se rechaza`() {
        assertNotNull(IpValidator.invalidReason("165.140"))
        assertNotNull(IpValidator.invalidReason("1.2.3.4:51820"))
        assertNotNull(IpValidator.invalidReason("example.com"))
    }
}
