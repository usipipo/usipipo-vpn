package com.usipipo.vpn.tunnel

/**
 * El sistema ha denegado el arranque de un servicio en primer plano desde segundo plano.
 *
 * Android 12+ lanza `ForegroundServiceStartNotAllowedException` cuando una app en reposo
 * intenta arrancar un servicio. El arranque del servicio traduce ese error a esta excepción
 * para que el resto de la app no dependa de clases de Android y la denegación se pueda
 * distinguir de cualquier otro fallo.
 */
class BackgroundStartDeniedException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
