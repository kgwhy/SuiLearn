package com.suilearn.core.database

internal fun encodeStringListAsJson(items: List<String>): String =
    items.joinToString(prefix = "[", postfix = "]") { "\"${it.escapeJsonString()}\"" }

internal fun decodeStringListFromJson(json: String): List<String> {
    val parser = JsonStringArrayParser(json)
    return parser.parse()
}

private fun String.escapeJsonString(): String = buildString {
    for (char in this@escapeJsonString) {
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
}

private class JsonStringArrayParser(private val text: String) {
    private var index = 0

    fun parse(): List<String> {
        skipWhitespace()
        expect('[')
        val result = mutableListOf<String>()
        skipWhitespace()
        if (peek(']')) {
            index++
            ensureEnd()
            return result
        }
        while (true) {
            result += parseString()
            skipWhitespace()
            when {
                peek(',') -> {
                    index++
                    skipWhitespace()
                }
                peek(']') -> {
                    index++
                    ensureEnd()
                    return result
                }
                else -> error("Expected ',' or ']'.")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val result = StringBuilder()
        while (index < text.length) {
            val char = text[index++]
            when (char) {
                '"' -> return result.toString()
                '\\' -> {
                    if (index >= text.length) error("Unterminated escape sequence.")
                    result.append(
                        when (val escaped = text[index++]) {
                            '"' -> '"'
                            '\\' -> '\\'
                            '/' -> '/'
                            'b' -> '\b'
                            'f' -> '\u000C'
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            'u' -> parseUnicodeEscape()
                            else -> error("Unsupported escape sequence \\$escaped.")
                        }
                    )
                }
                else -> result.append(char)
            }
        }
        error("Unterminated string.")
    }

    private fun parseUnicodeEscape(): Char {
        if (index + 4 > text.length) error("Invalid unicode escape.")
        val hex = text.substring(index, index + 4)
        index += 4
        return hex.toInt(16).toChar()
    }

    private fun expect(expected: Char) {
        skipWhitespace()
        if (!peek(expected)) error("Expected '$expected'.")
        index++
    }

    private fun peek(char: Char): Boolean = index < text.length && text[index] == char

    private fun skipWhitespace() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    private fun ensureEnd() {
        skipWhitespace()
        if (index != text.length) error("Unexpected trailing content.")
    }

    private fun error(message: String): Nothing {
        throw IllegalArgumentException("$message At index $index.")
    }
}
