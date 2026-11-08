package com.usipipo.vpn.data

import com.usipipo.vpn.settings.FakeSharedPreferences
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Borrado completo de los datos de la app.
 *
 * Las preferencias se pasan por nombre para poder comprobarlas sin Android: quien
 * llama (AppContainer) decide qué fichero de preferencias corresponde a cada nombre.
 */
class StorageWipeTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `vacia el directorio de cache y lo deja creado`() {
        val cache = temp.newFolder("cache")
        cache.write("geo.json", "contenido")
        File(cache, "subdirectorio").apply { mkdirs() }.write("temporal.bin", "mas contenido")

        val resultado = StorageWipe(
            cacheDir = cache,
            prefsByName = { FakeSharedPreferences() },
            prefNames = emptyList(),
        ).run()

        assertTrue(resultado.isSuccess)
        assertTrue("el directorio se recrea para que la app siga escribiendo", cache.isDirectory)
        assertEquals(0, cache.listFiles()?.size)
    }

    @Test
    fun `limpia cada preferences que se le pasa`() {
        val ajustes = FakeSharedPreferences().apply {
            edit().putString("theme_mode", "dark").putBoolean("animations_enabled", false).apply()
        }
        val registro = FakeSharedPreferences().apply {
            edit().putBoolean("verbose", true).apply()
        }
        val pedidos = mutableListOf<String>()
        val porNombre = mapOf("usipipo_settings" to ajustes, "diagnostics" to registro)

        val resultado = StorageWipe(
            cacheDir = temp.newFolder("cache"),
            prefsByName = { name ->
                pedidos += name
                porNombre.getValue(name)
            },
            prefNames = porNombre.keys.toList(),
        ).run()

        assertTrue(resultado.isSuccess)
        assertEquals(setOf("usipipo_settings", "diagnostics"), pedidos.toSet())
        assertTrue(ajustes.getAll().isEmpty())
        assertTrue(registro.getAll().isEmpty())
    }

    @Test
    fun `si borrar las preferencias falla, el borrado entero falla`() {
        val cache = temp.newFolder("cache")
        cache.write("geo.json", "contenido")

        val resultado = StorageWipe(
            cacheDir = cache,
            prefsByName = { throw IllegalStateException("prefs ilegibles") },
            prefNames = listOf("usipipo_settings"),
        ).run()

        assertTrue(resultado.isFailure)
        assertEquals(1, cache.listFiles()?.size)
    }

    private fun File.write(name: String, content: String) {
        File(this, name).writeText(content)
    }
}
