package com.usipipo.vpn.data

import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.CountryInfo
import com.usipipo.vpn.domain.SavedConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Cada configuración se guarda en `<id>.enc` (cifrada). El índice `index.json`
 * solo contiene ids y nombres visibles, nunca claves.
 */
class EncryptedFileConfigStore(
    private val dir: File,
    private val crypto: Crypto,
) : ConfigStore {

    private val mutex = Mutex()
    private val indexFile = File(dir, "index.json")

    private val _configs = MutableStateFlow<List<SavedConfig>>(emptyList())
    private val _lastUsedId = MutableStateFlow<String?>(null)

    override val configs: StateFlow<List<SavedConfig>> = _configs.asStateFlow()
    override val lastUsedId: StateFlow<String?> = _lastUsedId.asStateFlow()

    init {
        dir.mkdirs()
        loadIndex()
    }

    override suspend fun add(name: String, configText: String): Result<SavedConfig> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val clean = name.trim()
                if (_configs.value.any { it.name.equals(clean, ignoreCase = true) }) {
                    return@withContext Result.failure<SavedConfig>(DuplicateNameException())
                }
                runCatching {
                    val id = UUID.randomUUID().toString()
                    fileFor(id).writeBytes(crypto.encrypt(configText.toByteArray(Charsets.UTF_8)))
                    val saved = SavedConfig(id = id, name = clean)
                    _configs.value = _configs.value + saved
                    _lastUsedId.value = id
                    persistIndex()
                    saved
                }.onFailure { AppLog.e("Almacen", "No se pudo guardar la configuración", it) }
            }
        }

    override suspend fun readText(id: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileFor(id)
            if (!file.exists()) return@runCatching null
            String(crypto.decrypt(file.readBytes()), Charsets.UTF_8)
        }.onFailure {
            AppLog.e("Almacen", "No se pudo descifrar la configuración (cfg ${id.take(8)})", it)
        }.getOrNull()
    }

    override suspend fun remove(id: String) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                fileFor(id).delete()
                _configs.value = _configs.value.filterNot { it.id == id }
                if (_lastUsedId.value == id) _lastUsedId.value = null
                persistIndex()
            }
        }
    }

    override suspend fun setLastUsed(id: String) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                if (_configs.value.any { it.id == id }) {
                    _lastUsedId.value = id
                    persistIndex()
                }
            }
        }
    }

    private fun fileFor(id: String) = File(dir, "$id.enc")

    private fun loadIndex() {
        if (!indexFile.exists()) return
        runCatching {
            val root = JSONObject(indexFile.readText())
            val array = root.optJSONArray(KEY_CONFIGS) ?: JSONArray()
            // Un perfil ilegible no debe invalidar el resto del indice.
            val loaded = (0 until array.length()).mapNotNull { i ->
                runCatching { array.getJSONObject(i).toSavedConfig() }.getOrNull()
            }.filter { fileFor(it.id).exists() }
            _configs.value = loaded
            _lastUsedId.value = root.optString(KEY_LAST_USED).takeIf { id -> loaded.any { it.id == id } }
        }.onFailure { AppLog.e("Almacen", "No se pudo leer el índice de configuraciones", it) }
    }

    /**
     * Lee un perfil del indice. Los campos ausentes (indices antiguos) quedan en null
     * gracias a [optStringOrNull].
     */
    private fun JSONObject.toSavedConfig(): SavedConfig = SavedConfig(
        id = getString(KEY_ID),
        name = getString(KEY_NAME),
        favorite = optBoolean(KEY_FAVORITE, false),
        countryCode = optStringOrNull(KEY_COUNTRY_CODE),
        countryName = optStringOrNull(KEY_COUNTRY_NAME),
        countryIp = optStringOrNull(KEY_COUNTRY_IP),
        countryFetchedAt = optLong(KEY_COUNTRY_FETCHED_AT, -1).takeIf { it >= 0 },
    )

    private fun persistIndex() {
        val array = JSONArray()
        _configs.value.forEach {
            array.put(
                JSONObject()
                    .put(KEY_ID, it.id)
                    .put(KEY_NAME, it.name)
                    .put(KEY_FAVORITE, it.favorite)
                    .put(KEY_COUNTRY_CODE, it.countryCode.orEmpty())
                    .put(KEY_COUNTRY_NAME, it.countryName.orEmpty())
                    .put(KEY_COUNTRY_IP, it.countryIp.orEmpty())
                    .put(KEY_COUNTRY_FETCHED_AT, it.countryFetchedAt ?: -1L),
            )
        }
        val root = JSONObject().put(KEY_CONFIGS, array)
        _lastUsedId.value?.let { root.put(KEY_LAST_USED, it) }

        val tmp = File(dir, "index.json.tmp")
        tmp.writeText(root.toString())
        Files.move(
            tmp.toPath(),
            indexFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                if (_configs.value.none { it.id == id }) return@withContext
                _configs.value = _configs.value.map { if (it.id == id) it.copy(favorite = favorite) else it }
                persistIndex()
            }
        }
    }

    override suspend fun rename(id: String, name: String): Result<SavedConfig> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val clean = name.trim()
                val current = _configs.value.firstOrNull { it.id == id }
                    ?: return@withContext Result.failure<SavedConfig>(NoSuchElementException())
                if (clean.isEmpty()) {
                    return@withContext Result.failure<SavedConfig>(IllegalArgumentException("Nombre vacío"))
                }
                if (_configs.value.any { it.id != id && it.name.equals(clean, ignoreCase = true) }) {
                    return@withContext Result.failure<SavedConfig>(DuplicateNameException())
                }
                runCatching {
                    val renamed = current.copy(name = clean)
                    _configs.value = _configs.value.map { if (it.id == id) renamed else it }
                    persistIndex()
                    renamed
                }.onFailure { AppLog.e("Almacen", "No se pudo renombrar la configuración", it) }
            }
        }

    override suspend fun updateCountry(id: String, countryInfo: CountryInfo) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                if (_configs.value.none { it.id == id }) return@withContext
                _configs.value = _configs.value.map { cfg ->
                    if (cfg.id == id) {
                        cfg.copy(
                            countryCode = countryInfo.countryCode,
                            countryName = countryInfo.countryName,
                            countryIp = countryInfo.ip,
                            countryFetchedAt = countryInfo.fetchedAt,
                        )
                    } else cfg
                }
                persistIndex()
            }
        }
    }

    private companion object {
        const val KEY_CONFIGS = "configs"
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_LAST_USED = "lastUsedId"
        const val KEY_FAVORITE = "favorite"
        const val KEY_COUNTRY_CODE = "countryCode"
        const val KEY_COUNTRY_NAME = "countryName"
        const val KEY_COUNTRY_IP = "countryIp"
        const val KEY_COUNTRY_FETCHED_AT = "countryFetchedAt"
    }
}
