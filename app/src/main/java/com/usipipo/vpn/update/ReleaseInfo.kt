package com.usipipo.vpn.update

/** Lo que la app necesita saber del último release publicado en GitHub. */
data class ReleaseInfo(
    val version: Version,
    val htmlUrl: String,
    val name: String?,
)
