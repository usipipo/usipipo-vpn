package com.usipipo.vpn.data

import com.usipipo.vpn.domain.CountryInfo
import com.usipipo.vpn.domain.SavedConfig
import kotlinx.coroutines.flow.StateFlow

class DuplicateNameException : Exception("Ya existe una configuración con ese nombre")

/** Almacén de configuraciones de WireGuard. El texto del .conf nunca sale de aquí sin necesidad. */
interface ConfigStore {
    val configs: StateFlow<List<SavedConfig>>

    /** Última configuración elegida por el usuario (null si no hay ninguna). */
    val lastUsedId: StateFlow<String?>

    /** Guarda el .conf cifrado. La nueva configuración queda como la última usada. */
    suspend fun add(name: String, configText: String): Result<SavedConfig>

    /** Devuelve el .conf descifrado, o null si no existe o no se pudo descifrar. */
    suspend fun readText(id: String): String?

    suspend fun remove(id: String)

    suspend fun setLastUsed(id: String)

    /** Marca o desmarca una configuración como favorita. Ignora ids que no existen. */
    suspend fun setFavorite(id: String, favorite: Boolean)

    /** Cambia el nombre visible. Falla con [DuplicateNameException] si otro perfil ya lo usa (sin distinguir mayúsculas) y con [IllegalArgumentException] si queda vacío. */
    suspend fun rename(id: String, name: String): Result<SavedConfig>

    /** Actualiza los campos de caché de país de un perfil. Ignora ids que no existen. */
    suspend fun updateCountry(id: String, countryInfo: CountryInfo)
}
