package com.usipipo.vpn.ui.configs

import com.usipipo.vpn.domain.SavedConfig

enum class ConfigsTab { Mine, Favorites }

fun List<SavedConfig>.forTab(tab: ConfigsTab): List<SavedConfig> =
    if (tab == ConfigsTab.Favorites) filter { it.favorite } else this
