package com.usipipo.vpn.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParserTest {

    private val jsonValido = "{\"tag_name\":\"v0.2.0\"," +
        "\"name\":\"uSipipo VPN 0.2.0\"," +
        "\"body\":\"Notas de la release\"," +
        "\"html_url\":\"https://github.com/usipipo/usipipo-vpn/releases/tag/v0.2.0\"," +
        "\"draft\":false,\"prerelease\":false}"

    @Test
    fun `parsea un release valido`() {
        val release = ReleaseParser.parse(jsonValido)

        assertEquals(Version(0, 2, 0), release?.version)
        assertEquals("https://github.com/usipipo/usipipo-vpn/releases/tag/v0.2.0", release?.htmlUrl)
        assertEquals("uSipipo VPN 0.2.0", release?.name)
    }

    @Test
    fun `acepta un tag sin la v`() {
        val json = jsonValido.replace("v0.2.0", "0.2.0")

        assertEquals(Version(0, 2, 0), ReleaseParser.parse(json)?.version)
    }

    @Test
    fun `funciona sin body`() {
        val json = "{\"tag_name\":\"v1.0.0\"," +
            "\"html_url\":\"https://github.com/usipipo/usipipo-vpn/releases/tag/v1.0.0\"}"

        val release = ReleaseParser.parse(json)

        assertEquals(Version(1, 0, 0), release?.version)
        assertNull(release?.name)
    }

    @Test
    fun `funciona sin name`() {
        val json = "{\"tag_name\":\"v0.1.1\",\"name\":null," +
            "\"html_url\":\"https://example.test/releases/v0.1.1\"}"

        val release = ReleaseParser.parse(json)

        assertEquals(Version(0, 1, 1), release?.version)
        assertNull(release?.name)
    }

    @Test
    fun `ignora objetos y arrays anidados`() {
        val json = "{\"author\":{\"login\":\"usipipo\",\"html_url\":\"https://otro.test\"}," +
            "\"assets\":[{\"name\":\"app-release.apk\",\"size\":1024}]," +
            "\"tag_name\":\"v0.3.0\"," +
            "\"html_url\":\"https://github.com/usipipo/usipipo-vpn/releases/tag/v0.3.0\"}"

        val release = ReleaseParser.parse(json)

        assertEquals("https://github.com/usipipo/usipipo-vpn/releases/tag/v0.3.0", release?.htmlUrl)
        assertEquals(Version(0, 3, 0), release?.version)
    }

    @Test
    fun `entiende escapes basicos en los strings`() {
        val json = "{\"tag_name\":\"v1.2.0\"," +
            "\"html_url\":\"https://example.test/r?a=b\\u0026c=1\"," +
            "\"name\":\"Linea \\\"entre comillas\\\" y salto\\nlinea\"}"

        val release = ReleaseParser.parse(json)

        assertEquals("https://example.test/r?a=b&c=1", release?.htmlUrl)
        assertTrue(release?.name!!.contains("Linea \"entre comillas\""))
    }

    @Test
    fun `devuelve null si falta tag_name`() {
        val json = "{\"name\":\"algo\",\"html_url\":\"https://example.test\"}"

        assertNull(ReleaseParser.parse(json))
    }

    @Test
    fun `devuelve null si falta html_url`() {
        val json = "{\"tag_name\":\"v1.0.0\",\"name\":\"algo\"}"

        assertNull(ReleaseParser.parse(json))
    }

    @Test
    fun `devuelve null si el tag no parsea`() {
        val json = "{\"tag_name\":\"nightly\",\"html_url\":\"https://example.test\"}"

        assertNull(ReleaseParser.parse(json))
    }

    @Test
    fun `devuelve null con json roto`() {
        assertNull(ReleaseParser.parse("{"))
        assertNull(ReleaseParser.parse("{\"tag_name\": "))
        assertNull(ReleaseParser.parse("{\"tag_name\":\"v1.0.0\",}"))
        assertNull(ReleaseParser.parse("\"solo un string\""))
        assertNull(ReleaseParser.parse(""))
        assertNull(ReleaseParser.parse("[]"))
    }
}
