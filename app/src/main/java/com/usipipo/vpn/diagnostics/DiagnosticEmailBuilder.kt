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
            // Todos los textos se sanean: el asunto siempre tiene cuatro campos
            // separados por " | " y la cabecera cinco líneas, pase lo que pase.
            val cleanVersion = sanitize(version)
            val cleanAndroid = sanitize(androidRelease)
            val cleanManufacturer = sanitize(manufacturer).ifEmpty { UNKNOWN }
            val cleanModel = sanitize(model).ifEmpty { UNKNOWN }
            val deviceText = joinDevice(cleanManufacturer, cleanModel)

            val subject = buildSubject(cleanVersion, deviceText, cleanAndroid)
            val header = buildHeader(
                appName = sanitize(appName),
                version = cleanVersion,
                deviceText = deviceText,
                androidRelease = cleanAndroid,
                verbose = verbose,
            )

            Parts(subject, header)
        }.getOrElse { fallback(appName, version, verbose) }
    }

    /** Quita saltos de línea y el separador de campos, y recorta los espacios sobrantes. */
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
            "v${trimField(version)}",
            trimField(deviceText),
            "Android ${trimField(androidRelease)}",
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

    /** El correo nunca se pierde por un error de formato: siempre sale el prefijo. */
    private fun fallback(appName: String, version: String, verbose: Boolean): Parts {
        val cleanVersion = sanitize(version)
        return Parts(
            subject = "$PREFIX | ${trimField(cleanVersion)} | $UNKNOWN | Android $UNKNOWN",
            header = buildString {
                appendLine("app: ${trimField(sanitize(appName))}")
                appendLine("version: ${trimField(cleanVersion)}")
                appendLine("model: $UNKNOWN")
                appendLine("android: $UNKNOWN")
                appendLine("verbose: ${if (verbose) "si" else "no"}")
            },
        )
    }
}
