package com.usipipo.vpn.backup

/**
 * Nombres de los perfiles dentro de la copia.
 *
 * Las entradas del zip se llaman por id, así que sin este manifiesto el nombre
 * visible del perfil se perdería al importar (y se acabaría usando el id como
 * nombre). Es opcional al leer: una copia sin él sigue importando, con el id como
 * nombre.
 */
object BackupManifest {

    /** Texto JSON del mapa `id -> nombre`. */
    fun serialize(profiles: List<BackupProfile>): String {
        val names = org.json.JSONObject()
        profiles.forEach { names.put(it.id, it.name) }
        return names.toString()
    }

    /** Mapa `id -> nombre`; un JSON inválido es una copia corrupta. */
    fun parse(json: String): Map<String, String> {
        val root = try {
            org.json.JSONObject(json)
        } catch (e: Exception) {
            throw CorruptBackupException("manifest.json no es JSON válido")
        }
        val names = mutableMapOf<String, String>()
        root.keys().forEach { key ->
            when (val value = root.get(key)) {
                is String -> names[key] = value
                else -> Unit
            }
        }
        return names
    }
}
