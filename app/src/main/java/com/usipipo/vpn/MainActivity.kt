package com.usipipo.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.usipipo.vpn.ui.UsipipoRoot
import com.usipipo.vpn.ui.theme.UsipipoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as UsipipoApp).container
        setContent {
            UsipipoTheme {
                UsipipoRoot(container)
            }
        }
    }
}
