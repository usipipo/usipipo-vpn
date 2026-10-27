package com.usipipo.vpn.widget

/** Guarda el último estado del widget para poder contrastarlo al arrancar. */
interface WidgetStateStore {
    fun read(): PersistedWidgetState?

    fun write(state: PersistedWidgetState)
}
