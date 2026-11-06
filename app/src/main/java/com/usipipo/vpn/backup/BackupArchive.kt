package com.usipipo.vpn.backup

/**
 * Nombres de entrada del zip de copia.
 *
 * Un zip no es de fiar: una entrada puede llamarse `../../algo` y escribir fuera de
 * donde toca (zip slip). Aquí se fija el nombre que la app acepta y se valida antes
 * de leer nada.
 */
object BackupArchive {

    /** Entrada con los ajustes de la app. */
    const val SETTINGS_ENTRY: String = "settings.json"

    /** Sufijo de las entradas de perfil cifradas. */
    const val ENTRY_SUFFIX: String = ".conf.enc"

    /** Nombre de la entrada de un perfil: su id más el sufijo. */
    fun entryName(configId: String): String = "$configId$ENTRY_SUFFIX"

    /** Id del perfil que guarda esa entrada, o null si el nombre no es de perfil. */
    fun configIdFromEntry(entryName: String): String? {
        if (!entryName.endsWith(ENTRY_SUFFIX)) return null
        val id = entryName.removeSuffix(ENTRY_SUFFIX)
        return id.takeIf { it.isNotEmpty() && isSafeEntryName(entryName) }
    }

    /** ¿Se puede leer esa entrada sin salirse del destino? */
    fun isSafeEntryName(name: String): Boolean {
        if (name.isEmpty()) return false
        if (name.startsWith("/")) return false
        if (name.contains("..")) return false
        // La app solo escribe entradas planas: un perfil o settings.json.
        return name.none { it == '/' || it == '\\' }
    }
}
