package com.usipipo.vpn.domain

/** La última usada si sigue existiendo; si no, la primera de la lista. */
fun selectedConfigId(configs: List<SavedConfig>, lastUsedId: String?): String? =
    configs.firstOrNull { it.id == lastUsedId }?.id ?: configs.firstOrNull()?.id
