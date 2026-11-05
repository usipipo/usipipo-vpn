package com.usipipo.vpn.notifications

/**
 * Decide si un aviso propio se publica. Función pura, sin Android: se prueba en la JVM.
 *
 * Reglas del spec de Notificaciones (RF-1, RF-6 y RF-8): el interruptor general
 * silencia todos los avisos propios, cada tipo de aviso tiene el suyo, y sin el
 * permiso de notificaciones no se publica nada (la app no falla).
 */
internal object NoticePolicy {

    fun allowed(master: Boolean, type: Boolean, permissionGranted: Boolean): Boolean =
        master && type && permissionGranted
}
