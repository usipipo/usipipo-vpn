package com.usipipo.vpn.ui.configs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.home.HomeViewModel

@Composable
fun ConfigsScreen(vm: HomeViewModel, onImport: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.nav_configs), color = MaterialTheme.colorScheme.onBackground)
    }
}
