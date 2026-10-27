package com.usipipo.vpn.widget

/**
 * Estado del widget guardado entre reinicios.
 *
 * Solo guarda el estado y su fecha: nunca el perfil, el país, el endpoint ni las claves.
 */
data class PersistedWidgetState(
    val status: WidgetStatus,
    val updatedAtEpochMs: Long,
)
