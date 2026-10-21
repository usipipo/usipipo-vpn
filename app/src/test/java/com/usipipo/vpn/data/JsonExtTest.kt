package com.usipipo.vpn.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonExtTest {

    @Test
    fun `clave ausente devuelve null`() {
        val json = JSONObject("""{"a":"1"}""")

        assertNull(json.optStringOrNull("countryCode"))
    }

    @Test
    fun `valor null explicito devuelve null`() {
        val json = JSONObject("""{"countryCode":null}""")

        assertNull(json.optStringOrNull("countryCode"))
    }

    @Test
    fun `string vacio devuelve null`() {
        val json = JSONObject("""{"countryCode":""}""")

        assertNull(json.optStringOrNull("countryCode"))
    }

    @Test
    fun `valor valido devuelve el string`() {
        val json = JSONObject("""{"countryCode":"DE"}""")

        assertEquals("DE", json.optStringOrNull("countryCode"))
    }

    @Test
    fun `clave ausente no devuelve el string null`() {
        val json = JSONObject("""{"other":"x"}""")

        // Regresion: optString(key, "") en Android devuelve "null" en vez del fallback.
        assertNull(json.optStringOrNull("countryCode"))
    }
}
