package com.suilearn.core.importer

import com.suilearn.core.model.QuestionPack
import com.suilearn.core.model.QuestionPackCategory
import com.suilearn.core.model.QuestionPackKnowledgePoint
import com.suilearn.core.model.QuestionPackOption
import com.suilearn.core.model.QuestionPackQuestion
import com.suilearn.core.model.QuestionType

object QuestionPackJsonParser {
    fun parse(json: String): QuestionPack {
        val root = JsonParser(json).parseObject()
        return QuestionPack(
            schemaVersion = root.int("schemaVersion"),
            packId = root.string("packId"),
            packName = root.string("packName"),
            packVersion = root.int("packVersion"),
            description = root.string("description"),
            categories = root.objects("categories").map {
                QuestionPackCategory(
                    categoryId = it.string("categoryId"),
                    name = it.string("name"),
                    description = it.string("description"),
                    sortOrder = it.int("sortOrder"),
                )
            },
            knowledgePoints = root.objects("knowledgePoints").map {
                QuestionPackKnowledgePoint(
                    knowledgePointId = it.string("knowledgePointId"),
                    categoryId = it.string("categoryId"),
                    name = it.string("name"),
                    description = it.string("description"),
                    sortOrder = it.int("sortOrder"),
                )
            },
            questions = root.objects("questions").map {
                QuestionPackQuestion(
                    questionId = it.string("questionId"),
                    categoryId = it.string("categoryId"),
                    type = QuestionType.valueOf(it.string("type")),
                    stem = it.string("stem"),
                    options = it.objects("options").map { option ->
                        QuestionPackOption(
                            key = option.string("key"),
                            content = option.string("content"),
                        )
                    },
                    answer = it.strings("answer"),
                    explanation = it.string("explanation"),
                    difficulty = it.int("difficulty"),
                    knowledgePointIds = it.strings("knowledgePointIds"),
                    sortOrder = it.int("sortOrder"),
                    deprecated = it.booleanOrDefault("deprecated", false),
                )
            },
        )
    }
}

private class JsonParser(private val text: String) {
    private var index = 0

    fun parseObject(): Map<String, Any?> {
        val value = parseValue()
        skipWhitespace()
        if (index != text.length) error("Unexpected trailing content.")
        return value.asObject()
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        if (index >= text.length) error("Unexpected end of JSON.")
        return when (text[index]) {
            '{' -> parseObjectValue()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> parseLiteral("true", true)
            'f' -> parseLiteral("false", false)
            'n' -> parseLiteral("null", null)
            '-', in '0'..'9' -> parseNumber()
            else -> error("Unexpected character '${text[index]}'.")
        }
    }

    private fun parseObjectValue(): Map<String, Any?> {
        expect('{')
        val result = linkedMapOf<String, Any?>()
        skipWhitespace()
        if (peek('}')) {
            index++
            return result
        }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            result[key] = parseValue()
            skipWhitespace()
            when {
                peek(',') -> index++
                peek('}') -> {
                    index++
                    return result
                }
                else -> error("Expected ',' or '}'.")
            }
        }
    }

    private fun parseArray(): List<Any?> {
        expect('[')
        val result = mutableListOf<Any?>()
        skipWhitespace()
        if (peek(']')) {
            index++
            return result
        }
        while (true) {
            result += parseValue()
            skipWhitespace()
            when {
                peek(',') -> index++
                peek(']') -> {
                    index++
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

    private fun parseNumber(): Number {
        val start = index
        if (peek('-')) index++
        while (index < text.length && text[index].isDigit()) index++
        val isDecimal = index < text.length && text[index] == '.'
        if (isDecimal) {
            index++
            while (index < text.length && text[index].isDigit()) index++
        }
        if (index < text.length && (text[index] == 'e' || text[index] == 'E')) {
            index++
            if (index < text.length && (text[index] == '+' || text[index] == '-')) index++
            while (index < text.length && text[index].isDigit()) index++
        }
        val raw = text.substring(start, index)
        return if (isDecimal || raw.contains('e', ignoreCase = true)) raw.toDouble() else raw.toLong()
    }

    private fun parseLiteral(literal: String, value: Any?): Any? {
        if (!text.startsWith(literal, index)) error("Expected '$literal'.")
        index += literal.length
        return value
    }

    private fun expect(expected: Char) {
        skipWhitespace()
        if (index >= text.length || text[index] != expected) error("Expected '$expected'.")
        index++
    }

    private fun peek(char: Char): Boolean = index < text.length && text[index] == char

    private fun skipWhitespace() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    private fun error(message: String): Nothing {
        throw IllegalArgumentException("$message At index $index.")
    }
}

private fun Map<String, Any?>.required(key: String): Any? =
    if (containsKey(key)) get(key) else throw IllegalArgumentException("Missing required key '$key'.")

private fun Map<String, Any?>.string(key: String): String = required(key) as? String
    ?: throw IllegalArgumentException("Expected string for key '$key'.")

private fun Map<String, Any?>.int(key: String): Int = when (val value = required(key)) {
    is Long -> value.toInt()
    is Int -> value
    else -> throw IllegalArgumentException("Expected integer for key '$key'.")
}

private fun Map<String, Any?>.booleanOrDefault(key: String, default: Boolean): Boolean =
    when (val value = get(key)) {
        null -> default
        is Boolean -> value
        else -> throw IllegalArgumentException("Expected boolean for key '$key'.")
    }

private fun Map<String, Any?>.objects(key: String): List<Map<String, Any?>> =
    list(key).map { it.asObject() }

private fun Map<String, Any?>.strings(key: String): List<String> =
    list(key).map {
        it as? String ?: throw IllegalArgumentException("Expected string item in '$key'.")
    }

private fun Map<String, Any?>.list(key: String): List<Any?> = required(key) as? List<Any?>
    ?: throw IllegalArgumentException("Expected array for key '$key'.")

private fun Any?.asObject(): Map<String, Any?> {
    @Suppress("UNCHECKED_CAST")
    return this as? Map<String, Any?>
        ?: throw IllegalArgumentException("Expected JSON object.")
}
