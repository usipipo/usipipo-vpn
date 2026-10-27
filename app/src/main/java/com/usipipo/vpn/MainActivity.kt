package com.usipipo.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.usipipo.vpn.ui.UsipipoRoot
import com.usipipo.vpn.ui.nav.MainTab
import com.usipipo.vpn.ui.theme.UsipipoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as UsipipoApp).container
        val initialTab = MainTab.fromKey(intent.getStringExtra(EXTRA_INITIAL_TAB))
        setContent {
            UsipipoTheme {
                UsipipoRoot(container, initialTab)
            }
        }
    }

    companion object {
        const val EXTRA_INITIAL_TAB: String = "com.usipipo.vpn.extra.INITIAL_TAB"
    }
}
