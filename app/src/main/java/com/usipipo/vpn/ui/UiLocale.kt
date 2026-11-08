package com.usipipo.vpn.ui

import java.util.Locale

/**
 * Idioma con el que la app da formato a sus textos.
 *
 * La app solo está en español: no hay `values-en` ni `localeConfig`, así que las
 * etiquetas salen siempre en español caiga lo que caiga. Los números tienen que
 * acompañarlas —con `Locale.getDefault()`, un móvil en inglés mostraba «1.0 KB»
 * al lado de «Almacenamiento»—.
 *
 * El día que se añada otro idioma, esto se sustituye por la locale con la que
 * Android resuelve los recursos de la app, y los tests dejan de fijarla.
 */
val ESPANOL: Locale = Locale.forLanguageTag("es-ES")
