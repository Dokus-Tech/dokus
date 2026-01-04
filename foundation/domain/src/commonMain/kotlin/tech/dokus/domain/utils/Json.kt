package tech.dokus.domain.utils

import kotlinx.serialization.json.Json

val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = false
    prettyPrint = true
}

inline fun <reified T> parseSafe(value: String): Result<T> {
    return runCatching { json.decodeFromString<T>(value) }
}