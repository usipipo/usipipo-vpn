package com.usipipo.vpn.update

/**
 * Lee un release de GitHub del JSON devuelto por `/repos/{owner}/{repo}/releases/latest`.
 * Es Kotlin puro (sin `org.json`) para poder probarlo en la JVM.
 *
 * Solo lee `tag_name`, `html_url` y `name`; el resto del documento se ignora.
 */
object ReleaseParser {

    fun parse(json: String): ReleaseInfo? {
        val root = JsonReader(json).readObjectOrNull() ?: return null
        val tag = root["tag_name"] as? String ?: return null
        val htmlUrl = root["html_url"] as? String ?: return null
        if (htmlUrl.isBlank()) return null
        val version = Version.parse(tag) ?: return null
        return ReleaseInfo(version = version, htmlUrl = htmlUrl, name = root["name"] as? String)
    }
}

/** Lector mínimo de JSON: objetos, arrays, strings con escapes, números y constantes. */
private class JsonReader(private val text: String) {

    private class MalformedJson : Exception()

    private var pos = 0

    fun readObjectOrNull(): Map<String, Any?>? {
        pos = 0
        return try {
            skipBlank()
            if (peek() != '{') return null
            val value = readObject()
            skipBlank()
            if (pos != text.length) return null
            value
        } catch (e: MalformedJson) {
            null
        }
    }

    private fun readValue(): Any? = when (peek()) {
        '{' -> readObject()
        '[' -> readArray()
        '"' -> readString()
        't' -> readLiteral("true", true)
        'f' -> readLiteral("false", false)
        'n' -> readLiteral("null", null)
        else -> readNumber()
    }

    private fun readObject(): Map<String, Any?> {
        expect('{')
        val out = linkedMapOf<String, Any?>()
        skipBlank()
        if (peek() == '}') {
            pos++
            return out
        }
        while (true) {
            skipBlank()
            val key = readString() ?: throw MalformedJson()
            skipBlank()
            expect(':')
            skipBlank()
            out[key] = readValue()
            skipBlank()
            when (peek()) {
                ',' -> pos++
                '}' -> {
                    pos++
                    return out
                }
                else -> throw MalformedJson()
            }
        }
    }

    private fun readArray(): List<Any?> {
        expect('[')
        val out = mutableListOf<Any?>()
        skipBlank()
        if (peek() == ']') {
            pos++
            return out
        }
        while (true) {
            skipBlank()
            out += readValue()
            skipBlank()
            when (peek()) {
                ',' -> pos++
                ']' -> {
                    pos++
                    return out
                }
                else -> throw MalformedJson()
            }
        }
    }

    private fun readString(): String? {
        if (peek() != '"') return null
        pos++
        val sb = StringBuilder()
        while (true) {
            when (val c = peek()) {
                null -> throw MalformedJson()
                '"' -> {
                    pos++
                    return sb.toString()
                }
                '\\' -> {
                    pos++
                    when (val escape = peek()) {
                        '"', '\\', '/' -> sb.append(escape)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val code = text.drop(pos + 1).take(4).toIntOrNull(16)
                                ?: throw MalformedJson()
                            sb.append(code.toChar())
                            pos += 4
                        }
                        else -> throw MalformedJson()
                    }
                    pos++
                }
                else -> {
                    sb.append(c)
                    pos++
                }
            }
        }
    }

    private fun readNumber(): Any? {
        val start = pos
        if (peek() == '-' || peek() == '+') pos++
        while (true) {
            when (peek()) {
                in '0'..'9', '.', 'e', 'E', '-', '+' -> pos++
                else -> break
            }
        }
        if (start == pos) throw MalformedJson()
        return text.substring(start, pos).toDoubleOrNull() ?: throw MalformedJson()
    }

    private fun readLiteral(word: String, value: Any?): Any? {
        if (!text.startsWith(word, pos)) throw MalformedJson()
        pos += word.length
        return value
    }

    private fun expect(char: Char) {
        if (peek() != char) throw MalformedJson()
        pos++
    }

    private fun peek(): Char? = text.getOrNull(pos)

    private fun skipBlank() {
        while (pos < text.length && text[pos].isWhitespace()) pos++
    }
}
