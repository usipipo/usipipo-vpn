package com.usipipo.vpn.ui.settings.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Resultado de la última acción, para que la UI lo enseñe sin adivinar. */
sealed interface StorageMessage {
    data object LogCleared : StorageMessage
    data object CacheCleared : StorageMessage
    data object Wiped : StorageMessage

    /** El borrado no se completó. No se marca nada como hecho. */
    data object Failed : StorageMessage
}

/** Estado de la pantalla de Almacenamiento. */
data class StorageUiState(
    val profilesBytes: Long = 0L,
    val logBytes: Long = 0L,
    val tempBytes: Long = 0L,
    val busy: Boolean = false,
    /** Palabra tecleada para confirmar el borrado total. */
    val wipeWord: String = "",
    /** Solo «BORRAR» escrito exacto habilita el borrado total. */
    val wipeReady: Boolean = false,
    val message: StorageMessage? = null,
) {
    val totalBytes: Long get() = StorageFormat.total(profilesBytes, logBytes, tempBytes)
}

/**
 * Almacenamiento: cuánto ocupa la app y cómo liberarlo.
 *
 * El borrado total no se puede deshacer, así que pide la palabra clave [WIPE_WORD] y
 * deja el resumen a cero solo cuando el borrado acaba de verdad.
 */
class StorageViewModel(
    private val sizes: () -> StorageBreakdown,
    private val onClearLog: () -> Unit,
    private val onClearCache: suspend () -> Unit,
    private val onWipeAll: suspend () -> Boolean,
    private val workDispatcher: CoroutineContext = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(readState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    /** Borra el registro de diagnóstico. */
    fun clearLog() {
        launch(StorageMessage.LogCleared) { onClearLog() }
    }

    /** Vacía el caché de país de los perfiles y los temporales de importación. */
    fun clearCache() {
        launch(StorageMessage.CacheCleared) { onClearCache() }
    }

    /** Palabra tecleada: decide si el borrado total se puede confirmar. */
    fun onWipeWordChange(word: String) {
        _uiState.value = _uiState.value.copy(
            wipeWord = word,
            wipeReady = word.trim() == WIPE_WORD,
        )
    }

    /**
     * Borra todos los datos. La palabra clave se olvida al acabar, falle o no: el
     * diálogo se abre de nuevo desde cero.
     */
    fun wipeAll() {
        launch(exito = StorageMessage.Wiped, alAcabar = { onWipeWordChange("") }) {
            if (!onWipeAll()) error("El borrado no se completó")
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    /**
     * Ejecuta el borrado fuera del hilo principal y actualiza el resumen al terminar.
     * [refresh] solo toca los tamaños, así que el mensaje y la palabra clave siguen
     * en pie aunque el borrado falle.
     */
    private fun launch(
        exito: StorageMessage,
        alAcabar: () -> Unit = {},
        accion: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            try {
                withContext(workDispatcher) { accion() }
                _uiState.value = _uiState.value.copy(message = exito)
                alAcabar()
            } catch (e: CancellationException) {
                // Cancelar el job no es un fallo: se propaga sin tocar el estado.
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = StorageMessage.Failed)
            } finally {
                _uiState.value = _uiState.value.copy(busy = false)
            }
            refresh()
        }
    }

    private fun refresh() {
        val medido = readState()
        _uiState.value = _uiState.value.copy(
            profilesBytes = medido.profilesBytes,
            logBytes = medido.logBytes,
            tempBytes = medido.tempBytes,
        )
    }

    private fun readState(): StorageUiState {
        val breakdown = sizes()
        return StorageUiState(
            profilesBytes = breakdown.profilesBytes,
            logBytes = breakdown.logBytes,
            tempBytes = breakdown.tempBytes,
        )
    }

    private companion object {
        /** Palabra clave del borrado total: escrita por el usuario, sin alternativas. */
        const val WIPE_WORD: String = "BORRAR"
    }
}
