package com.usipipo.vpn.ui.nav

/** Las cuatro pestañas de la barra inferior. */
enum class MainTab { Home, Configs, Import, Diagnostics }

/** Destino del botón Atrás: Inicio desde cualquier otra pestaña; null en Inicio (sale de la app). */
fun backTarget(tab: MainTab): MainTab? = if (tab == MainTab.Home) null else MainTab.Home
