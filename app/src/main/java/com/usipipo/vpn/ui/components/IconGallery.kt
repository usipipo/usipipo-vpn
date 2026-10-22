package com.usipipo.vpn.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.theme.UsipipoTheme

/**
 * Galería del set de iconos propio, para revisarlos de un vistazo en claro y en oscuro.
 * Los cuatro primeros son los de la barra inferior; el resto, los de acciones.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconGallery(modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(
            R.drawable.ic_nav_home, R.drawable.ic_nav_configs,
            R.drawable.ic_nav_import, R.drawable.ic_nav_diagnostics,
            R.drawable.ic_arrow_right, R.drawable.ic_arrow_back,
            R.drawable.ic_add, R.drawable.ic_star, R.drawable.ic_star_filled,
            R.drawable.ic_edit, R.drawable.ic_delete, R.drawable.ic_qr,
            R.drawable.ic_clock, R.drawable.ic_download, R.drawable.ic_upload,
            R.drawable.ic_server, R.drawable.ic_import,
            R.drawable.ic_power, R.drawable.ic_update,
            R.drawable.ic_shield_check, R.drawable.ic_empty_shield,
        ).forEach { res ->
            Icon(painterResource(res), contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

@Preview(name = "Set de iconos")
@Preview(name = "Set de iconos oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewIconGallery() {
    UsipipoTheme {
        Surface {
            IconGallery()
        }
    }
}
