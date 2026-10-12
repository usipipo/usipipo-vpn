package com.usipipo.vpn.update

import com.usipipo.vpn.diagnostics.AppLog
import kotlinx.coroutines.CancellationException

/**
 * Decide si hay que avisar al usuario.
 *
 * El reloj y las dependencias se inyectan, así que las pruebas no tocan red ni calendario.
 * Reglas: throttle de [THROTTLE_MS] salvo con `force`, fallo siempre silencioso (solo registro),
 * y las versiones pospuestas no se vuelven a enseñar.
 */
class UpdateChecker(
    private val source: ReleaseSource,
    private val store: UpdateStateStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun check(currentVersion: String, force: Boolean = false): UpdateStatus {
        val installed = Version.parse(currentVersion)
        if (installed == null) {
            AppLog.w(TAG, "Versión instalada no reconocida: «$currentVersion»")
            return UpdateStatus.Failed
        }

        val now = clock()
        if (!force && isThrottled(now)) {
            AppLog.d(TAG, "Comprobación automática frenada por el throttle de 24 h")
            return UpdateStatus.SkippedThrottled
        }

        val release = try {
            source.latest()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w(TAG, "No se pudo consultar la última release", e)
            null
        }

        // Sin release no se toca la marca: se reintenta en el siguiente arranque.
        if (release == null) {
            AppLog.w(TAG, "No hay release que comparar con la versión instalada")
            return UpdateStatus.Failed
        }

        store.lastCheckAt = clock()

        return when {
            release.version <= installed -> UpdateStatus.UpToDate
            isSkipped(release.version) -> UpdateStatus.Postponed
            else -> UpdateStatus.UpdateAvailable(release)
        }
    }

    private fun isThrottled(now: Long): Boolean {
        val last = store.lastCheckAt
        return last > 0L && now - last < THROTTLE_MS
    }

    private fun isSkipped(version: Version): Boolean {
        val skipped = Version.parse(store.skippedVersion) ?: return false
        return skipped == version
    }

    private companion object {
        const val TAG = "Actualizar"
        const val THROTTLE_MS = 24 * 60 * 60 * 1000L
    }
}
