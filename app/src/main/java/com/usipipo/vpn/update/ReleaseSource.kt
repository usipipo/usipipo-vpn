package com.usipipo.vpn.update

import com.usipipo.vpn.diagnostics.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Quién sabe cuál es el último release. Se inyecta para poder probarlo sin red. */
interface ReleaseSource {
    suspend fun latest(): ReleaseInfo?
}

/** Consulta la API pública de GitHub. Sin credenciales: solo funciona en repos públicos. */
class GitHubReleaseSource(
    private val owner: String = OWNER,
    private val repo: String = REPO,
) : ReleaseSource {

    override suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val connection = try {
            val url = URL("$API_BASE/repos/$owner/$repo/releases/latest")
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", USER_AGENT)
            }
        } catch (e: IOException) {
            AppLog.w(TAG, "No se pudo abrir la consulta de releases", e)
            return@withContext null
        }

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                AppLog.w(TAG, "GitHub respondió HTTP ${connection.responseCode}")
                return@withContext null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            ReleaseParser.parse(body) ?: run {
                AppLog.w(TAG, "La respuesta de GitHub no trae un release utilizable")
                null
            }
        } catch (e: IOException) {
            AppLog.w(TAG, "Fallo de red al consultar la última release", e)
            null
        } catch (e: CancellationException) {
            throw e
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val OWNER = "usipipo"
        const val REPO = "usipipo-vpn"

        const val API_BASE = "https://api.github.com"
        const val USER_AGENT = "usipipo-vpn-android"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        const val TAG = "Actualizar"
    }
}
