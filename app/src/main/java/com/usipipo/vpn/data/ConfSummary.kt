package com.usipipo.vpn.data

/** Lo único que la interfaz necesita saber de un `.conf`: nunca claves. */
data class ConfSummary(val endpoint: String?, val routesIpv6: Boolean) {

    companion object {
        fun parse(conf: String): ConfSummary {
            var endpoint: String? = null
            var routesIpv6 = false
            conf.lineSequence().forEach { raw ->
                val separator = raw.indexOf('=')
                if (separator < 0) return@forEach
                val key = raw.substring(0, separator).trim().lowercase()
                val value = raw.substring(separator + 1).trim()
                when (key) {
                    "endpoint" -> if (endpoint == null) endpoint = value
                    "allowedips" -> if (value.split(',').any { it.trim() == "::/0" }) routesIpv6 = true
                }
            }
            return ConfSummary(endpoint = endpoint, routesIpv6 = routesIpv6)
        }
    }
}
