package com.usipipo.vpn.ui.settings.storage

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tamaños leídos de disco para el resumen de Almacenamiento.
 *
 * El registro se mide con `AppLog.sizeBytes()`, que es un objeto sin inicializar fuera
 * de la app y devuelve 0: por eso aquí solo se comprueba lo que depende de los
 * directorios, que es lo que el test puede controlar.
 */
class StorageSizesTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `suma los ficheros de cada directorio`() {
        val configs = temp.newFolder("configs")
        val cache = temp.newFolder("cache")
        configs.write("index.json", "x".repeat(100))
        configs.write("perfil-uno.conf", "y".repeat(200))
        cache.write("geo.json", "z".repeat(50))

        val desglose = StorageSizes.read(configsDir = configs, cacheDir = cache)

        assertEquals(300L, desglose.profilesBytes)
        assertEquals(50L, desglose.tempBytes)
        assertEquals(350L, desglose.totalBytes)
    }

    @Test
    fun `los subdirectorios tambien cuentan`() {
        val configs = temp.newFolder("configs")
        val anidado = File(configs, "perfiles").apply { mkdirs() }
        anidado.write("perfil.conf", "y".repeat(120))

        val desglose = StorageSizes.read(configsDir = configs, cacheDir = temp.root)

        assertEquals(120L, desglose.profilesBytes)
    }

    @Test
    fun `un directorio que no existe mide cero`() {
        val desglose = StorageSizes.read(
            configsDir = File(temp.root, "no-existe"),
            cacheDir = File(temp.root, "tampoco"),
        )

        assertEquals(0L, desglose.profilesBytes)
        assertEquals(0L, desglose.tempBytes)
        assertEquals(0L, desglose.totalBytes)
    }

    private fun File.write(name: String, content: String) {
        File(this, name).writeText(content)
    }
}
