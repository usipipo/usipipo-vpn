package com.usipipo.vpn.ui.diagnostics

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.components.UsipipoCard
import com.usipipo.vpn.ui.theme.UsipipoTheme

/** La pista de accesos rápidos solo tiene sentido desde Android 13 (mosaicos más accesibles). */
fun shouldShowQuickAccessHint(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU

/** Tarjeta que explica cómo añadir el widget y el mosaico de uSipipo VPN. */
@Composable
fun QuickAccessHint() {
    UsipipoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_access_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HintStep(number = 1, text = stringResource(R.string.quick_access_widget_hint))
            HintStep(number = 2, text = stringResource(R.string.quick_access_tile_hint))
        }
    }
}

@Composable
private fun HintStep(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(name = "Pista de accesos rápidos")
@Preview(name = "Pista de accesos rápidos oscura", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewQuickAccessHint() {
    UsipipoTheme {
        Surface {
            QuickAccessHint()
        }
    }
}
