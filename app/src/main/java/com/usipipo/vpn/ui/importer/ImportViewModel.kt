package com.usipipo.vpn.ui.importer

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.data.ConfigImporter
import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.data.DuplicateNameException
import com.usipipo.vpn.diagnostics.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

enum class ImportStage { Choose, Name }

enum class ImportError { InvalidConfig, TooLarge, ReadFailed, NameEmpty, Duplicate, SaveFailed }

data class ImportUiState(
    val stage: ImportStage = ImportStage.Choose,
    val name: String = "",
    val error: ImportError? = null,
    val done: Boolean = false,
)

class ImportViewModel(
    private val importer: ConfigImporter,
    private val store: ConfigStore,
) : ViewModel() {

    private val _ui = MutableStateFlow(ImportUiState())
    val ui: StateFlow<ImportUiState> = _ui.asStateFlow()

    /** Texto del .conf ya validado; vive solo en memoria hasta guardarlo. */
    private var pendingText: String? = null

    fun onFilePicked(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { readPicked(resolver, uri) } }
            result.fold(
                onSuccess = { picked ->
                    if (picked == null) {
                        AppLog.w(TAG, "Archivo rechazado: supera $MAX_BYTES bytes")
                        fail(ImportError.TooLarge)
                    } else {
                        AppLog.i(TAG, "Archivo leído (${picked.first.length} caracteres)")
                        handleText(picked.first, suggestedName(picked.second))
                    }
                },
                onFailure = { e ->
                    AppLog.e(TAG, "No se pudo leer el archivo (${e.javaClass.simpleName})")
                    fail(ImportError.ReadFailed)
                },
            )
        }
    }

    fun onQrScanned(text: String) {
        AppLog.i(TAG, "QR escaneado (${text.length} caracteres)")
        handleText(text, suggestedName = null)
    }

    fun onNameChange(value: String) {
        _ui.update { it.copy(name = value.take(MAX_NAME), error = null) }
    }

    fun save() {
        val text = pendingText ?: return
        val name = _ui.value.name.trim()
        if (name.isEmpty()) {
            fail(ImportError.NameEmpty)
            return
        }
        viewModelScope.launch {
            store.add(name, text).fold(
                onSuccess = {
                    AppLog.i(TAG, "Configuración guardada")
                    pendingText = null
                    _ui.update { it.copy(done = true, error = null) }
                },
                onFailure = { e ->
                    AppLog.w(TAG, "No se pudo guardar (${e.javaClass.simpleName})")
                    fail(if (e is DuplicateNameException) ImportError.Duplicate else ImportError.SaveFailed)
                },
            )
        }
    }

    /** Vuelve al estado inicial. Se llama al salir de la pantalla de importar. */
    fun reset() {
        pendingText = null
        _ui.value = ImportUiState()
    }

    private fun handleText(raw: String, suggestedName: String?) {
        importer.parse(raw).fold(
            onSuccess = { normalized ->
                AppLog.i(TAG, "Configuración válida")
                pendingText = normalized
                val name = suggestedName?.takeIf { it.isNotBlank() } ?: defaultName()
                _ui.update { it.copy(stage = ImportStage.Name, name = name, error = null) }
            },
            onFailure = { e ->
                // Solo el tipo de error: el mensaje del parser puede incluir texto del .conf.
                AppLog.w(TAG, "Configuración no válida (${e.cause?.javaClass?.simpleName ?: e.javaClass.simpleName})")
                fail(ImportError.InvalidConfig)
            },
        )
    }

    private fun fail(error: ImportError) {
        _ui.update { it.copy(error = error) }
    }

    private fun defaultName() = "uSipipo ${store.configs.value.size + 1}"

    private fun suggestedName(fileName: String?): String? =
        fileName?.removeSuffix(".conf")?.take(MAX_NAME)

    /** Devuelve (texto, nombre del archivo) o null si supera el tamaño máximo. */
    private fun readPicked(resolver: ContentResolver, uri: Uri): Pair<String, String?>? {
        val stream = resolver.openInputStream(uri) ?: throw IOException("Sin flujo de lectura")
        val bytes = stream.use { it.readLimited(MAX_BYTES) } ?: return null
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        return String(bytes, Charsets.UTF_8) to displayName
    }

    private fun InputStream.readLimited(max: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var total = 0
        while (true) {
            val n = read(buffer)
            if (n < 0) break
            total += n
            if (total > max) return null
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private companion object {
        const val TAG = "Importar"
        const val MAX_NAME = 30
        const val MAX_BYTES = 64 * 1024
    }
}
