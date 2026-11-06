package com.usipipo.vpn.ui.settings.backup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usipipo.vpn.R
import com.usipipo.vpn.backup.BackupFormat
import com.usipipo.vpn.domain.SavedConfig

/**
 * Subpantalla de Copia de seguridad con su ViewModel.
 *
 * Los ficheros se eligen con el selector del sistema (SAF): la app no pide permisos
 * de almacenamiento externo. Si el usuario cancela el selector no pasa nada. La
 * contraseña se pide antes de escribir, así que el selector solo se abre cuando ya
 * se tiene todo lo que hace falta para exportar.
 */
@Composable
fun BackupRoute(
    viewModel: BackupViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    /** Contraseña de la exportación que espera al selector de documentos. */
    var password by remember { mutableStateOf<String?>(null) }

    /** Perfil a exportar en claro, esperando el aviso de claves privadas. */
    var profileToExport by remember { mutableStateOf<String?>(null) }

    /** Copia elegida para importar, esperando su contraseña. */
    var uriToImport by remember { mutableStateOf<Uri?>(null) }

    val createZip = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MIME_ZIP),
    ) { uri ->
        val clave = password
        password = null
        if (uri == null || clave == null) return@rememberLauncherForActivityResult
        val out = context.contentResolver.openOutputStream(uri)
        if (out == null) {
            viewModel.consumeMessage()
        } else {
            viewModel.exportAll(clave, out)
        }
    }

    val createConf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MIME_CONF),
    ) { uri ->
        val id = profileToExport
        profileToExport = null
        if (uri == null || id == null) return@rememberLauncherForActivityResult
        val out = context.contentResolver.openOutputStream(uri)
        if (out == null) {
            viewModel.consumeMessage()
        } else {
            viewModel.exportProfile(id, out)
        }
    }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val input = context.contentResolver.openInputStream(uri)
        if (input == null) {
            viewModel.consumeMessage()
            return@rememberLauncherForActivityResult
        }
        val bytes = input.use { it.readBytes() }
        if (BackupFormat.looksLikeZip(bytes)) {
            // Copia cifrada: hace falta la contraseña antes de leer nada.
            uriToImport = uri
        } else {
            viewModel.importProfile(bytes.toString(Charsets.UTF_8), nombreDocumento(context, uri))
        }
    }

    BackupScreen(
        lastBackupAt = state.lastBackupAt,
        profiles = state.profiles,
        busy = state.busy,
        conflicts = state.conflicts.map { it.existing.name to it.incoming.name },
        message = messageText(context, state.message),
        profileWarningId = profileToExport,
        importPasswordUriVisible = uriToImport != null,
        onExportAll = { clave -> password = clave; createZip.launch(SUGGESTED_ZIP_NAME) },
        onExportProfile = { id -> profileToExport = id },
        onExportProfileConfirm = {
            val id = profileToExport
            profileToExport = null
            if (id != null) createConf.launch("$id.conf")
        },
        onExportProfileCancel = { profileToExport = null },
        onImport = { openDocument.launch(arrayOf(MIME_ZIP, MIME_CONF, "*/*")) },
        onImportPassword = { clave ->
            val uri = uriToImport
            uriToImport = null
            if (uri != null) {
                val input = context.contentResolver.openInputStream(uri)
                if (input == null) viewModel.consumeMessage() else viewModel.import(clave, input)
            }
        },
        onImportPasswordCancel = { uriToImport = null },
        onResolveConflict = viewModel::resolveConflict,
        onCancelImport = viewModel::cancelImport,
        onMessageShown = viewModel::consumeMessage,
        onBack = onBack,
    )
}

/** Nombre visible del documento elegido; si el sistema no lo da, uno genérico. */
private fun nombreDocumento(context: Context, uri: Uri): String {
    val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
    val nombre = context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
    return nombre?.removeSuffix(".conf")?.takeIf { it.isNotBlank() } ?: DEFAULT_PROFILE_NAME
}

private const val DEFAULT_PROFILE_NAME = "Perfil importado"

/** Texto del mensaje de resultado: la UI no interpreta los tipos sellados. */private fun messageText(context: Context, message: BackupMessage?): String? = when (message) {
    null -> null
    BackupMessage.Exported -> context.getString(R.string.backup_exported)
    BackupMessage.Imported -> context.getString(R.string.backup_imported)
    BackupMessage.ProfileExported -> context.getString(R.string.backup_profile_exported)
    BackupMessage.ProfileUnreadable -> context.getString(R.string.backup_profile_unreadable)
    BackupMessage.EmptyPassword -> context.getString(R.string.backup_empty_password)
    BackupMessage.WrongPassword -> context.getString(R.string.backup_wrong_password)
    BackupMessage.CorruptBackup -> context.getString(R.string.backup_corrupt)
    is BackupMessage.Conflicts ->
        context.getString(R.string.backup_conflicts, message.count)
}

private const val MIME_ZIP = "application/zip"
private const val MIME_CONF = "application/x-wireguard"
private const val SUGGESTED_ZIP_NAME = "usipipo-copia.zip"
