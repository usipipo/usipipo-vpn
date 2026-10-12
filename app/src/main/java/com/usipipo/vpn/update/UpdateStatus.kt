package com.usipipo.vpn.update

/** Resultado de una comprobación de actualizaciones. La UI solo consume este sellado. */
sealed interface UpdateStatus {

    /** La versión instalada es la última (o más nueva) que hay publicada. */
    data object UpToDate : UpdateStatus

    /** Hay una versión más nueva publicada. */
    data class UpdateAvailable(val info: ReleaseInfo) : UpdateStatus

    /** Hay versión más nueva, pero el usuario ya pospuso esa concreta. */
    data object Postponed : UpdateStatus

    /** No se pudo comprobar (sin red, HTTP distinto de 200, JSON inesperado...). */
    data object Failed : UpdateStatus

    /** Comprobación automática frenada por el throttle de 24 h. */
    data object SkippedThrottled : UpdateStatus
}
