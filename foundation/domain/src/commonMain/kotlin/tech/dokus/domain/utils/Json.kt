package tech.dokus.domain.utils

import kotlinx.serialization.json.Json

val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = false
    prettyPrint = true
}