package com.usipipo.vpn.data

/**
 * Prepara el texto de un `.conf` justo antes de pasarlo al túnel: inyecta la lista
 * global de apps excluidas en el bloque [Interface], como `ExcludedApplications`.
 *
 * El `.conf` guardado en disco no se modifica nunca: esto solo altera la copia que se
 * lee. Si la lista está vacía, el texto se devuelve tal cual.
 */
object ExcludedAppsConfig {

    /**
     * Devuelve [configText] con `ExcludedApplications = pkg1, pkg2` en su [Interface].
     *
     * - Sin paquetes válidos, devuelve el texto sin cambios.
     * - Si el `.conf` ya declara `ExcludedApplications`, se sustituye el valor.
     * - Si declara `IncludedApplications`, no se inyecta nada: WireGuard rechaza tener
     *   ambas y el túnel no arrancaría. Se prefiere conectar sin exclusiones.
     */
    fun inject(configText: String, packages: List<String>): String {
        val limpios = packages
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
        if (limpios.isEmpty()) return configText
        if (tieneIncludedApplications(configText)) return configText

        val linea = "ExcludedApplications = ${limpios.joinToString(", ")}"
        Regex("(?im)^[ \\t]*ExcludedApplications[ \\t]*=[^\\n]*$").let { existente ->
            if (existente.containsMatchIn(configText)) {
                return existente.replace(configText, linea)
            }
        }

        // Sin línea previa: se cuela justo debajo de la cabecera [Interface].
        // replaceFirst solo acepta texto de reemplazo; la variante con lambda es de
        // replace, que aquí es equivalente porque [Interface] aparece una sola vez.
        val interface_ = Regex("(?im)^[ \\t]*\\[Interface\\][ \\t]*$")
        if (!interface_.containsMatchIn(configText)) return configText
        return interface_.replace(configText) { "${it.value}\n$linea" }
    }

    /** Quita de [packages] los que ya no están en [installed]. */
    fun keepInstalled(packages: List<String>, installed: Set<String>): List<String> =
        packages.filter { it in installed }

    /** Texto que se guarda en `excluded_apps`. */
    fun encode(packages: List<String>): String =
        packages.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")

    /** Lista que representa el texto guardado en `excluded_apps`. */
    fun decode(text: String): List<String> =
        text.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    private fun tieneIncludedApplications(configText: String): Boolean =
        Regex("(?im)^[ \\t]*IncludedApplications[ \\t]*=").containsMatchIn(configText)
}
