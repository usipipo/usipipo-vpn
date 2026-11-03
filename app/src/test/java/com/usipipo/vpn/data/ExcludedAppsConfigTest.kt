package com.usipipo.vpn.data

import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import kotlin.text.Charsets.UTF_8
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Inyección de `ExcludedApplications` en el bloque [Interface] de un `.conf`, antes de
 * `Config.parse`. La lista es global y el `.conf` guardado en disco no se modifica.
 */
@Suppress("FunctionName")
class ExcludedAppsConfigTest {

    @Test
    fun `una lista vacia no toca el conf`() {
        val conf = conf()

        assertEquals(conf, ExcludedAppsConfig.inject(conf, emptyList()))
    }

    @Test
    fun `inyecta la linea en el interface`() {
        val conf = conf()

        val salida = ExcludedAppsConfig.inject(conf, listOf("com.app.excluida", "com.otra.app"))

        assertTrue(salida.contains("ExcludedApplications = com.app.excluida, com.otra.app"))
        assertTrue("el interface debe conservar las demás claves", salida.contains("PrivateKey = clave"))
        assertTrue("el peer debe quedar intacto", salida.contains("[Peer]"))
    }

    @Test
    fun `el conf de salida lo parsea wireguard`() {
        val salida = ExcludedAppsConfig.inject(conf(), listOf("com.app.excluida", "com.otra.app"))

        assertEquals(
            setOf("com.app.excluida", "com.otra.app"),
            paquetesDe(salida),
        )
    }

    @Test
    fun `reemplaza una lista que ya existe en el conf`() {
        val conf = conf().replace(
            "PrivateKey = clave",
            "PrivateKey = clave\nExcludedApplications = com.vieja.app",
        )

        val salida = ExcludedAppsConfig.inject(conf, listOf("com.app.excluida"))

        assertEquals(setOf("com.app.excluida"), paquetesDe(salida))
    }

    @Test
    fun `no duplica si se inyectan los mismos paquetes`() {
        val unaVez = ExcludedAppsConfig.inject(conf(), listOf("com.app.excluida"))
        val dosVeces = ExcludedAppsConfig.inject(unaVez, listOf("com.app.excluida"))

        assertEquals(unaVez, dosVeces)
    }

    @Test
    fun `un conf con includedapplications no se toca`() {
        // Upstream rechaza tener included y excluded a la vez: inyectar rompería el parse.
        val conf = conf().replace(
            "PrivateKey = clave",
            "PrivateKey = clave\nIncludedApplications = com.app.permitida",
        )

        val salida = ExcludedAppsConfig.inject(conf, listOf("com.app.excluida"))

        assertEquals(conf, salida)
    }

    @Test
    fun `la clave excludedapplications se acepta en minusculas`() {
        val conf = conf().replace(
            "PrivateKey = clave",
            "PrivateKey = clave\nexcludedapplications = com.vieja.app",
        )

        val salida = ExcludedAppsConfig.inject(conf, listOf("com.app.excluida"))

        assertEquals(setOf("com.app.excluida"), paquetesDe(salida))
    }

    @Test
    fun `la inyeccion no cambia el conf original`() {
        val conf = conf()

        ExcludedAppsConfig.inject(conf, listOf("com.app.excluida"))

        assertEquals(emptySet<String>(), paquetesDe(conf))
    }

    @Test
    fun `los paquetes se ordenan y deduplican`() {
        val salida = ExcludedAppsConfig.inject(conf(), listOf("com.zeta", "com.alfa", "com.zeta"))

        assertEquals(
            listOf("com.alfa", "com.zeta"),
            paquetesDe(salida).sorted(),
        )
    }

    @Test
    fun `un paquete en blanco se descarta`() {
        val salida = ExcludedAppsConfig.inject(conf(), listOf("", "  ", "com.app.excluida"))

        assertEquals(setOf("com.app.excluida"), paquetesDe(salida))
    }

    @Test
    fun `una lista que solo trae espacios equivale a vacia`() {
        val conf = conf()

        assertEquals(conf, ExcludedAppsConfig.inject(conf, listOf("", "   ")))
    }

    @Test
    fun `quita los paquetes que ya no estan instalados`() {
        val viva = ExcludedAppsConfig.keepInstalled(
            packages = listOf("com.app.excluida", "com.desinstalada.app"),
            installed = setOf("com.app.excluida"),
        )

        assertEquals(listOf("com.app.excluida"), viva)
    }

    @Test
    fun `guardar la lista como texto y leerla devuelve lo mismo`() {
        val paquetes = listOf("com.app.excluida", "com.otra.app")

        val texto = ExcludedAppsConfig.encode(paquetes)

        assertEquals(paquetes, ExcludedAppsConfig.decode(texto))
    }

    @Test
    fun `un texto vacio decodifica a lista vacia`() {
        assertEquals(emptyList<String>(), ExcludedAppsConfig.decode(""))
        assertEquals(emptyList<String>(), ExcludedAppsConfig.decode("  "))
    }

    @Test
    fun `una lista vacia codifica a texto vacio`() {
        assertEquals("", ExcludedAppsConfig.encode(emptyList()))
    }

    private fun conf(): String = buildString {
        append("[Interface]\n")
        append("PrivateKey = clave\n")
        append("Address = 10.0.0.2/32\n")
        append("DNS = 1.1.1.1\n")
        append("\n")
        append("[Peer]\n")
        append("PublicKey = clavepeer\n")
        append("AllowedIPs = 0.0.0.0/0\n")
        append("Endpoint = 1.2.3.4:51820\n")
    }

    /** WireGuard solo acepta InputStream o BufferedReader, nunca texto plano. */
    private fun paquetesDe(text: String): Set<String> =
        Config.parse(ByteArrayInputStream(text.toByteArray(UTF_8))).interface.excludedApplications
}
