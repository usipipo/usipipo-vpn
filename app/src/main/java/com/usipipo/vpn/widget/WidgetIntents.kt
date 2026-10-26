package com.usipipo.vpn.widget

import android.content.Context
import android.content.Intent
import com.usipipo.vpn.MainActivity
import com.usipipo.vpn.ui.nav.MainTab

object WidgetIntents {
    const val ACTION_TOGGLE: String = "com.usipipo.vpn.widget.action.TOGGLE"

    /** Intent hacia la app en la pestaña Importar; quien lo recibe decide cómo abrirlo. */
    fun importerIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_INITIAL_TAB, MainTab.Import.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    fun openImporter(context: Context): Unit {
        context.startActivity(importerIntent(context))
    }
}
