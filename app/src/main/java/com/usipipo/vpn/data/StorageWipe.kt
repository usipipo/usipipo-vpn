package com.usipipo.vpn.data

import android.content.SharedPreferences
import java.io.File

/**
 * Borrado completo de los datos de la app: caché de la app y ficheros de preferencias.
 *
 * Los nombres de las preferencias los pone quien llame ([com.usipipo.vpn.AppContainer]),
 * que es el único sitio que conoce todas las dependencias. Así este borrado se puede
 * probar en la JVM con preferencias en memoria.
 */
class StorageWipe(
    private val cacheDir: File,
    private val prefsByName: (String) -> SharedPreferences,
    private val prefNames: List<String>,
) {

    /**
     * Vacía cada fichero de preferencias y después borra el caché.
     *
     * Las preferencias van primero para que un fallo ahí deje el caché intacto.
     * Devuelve fallo si algo no se pudo borrar: quien llama decide si aplica el resto
     * o lo deja todo como estaba.
     */
    fun run(): Result<Unit> = runCatching {
        prefNames.forEach { name ->
            prefsByName(name).edit().clear().commit()
        }
        emptyDir(cacheDir)
    }
}

/**
 * Vacía un directorio dejándolo creado: la app escribe en su caché sin comprobar si
 * existe, así que borrarlo del todo rompería la siguiente escritura.
 */
fun emptyDir(dir: File) {
    dir.deleteRecursively()
    dir.mkdirs()
}
