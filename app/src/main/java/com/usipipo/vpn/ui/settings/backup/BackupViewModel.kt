package com.usipipo.vpn.ui.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.backup.BackupContent
import com.usipipo.vpn.backup.BackupFingerprint
import com.usipipo.vpn.backup.BackupProfile
import com.usipipo.vpn.backup.BackupReader
import com.usipipo.vpn.backup.BackupSettings
import com.usipipo.vpn.backup.BackupWriter
import com.usipipo.vpn.backup.CorruptBackupException
import com.usipipo.vpn.backup.WrongPasswordException
import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.settings.AppSettings
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Conflicto de un perfil entrante que ya está en el dispositivo. */
data class DuplicateConflict(val existing: SavedConfig, val incoming: BackupProfile)

/** Qué hacer con un duplicado. */
enum class DuplicateDecision { Overwrite, KeepBoth, Skip }

/** Estado de la subpantalla de copia de seguridad. */
data class BackupUiState(
    val lastBackupAt: Long = 0L,
    val profiles: List<SavedConfig> = emptyList(),
    val busy: Boolean = false,
    val conflicts: List<DuplicateConflict> = emptyList(),
    val message: BackupMessage? = null,
)

/** Resultado de la última acción, para que la UI lo enseñe sin adivinar. */
sealed interface BackupMessage {
    data object Exported : BackupMessage
    data object Imported : BackupMessage
    data object ProfileExported : BackupMessage
    data object ProfileUnreadable : BackupMessage
    data object EmptyPassword : BackupMessage
    data object WrongPassword : BackupMessage

    /** Copia ilegible; [reason] es el motivo concreto, para poder diagnosticarlo. */
    data class CorruptBackup(val reason: String? = null) : BackupMessage

    /** La escritura falló (disco lleno, permiso retirado): no se marca la copia. */
    data object ExportFailed : BackupMessage
    data class Conflicts(val count: Int) : BackupMessage
}

/**
 * Copia de seguridad: exportar todo en zip cifrado, exportar un perfil en claro e
 * importar con resolución de duplicados.
 *
 * La contraseña solo pasa por aquí y no se guarda: quien la pierde pierde la copia.
 * Si la copia trae duplicados, no se importa nada hasta que la UI resuelva cada
 * conflicto con [resolveConflict].
 */
