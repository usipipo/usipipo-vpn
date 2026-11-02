package com.usipipo.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.usipipo.vpn.settings.darkTheme
import com.usipipo.vpn.ui.UsipipoRoot
import com.usipipo.vpn.ui.nav.MainTab
import com.usipipo.vpn.ui.theme.UsipipoTheme
import com.usipipo.vpn.ui.theme.rememberAppearanceState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as UsipipoApp).container
        val initialTab = MainTab.fromKey(intent.getStringExtra(EXTRA_INITIAL_TAB))
        setContent {
            // El aspecto visual vive en el almacen: cada cambio se aplica de inmediato.
            val appearance = rememberAppearanceState(container.appSettings)
            UsipipoTheme(
                darkTheme = appearance.themeMode.darkTheme(isSystemInDarkTheme()),
                palette = appearance.palette,
                animationsEnabled = appearance.animationsEnabled,
            ) {
                UsipipoRoot(container, initialTab)
            }
        }
    }

    companion object {
        const val EXTRA_INITIAL_TAB: String = "com.usipipo.vpn.extra.INITIAL_TAB"
    }
}
