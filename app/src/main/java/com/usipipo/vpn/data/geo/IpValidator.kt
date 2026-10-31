package com.usipipo.vpn.data.geo

/**
 * Valida que un texto sea una IP completa: IPv4 en notación de cuatro octetos o
 * IPv6 literal.
 *
 * Código puro (sin Android ni red) para poder validarla antes de cualquier
 * petición: una IP cortada, con puerto, con corchetes o un dominio no debe
 * llegar nunca al proveedor.
 *
 * No se aceptan las formas ambiguas ni las que un proveedor interpretaría de
 * otra manera: ceros a la izquierda (`01.2.3.4`), CIDR (`1.2.3.4/24`), zona
 * IPv6 (`fe80::1%eth0`) y IPv6 con IPv4 embebido (`::ffff:192.0.2.1`).
 */
object IpValidator {

    /** true si [ip] es una IPv4 o IPv6 completa y válida. */
    fun isValid(ip: String): Boolean = invalidReason(ip) == null

    /** Motivo por el que [ip] no es válida, o null si sí lo es. */
    fun invalidReason(ip: String): String? {
        if (ip.isBlank()) return "está vacía"
        if (ip != ip.trim()) return "tiene espacios"
        if (ip.contains('/')) return "incluye un CIDR"
        if (ip.contains('[') || ip.contains(']')) return "incluye corchetes"
        if (ip.contains('%')) return "incluye una zona IPv6"

        val colons = ip.count { it == ':' }
        if (colons == 1) return "parece host:puerto"
        if (colons == 0) return if (isIpv4(ip)) null else "no es una IPv4 válida"
        return if (isIpv6(ip)) null else "no es una IPv6 válida"
    }

    /** IPv4 de cuatro octetos decimales entre 0 y 255, sin ceros a la izquierda. */
    private fun isIpv4(text: String): Boolean {
        val parts = text.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() &&
                part.length <= 3 &&
                part.all { it in '0'..'9' } &&
                (part.length == 1 || part[0] != '0') &&
                part.toInt() <= 255
        }
    }

    /**
     * IPv6 literal: como mucho un `::` y ocho grupos hexadecimales. El `::`
     * representa uno o más grupos omitidos, así que la suma de grupos explícitos
     * debe ser menor que ocho.
     */
    private fun isIpv6(text: String): Boolean {
        val halves = text.split("::")
        if (halves.size > 2) return false

        val head = halves[0]
        val tail = halves.getOrNull(1)
        val headGroups = groups(head) ?: return false
        val tailGroups = if (tail == null) emptyList() else (groups(tail) ?: return false)

        val total = headGroups.size + tailGroups.size
        return if (tail == null) total == 8 else total <= 7
    }

    /** Grupos hexadecimales de una parte de una IPv6, o null si no lo son. */
    private fun groups(part: String): List<String>? {
        if (part.isEmpty()) return emptyList()
        val list = part.split(':')
        if (list.any { it.isEmpty() }) return null
        if (list.any { group ->
                group.length > 4 || group.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }
            }
        ) {
            return null
        }
        return list
    }
}
