package com.usipipo.vpn.ui.settings.appearance

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.settings.PaletteVariant
import com.usipipo.vpn.settings.ThemeMode
import com.usipipo.vpn.settings.tokens
import com.usipipo.vpn.ui.components.UsipipoCard
import com.usipipo.vpn.ui.theme.UsipipoTheme

/**
 * Subpantalla de Ajustes: modo de tema, animaciones y paleta de color.
 *
 * Recibe valores planos en lugar de un ViewModel para poder previsualizarla sin
 * depender del ciclo de vida. Cada cambio se avisa con su callback.
 */
@Composable
fun AppearanceScreen(
    themeMode: ThemeMode,
    animationsEnabled: Boolean,
    palette: PaletteVariant,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onPaletteChange: (PaletteVariant) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Cabecera(onBack)
            TarjetaTema(themeMode, onThemeModeChange)
            TarjetaAnimaciones(animationsEnabled, onAnimationsChange)
            TarjetaPaleta(palette, onPaletteChange)
        }
    }
}

@Composable
private fun Cabecera(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.cd_back),
            )
        }
        Text(
            text = stringResource(R.string.appearance_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun TarjetaTema(themeMode: ThemeMode, onChange: (ThemeMode) -> Unit) {
    UsipipoCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            TituloGrupo(stringResource(R.string.appearance_theme))
            OpcionTema(
                label = stringResource(R.string.appearance_theme_system),
                selected = themeMode == ThemeMode.System,
                onClick = { onChange(ThemeMode.System) },
            )
            OpcionTema(
                label = stringResource(R.string.appearance_theme_light),
                selected = themeMode == ThemeMode.Light,
                onClick = { onChange(ThemeMode.Light) },
            )
            OpcionTema(
                label = stringResource(R.string.appearance_theme_dark),
                selected = themeMode == ThemeMode.Dark,
                onClick = { onChange(ThemeMode.Dark) },
            )
        }
    }
}

@Composable
private fun OpcionTema(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.appearance_selected),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TarjetaAnimaciones(enabled: Boolean, onChange: (Boolean) -> Unit) {
    UsipipoCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onChange)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.appearance_animations),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.appearance_animations_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            // El toggle lo gestiona la fila entera (accesibilidad): el Switch es solo visual.
            Switch(checked = enabled, onCheckedChange = null)
        }
    }
}

@Composable
private fun TarjetaPaleta(palette: PaletteVariant, onChange: (PaletteVariant) -> Unit) {
    UsipipoCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            TituloGrupo(stringResource(R.string.appearance_palette))
            OpcionPaleta(
                label = stringResource(R.string.appearance_palette_azul_noche),
                color = Color(PaletteVariant.AzulNoche.tokens(isDark = false).primary),
                selected = palette == PaletteVariant.AzulNoche,
                onClick = { onChange(PaletteVariant.AzulNoche) },
            )
            OpcionPaleta(
                label = stringResource(R.string.appearance_palette_verde_bosque),
                color = Color(PaletteVariant.VerdeBosque.tokens(isDark = false).primary),
                selected = palette == PaletteVariant.VerdeBosque,
                onClick = { onChange(PaletteVariant.VerdeBosque) },
            )
            OpcionPaleta(
                label = stringResource(R.string.appearance_palette_ambar),
                color = Color(PaletteVariant.Ambar.tokens(isDark = false).primary),
                selected = palette == PaletteVariant.Ambar,
                onClick = { onChange(PaletteVariant.Ambar) },
            )
        }
    }
}

@Composable
private fun OpcionPaleta(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TituloGrupo(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Preview(name = "Apariencia claro", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Apariencia oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppearanceScreenPreview() {
    UsipipoTheme {
        AppearanceScreen(
            themeMode = ThemeMode.System,
            animationsEnabled = true,
            palette = PaletteVariant.AzulNoche,
            onThemeModeChange = {},
            onAnimationsChange = {},
            onPaletteChange = {},
            onBack = {},
        )
    }
}