class BackupViewModel(
    private val store: ConfigStore,
    private val settings: AppSettings,
    private val writer: BackupWriter = BackupWriter(),
    private val reader: BackupReader = BackupReader(),
    /**
     * Donde corre el cifrado y la escritura. En producción es IO: PBKDF2 con 100 000
     * iteraciones no puede ir en la interfaz. Los tests pasan un dispatcher de prueba.
     */
    private val workDispatcher: CoroutineContext = Dispatchers.IO,
) : ViewModel() {

    private val pending = mutableListOf<BackupProfile>()

    private val _uiState = MutableStateFlow(readState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /** Exporta todos los perfiles y los ajustes al stream que le da la UI (SAF). */
    fun exportAll(password: String, output: OutputStream) {
        if (password.isEmpty()) {
            publish(BackupMessage.EmptyPassword)
            return
        }
        launchCopy {
            output.use { stream ->
                val profiles = store.configs.value.map { config ->
                    BackupProfile(
                        id = config.id,
                        name = config.name,
                        configText = store.readText(config.id).orEmpty(),
                    )
                }
                writer.write(profiles, BackupSettings.snapshot(settings), password, stream)
            }
            settings.backupLastAt = System.currentTimeMillis()
            publish(BackupMessage.Exported)
        }
    }

    /**
     * Exporta un perfil en claro. Se llama solo después de enseñar el aviso de claves
     * privadas: el texto del `.conf` lleva la PrivateKey.
     */
    fun exportProfile(configId: String, output: OutputStream) {
        launchCopy {
            val config = store.configs.value.firstOrNull { it.id == configId }
            val text = store.readText(configId)
            if (config == null || text == null) {
                publish(BackupMessage.ProfileUnreadable)
                return@launchCopy
            }
            output.use { stream -> stream.write(text.toByteArray()) }
            settings.backupLastAt = System.currentTimeMillis()
            publish(BackupMessage.ProfileExported)
        }
    }

    /** Lee la copia del stream (SAF) y la deja lista para importar. */
    fun import(password: String, input: InputStream) {
        launchCopy {
            val content = try {
                reader.read(input, password)
            } catch (e: WrongPasswordException) {
                publish(BackupMessage.WrongPassword)
                return@launchCopy
            } catch (e: CorruptBackupException) {
                publish(BackupMessage.CorruptBackup(e.message))
                return@launchCopy
            }
            val conflicts = detectConflicts(content)
            // Los ajustes se restauran al leer la copia, antes de tocar los perfiles:
            // si algo falla después, el usuario ya tiene la foto aplicada.
            applySettings(content.settings)
            if (conflicts.isEmpty()) {
                importAll(content.profiles)
                publish(BackupMessage.Imported)
            } else {
                pending.clear()
                pending.addAll(conflicts.map { it.incoming })
                val libres = content.profiles - conflicts.map { it.incoming }.toSet()
                importAll(libres)
                publish(BackupMessage.Conflicts(conflicts.size))
                _uiState.value = _uiState.value.copy(conflicts = conflicts)
            }
        }
    }

    /**
     * Importa un `.conf` suelto elegido en el selector (RF-6). El nombre sale del
     * fichero; si viene con un perfil repetido, se queda el que ya estaba y se avisa.
     */
    fun importProfile(configText: String, suggestedName: String) {
        launchCopy {
            val inUse = store.configs.value.any { it.name.equals(suggestedName, ignoreCase = true) }
            if (inUse) {
                publish(BackupMessage.ProfileUnreadable)
                return@launchCopy
            }
            store.add(suggestedName, configText)
            settings.backupLastAt = System.currentTimeMillis()
            publish(BackupMessage.Imported)
        }
    }

    /** Aplica la decisión del usuario al primer duplicado pendiente. */
    fun resolveConflict(decision: DuplicateDecision) {
        val conflict = _uiState.value.conflicts.firstOrNull() ?: return
        val incoming = pending.removeFirstOrNull() ?: conflict.incoming
        launchCopy {
            when (decision) {
                DuplicateDecision.Skip -> Unit
                DuplicateDecision.Overwrite -> {
                    store.remove(conflict.existing.id)
                    store.add(incoming.name, incoming.configText)
                }
                DuplicateDecision.KeepBoth -> {
                    // El almacén no admite dos perfiles con el mismo nombre: se busca
                    // uno libre en lugar de fallar en silencio.
                    val libres = store.configs.value.map { it.name }
                    store.add(BackupNames.suggestUnique(incoming.name, libres), incoming.configText)
                }
            }
            val rest = _uiState.value.conflicts.drop(1)
            _uiState.value = _uiState.value.copy(conflicts = rest)
            if (rest.isEmpty()) {
                settings.backupLastAt = System.currentTimeMillis()
                publish(BackupMessage.Imported)
            }
        }
    }

    /** Descarta los duplicados pendientes: ya no se importa nada más. */
    fun cancelImport() {
        pending.clear()
        _uiState.value = _uiState.value.copy(conflicts = emptyList())
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    /** Decide qué perfiles de la copia ya están en el dispositivo. */
    private suspend fun detectConflicts(content: BackupContent): List<DuplicateConflict> =
        content.profiles.mapNotNull { incoming ->
            val existing = store.configs.value.firstOrNull { config ->
                BackupFingerprint.isDuplicate(
                    existingName = config.name,
                    existingText = store.readText(config.id).orEmpty(),
                    incomingName = incoming.name,
                    incomingText = incoming.configText,
                )
            }
            existing?.let { DuplicateConflict(existing = it, incoming = incoming) }
        }

    private suspend fun importAll(profiles: List<BackupProfile>) {
        profiles.forEach { store.add(it.name, it.configText) }
        if (profiles.isNotEmpty()) {
            settings.backupLastAt = System.currentTimeMillis()
        }
    }

    /** Aplica los ajustes que trae la copia; las claves ajenas se ignoran. */
    private fun applySettings(values: Map<String, Any>) {
        BackupSettings.restore(settings, values)
    }

    /**
     * Cifrar, derivar la clave y escribir el zip cuesta (PBKDF2 con 100 000
     * iteraciones): se sale del hilo principal, que es donde corre viewModelScope.
     */
    private fun launchCopy(block: suspend () -> Unit) {
        _uiState.value = _uiState.value.copy(busy = true, message = null)
        viewModelScope.launch {
            try {
                withContext(workDispatcher) { block() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Escritura cortada, disco lleno, permiso retirado: se avisa con un
                // mensaje claro en vez de dejar que la excepción se pierda.
                publish(BackupMessage.ExportFailed)
            } finally {
                refresh()
            }
        }
    }

    private fun publish(message: BackupMessage) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    private fun refresh() {
        _uiState.value = readState().copy(
            busy = false,
            message = _uiState.value.message,
            conflicts = _uiState.value.conflicts,
        )
    }

    private fun readState(): BackupUiState = BackupUiState(
        lastBackupAt = settings.backupLastAt,
        profiles = store.configs.value,
    )
}
