package com.suilearn.core.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRemoteApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val connectTimeoutMs: Int = 3_000,
    private val readTimeoutMs: Int = 3_000,
) {
    suspend fun getProviderStatus(): AiProviderStatus = withContext(Dispatchers.IO) {
        parseProviderStatus(request(path = "/ai/provider-status"))
    }

    suspend fun listGeneratedContents(status: String? = null): List<GeneratedQuestionDraft> = withContext(Dispatchers.IO) {
        val query = status?.let { "?status=${it.urlEncode()}" }.orEmpty()
        parseGeneratedContents(request(path = "/ai/generated-contents$query"))
    }

    suspend fun getTaskStatus(taskId: String): TaskStatus = withContext(Dispatchers.IO) {
        parseTaskStatus(request(path = "/tasks/${taskId.urlEncode()}"))
    }

    suspend fun reviewGeneratedContent(id: String, status: String): GeneratedQuestionDraft = withContext(Dispatchers.IO) {
        val body = """{"status":"$status"}"""
        parseGeneratedQuestionDraft(
            request(
                path = "/ai/generated-contents/${id.urlEncode()}",
                method = "PATCH",
                body = body,
            )
        )
    }

    private fun request(path: String, method: String = "GET", body: String? = null): String {
        val connection = URL("${baseUrl.trimEnd('/')}$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.setRequestProperty("Accept", "application/json")
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }

        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (statusCode !in 200..299) {
            throw IOException("HTTP $statusCode ${response.take(120)}")
        }
        return response
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/api/v2"

        fun parseProviderStatus(json: String): AiProviderStatus {
            val root = JsonValueParser(json).parse().asObject()
            return AiProviderStatus(
                providerType = root.requiredString("providerType"),
                configured = root.requiredBoolean("configured"),
                available = root.requiredBoolean("available"),
                chatModel = root.optionalString("chatModel"),
                embeddingModel = root.optionalString("embeddingModel"),
                embeddingDimensions = root.optionalInt("embeddingDimensions"),
                message = root.optionalString("message"),
            )
        }

        fun parseGeneratedContents(json: String): List<GeneratedQuestionDraft> =
            JsonValueParser(json).parse().asArray().map { parseGeneratedQuestionDraft(it.asObject()) }

        fun parseGeneratedQuestionDraft(json: String): GeneratedQuestionDraft =
            parseGeneratedQuestionDraft(JsonValueParser(json).parse().asObject())

        fun parseTaskStatus(json: String): TaskStatus {
            val root = JsonValueParser(json).parse().asObject()
            return TaskStatus(
                id = root.requiredString("id"),
                kind = root.requiredString("kind"),
                status = root.requiredString("status"),
                currentStep = root.optionalString("currentStep"),
                errorMessage = root.optionalString("errorMessage"),
            )
        }

        private fun parseGeneratedQuestionDraft(root: Map<String, Any?>): GeneratedQuestionDraft =
            GeneratedQuestionDraft(
                id = root.requiredString("id"),
                knowledgeBaseId = root.requiredString("knowledgeBaseId"),
                generationTaskId = root.optionalString("generationTaskId"),
                status = root.requiredString("status"),
                stem = root.requiredString("stem"),
            )
    }
}

private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

private class JsonValueParser(private val text: String) {
    private var index = 0

    fun parse(): Any? {
        val value = parseValue()
        skipWhitespace()
        if (index != text.length) error("Unexpected trailing content.")
        return value
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        if (index >= text.length) error("Unexpected end of JSON.")
        return when (text[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> parseLiteral("true", true)
            'f' -> parseLiteral("false", false)
            'n' -> parseLiteral("null", null)
            '-', in '0'..'9' -> parseNumber()
            else -> error("Unexpected character '${text[index]}'.")
        }
    }

    private fun parseObject(): Map<String, Any?> {
        expect('{')
        val result = linkedMapOf<String, Any?>()
        skipWhitespace()
        if (peek('}')) {
            index++
            return result
        }
        while (true) {
            val key = parseString()
            expect(':')
            result[key] = parseValue()
            skipWhitespace()
            when {
                peek(',') -> {
                    index++
                    skipWhitespace()
                }
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
            when (val char = text[index++]) {
                '"' -> return result.toString()
                '\\' -> result.append(parseEscape())
                else -> result.append(char)
            }
        }
        error("Unterminated string.")
    }

    private fun parseEscape(): Char {
        if (index >= text.length) error("Unterminated escape sequence.")
        return when (val escaped = text[index++]) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                if (index + 4 > text.length) error("Invalid unicode escape.")
                val hex = text.substring(index, index + 4)
                index += 4
                hex.toInt(16).toChar()
            }
            else -> error("Unsupported escape sequence \\$escaped.")
        }
    }

    private fun parseNumber(): Number {
        val start = index
        if (peek('-')) index++
        while (index < text.length && text[index].isDigit()) index++
        val decimal = index < text.length && text[index] == '.'
        if (decimal) {
            index++
            while (index < text.length && text[index].isDigit()) index++
        }
        if (index < text.length && (text[index] == 'e' || text[index] == 'E')) {
            index++
            if (index < text.length && (text[index] == '+' || text[index] == '-')) index++
            while (index < text.length && text[index].isDigit()) index++
        }
        val raw = text.substring(start, index)
        return if (decimal || raw.contains('e', ignoreCase = true)) raw.toDouble() else raw.toLong()
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

private fun Any?.asObject(): Map<String, Any?> {
    @Suppress("UNCHECKED_CAST")
    return this as? Map<String, Any?> ?: throw IllegalArgumentException("Expected JSON object.")
}

private fun Any?.asArray(): List<Any?> {
    @Suppress("UNCHECKED_CAST")
    return this as? List<Any?> ?: throw IllegalArgumentException("Expected JSON array.")
}

private fun Map<String, Any?>.requiredString(key: String): String =
    this[key] as? String ?: throw IllegalArgumentException("Expected string for key '$key'.")

private fun Map<String, Any?>.requiredBoolean(key: String): Boolean =
    this[key] as? Boolean ?: throw IllegalArgumentException("Expected boolean for key '$key'.")

private fun Map<String, Any?>.optionalString(key: String): String? = this[key] as? String

private fun Map<String, Any?>.optionalInt(key: String): Int? = when (val value = this[key]) {
    null -> null
    is Long -> value.toInt()
    is Int -> value
    is Double -> value.toInt()
    else -> throw IllegalArgumentException("Expected integer for key '$key'.")
}
