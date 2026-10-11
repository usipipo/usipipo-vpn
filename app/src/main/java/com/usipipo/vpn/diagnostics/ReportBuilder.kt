package com.usipipo.vpn.diagnostics

/**
 * Arma el texto del informe que se adjunta al correo. Código puro (sin Android).
 * El resultado completo pasa por [LogRedactor], venga de donde venga cada sección.
 */
object ReportBuilder {

    const val DEFAULT_MAX_CHARS = 1_500_000
    private const val RESERVE = 200
    private const val TRIM_NOTICE = "[… recortado: se omitieron las líneas más antiguas …]"

    fun build(
        generatedAt: String,
        infoRows: List<Pair<String, String>>,
        log: String,
        logcat: String?,
        verbose: Boolean,
        maxChars: Int = DEFAULT_MAX_CHARS,
    ): String {
        val header = buildString {
            appendLine("=== uSipipo VPN · informe de diagnóstico ===")
            appendLine("Generado: $generatedAt")
            appendLine("Modo detallado: ${if (verbose) "activado" else "desactivado"}")
            appendLine()
            appendLine("--- Información ---")
            infoRows.forEach { (label, value) -> appendLine("$label: $value") }
            appendLine()
        }

        val remaining = (maxChars - header.length - RESERVE).coerceAtLeast(0)
        val logBudget = if (verbose) remaining / 2 else remaining
        val logcatBudget = remaining - logBudget

        val report = buildString {
            append(header)
            appendLine("--- Registro de la app ---")
            appendLine(tail(log.ifBlank { "(vacío)" }, logBudget))
            if (verbose) {
                appendLine()
                appendLine("--- logcat (proceso de la app) ---")
                appendLine(tail(logcat?.ifBlank { "(vacío)" } ?: "(no disponible)", logcatBudget))
            }
        }
        return LogRedactor.redact(report)
    }

    /** Conserva lo más reciente; si recorta, empieza en una línea completa y avisa. */
    internal fun tail(text: String, budget: Int): String {
        if (text.length <= budget) return text
        val cut = text.substring(text.length - budget)
        val firstNewline = cut.indexOf('\n')
        val clean = if (firstNewline in 0 until cut.length - 1) cut.substring(firstNewline + 1) else cut
        return TRIM_NOTICE + "\n" + clean
    }
}
