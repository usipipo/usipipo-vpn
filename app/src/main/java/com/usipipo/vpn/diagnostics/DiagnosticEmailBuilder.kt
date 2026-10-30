package com.usipipo.vpn.diagnostics

/**
 * Arma el asunto y la cabecera del correo de diagnóstico. Código puro (sin dependencias de Android).
 *
 * Formato del asunto (según docs/plans/2026-10-25-diagnostic-email-subject-design.md):
 *   uSipipo-Diag | v0.4.0 | Samsung SM-A256E | Android 16
 *
 * Cabecera (primeras líneas del cuerpo, cinco campos):
 *   app: uSipipo VPN
 *   version: 0.4.0
 *   model: Samsung SM-A256E
 *   android: 16
 *   verbose: no
 */
object DiagnosticEmailBuilder {

    private const val PREFIX = "uSipipo-Diag"
    private const val UNKNOWN = "desconocido"
    private const val MAX_FIELD_LEN = 40

    data class Parts(val subject: String, val header: String)

    fun build(
        appName: String,
        version: String,
        manufacturer: String,
        model: String,
        androidRelease: String,
        verbose: Boolean,
    ): Parts {
        return runCatching {
            val cleanManufacturer = sanitize(manufacturer).ifEmpty { UNKNOWN }
            val cleanModel = sanitize(model).ifEmpty { UNKNOWN }
            val deviceText = joinDevice(cleanManufacturer, cleanModel)

            val subject = buildSubject(version, deviceText, androidRelease)
            val header = buildHeader(appName, version, deviceText, androidRelease, verbose)

            Parts(subject, header)
        }.getOrElse { fallback(appName, version, verbose) }
    }

    private fun sanitize(text: String): String =
        text.replace("\n", " ").replace("\r", " ").replace("|", " ").trim()

    /** Si el modelo ya comienza con la marca, no se duplica. */
    private fun joinDevice(manufacturer: String, model: String): String =
        if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"

    private fun trimField(text: String): String = text.take(MAX_FIELD_LEN)

    private fun buildSubject(
        version: String,
        deviceText: String,
        androidRelease: String,
    ): String =
        listOf(
            PREFIX,
            "v$version",
            trimField(deviceText),
            "Android $androidRelease",
        ).joinToString(" | ")

    private fun buildHeader(
        appName: String,
        version: String,
        deviceText: String,
        androidRelease: String,
        verbose: Boolean,
    ): String =
        buildString {
            appendLine("app: ${trimField(appName)}")
            appendLine("version: ${trimField(version)}")
            appendLine("model: ${trimField(deviceText)}")
            appendLine("android: ${trimField(androidRelease)}")
            appendLine("verbose: ${if (verbose) "si" else "no"}")
        }

    private fun fallback(appName: String, version: String, verbose: Boolean): Parts =
        Parts(
            subject = "$PREFIX | $version | $UNKNOWN | Android $UNKNOWN",
            header = buildString {
                appendLine("app: $appName")
                appendLine("version: $version")
                appendLine("model: $UNKNOWN")
                appendLine("android: $UNKNOWN")
                appendLine("verbose: ${if (verbose) "si" else "no"}")
            },
        )
}
