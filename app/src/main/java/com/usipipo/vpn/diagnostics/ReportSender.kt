package com.usipipo.vpn.diagnostics

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Crea el archivo del informe (en caché, solo el último) y el correo que lo adjunta. */
object ReportSender {

    private const val DIR_NAME = "reports"

    /** Llamar fuera del hilo principal. */
    fun buildFile(
        context: Context,
        infoRows: List<Pair<String, String>>,
        verbose: Boolean,
    ): File {
        val dir = File(context.cacheDir, DIR_NAME).apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }
        val logcat = if (verbose) LogcatCollector.collect() else null
        val now = LocalDateTime.now()
        val text = ReportBuilder.build(
            generatedAt = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            infoRows = infoRows,
            log = AppLog.readAll(),
            logcat = logcat,
            verbose = verbose,
        )
        val stamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        return File(dir, "diagnostico-$stamp.txt").also { it.writeText(text) }
    }

    /**
     * ACTION_SENDTO con mailto: limita la lista a apps de correo. El cuerpo repite la información
     * del dispositivo por si el cliente de correo ignora el adjunto.
     */
    fun emailIntent(
        context: Context,
        file: File,
        address: String,
        subject: String,
        body: String,
    ): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$address")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
