package org.unishiasy.vehiclelog.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

// The tree parser overwrites duplicate keys. Check syntax, key uniqueness and nesting first.
internal class StrictBackupJson(private val source: String) {
    private var offset = 0
    private var values = 0

    fun validate() {
        value(0)
        syntax(peek() == null)
    }

    private fun value(depth: Int) {
        checkSnapshot(++values <= MAX_VALUES, BackupError.CollectionLimit)
        when (peek()) {
            '{' -> objectValue(depth)
            '[' -> arrayValue(depth)
            '"' -> stringToken()
            else -> {
                val start = offset
                while (offset < source.length && source[offset] !in " \t\r\n,]}") offset++
                syntax(scalar.matches(source.subSequence(start, offset)))
            }
        }
    }

    private fun objectValue(depth: Int) {
        syntax(depth < MAX_DEPTH && take('{'))
        if (take('}')) return
        val keys = HashSet<String>()
        do {
            syntax(peek() == '"')
            val start = offset
            val escaped = stringToken()
            val token = source.substring(start, offset)
            val key = if (escaped) (Json.parseToJsonElement(token) as JsonPrimitive).content
                else token.substring(1, token.lastIndex)
            checkSnapshot(keys.add(key), BackupError.DuplicateField)
            checkSnapshot(keys.size <= MAX_OBJECT_FIELDS, BackupError.UnknownField)
            syntax(take(':'))
            value(depth + 1)
        } while (take(','))
        syntax(take('}'))
    }

    private fun arrayValue(depth: Int) {
        syntax(depth < MAX_DEPTH && take('['))
        if (take(']')) return
        do {
            value(depth + 1)
        } while (take(','))
        syntax(take(']'))
    }

    private fun stringToken(): Boolean {
        offset++ // Opening quote.
        var escaped = false
        while (offset < source.length) {
            val character = source[offset++]
            if (character == '"') return escaped
            syntax(character >= ' ')
            if (character == '\\') {
                escaped = true
                syntax(offset < source.length)
                when (source[offset++]) {
                    '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> Unit
                    'u' -> repeat(4) {
                        syntax(offset < source.length && source[offset] in "0123456789abcdefABCDEF")
                        offset++
                    }
                    else -> syntax(false)
                }
            }
        }
        throw InvalidSnapshot(BackupError.MalformedJson)
    }

    private fun take(character: Char): Boolean {
        if (peek() != character) return false
        offset++
        return true
    }

    private fun peek(): Char? {
        while (offset < source.length && source[offset] in " \t\r\n") offset++
        return source.getOrNull(offset)
    }

    private fun syntax(valid: Boolean) = checkSnapshot(valid, BackupError.MalformedJson)

    private companion object {
        const val MAX_DEPTH = 3
        const val MAX_VALUES = 350_000
        const val MAX_OBJECT_FIELDS = 16
        val scalar = Regex("true|false|null|-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")
    }
}
