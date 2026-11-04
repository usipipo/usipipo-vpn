package com.usipipo.vpn.data

import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.settings.FakeSharedPreferences
import com.usipipo.vpn.ui.home.FakeConfigStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Lector del `.conf` listo para el túnel: inyección y poda de apps desinstaladas. */
class ConfigTextReaderTest {

    private val conf = """
        [Interface]
        PrivateKey = c1X3i1SeWVfHBDEYVA7IwJ4KiIb5OY5jgobosZdvtys=
        Address = 10.0.0.2/32
        DNS = 1.1.1.1

        [Peer]
        PublicKey = LvgFZ6NZXTEL5/jc4CXF2zCMcXa3nrSIM8b/r/+8kt0=
        AllowedIPs = 0.0.0.0/0
        Endpoint = 1.2.3.4:51820
    """.trimIndent()

    @Test
    fun `sin apps excluidas el texto no cambia`() = runTest {
        val lector = reader()

        val texto = lector.readForTunnel("cfg")

        assertEquals(conf, texto)
    }

    @Test
    fun `con apps excluidas el texto lleva la linea`() = runTest {
        val settings = AppSettings(FakeSharedPreferences())
        settings.excludedApps = "com.app.excluida"
        val lector = reader(settings = settings)

        val texto = lector.readForTunnel("cfg")

        assertEquals(
            "ExcludedApplications = com.app.excluida",
            texto?.lines()?.firstOrNull { it.startsWith("ExcludedApplications") },
        )
    }

    @Test
    fun `un paquete desinstalado se quita de la lista guardada`() = runTest {
        val settings = AppSettings(FakeSharedPreferences())
        settings.excludedApps = "com.app.excluida,com.desinstalada.app"
        val lector = reader(settings = settings, instalados = setOf("com.app.excluida"))

        lector.readForTunnel("cfg")

        assertEquals("com.app.excluida", settings.excludedApps)
    }

    @Test
    fun `sin poder consultar los paquetes no se poda nada`() = runTest {
        val settings = AppSettings(FakeSharedPreferences())
        settings.excludedApps = "com.app.excluida,com.desinstalada.app"
        val lector = reader(settings = settings, instalados = null)

        lector.readForTunnel("cfg")

        assertEquals("com.app.excluida,com.desinstalada.app", settings.excludedApps)
    }

    @Test
    fun `un conjunto vacio de instalados poda todo y no inyecta`() = runTest {
        val settings = AppSettings(FakeSharedPreferences())
        settings.excludedApps = "com.app.excluida"
        val lector = reader(settings = settings, instalados = setOf())

        lector.readForTunnel("cfg")

        // Un conjunto vacío significa "no hay nada instalado", que es distinto de "no
        // sé qué hay instalado": en el primer caso la lista se vacía.
        assertEquals("", settings.excludedApps)
    }

    @Test
    fun `un conf inexistente devuelve null`() = runTest {
        val lector = reader(store = FakeConfigStore(texts = emptyMap()))

        assertNull(lector.readForTunnel("cfg"))
    }

    @Test
    fun `la poda se mantiene en lecturas sucesivas`() = runTest {
        val settings = AppSettings(FakeSharedPreferences())
        settings.excludedApps = "com.app.excluida,com.desinstalada.app"
        val lector = reader(settings = settings, instalados = setOf("com.app.excluida"))

        lector.readForTunnel("cfg")
        lector.readForTunnel("cfg")

        assertEquals("com.app.excluida", settings.excludedApps)
    }

    private fun reader(
        store: ConfigStore = FakeConfigStore(texts = mapOf("cfg" to conf)),
        settings: AppSettings = AppSettings(FakeSharedPreferences()),
        // Por defecto no se pueden consultar los paquetes: es el caso seguro, el que
        // se da sin QUERY_ALL_PACKAGES, y no debe podar nada.
        instalados: Set<String>? = null,
    ) = ConfigTextReader(
        store = store,
        appSettings = settings,
        installedPackages = { instalados },
    )
}
