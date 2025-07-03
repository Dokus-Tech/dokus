package ai.thepredict.domain.utils

import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

@Throws
inline fun <reified T : Any> decodeJwtPayload(token: String): T {
    val parts = token.split(".")
    require(parts.size == 3) { "Invalid JWT format" }
    val payload = parts[1]
    val decodedBytes = decodeBase64Url(payload)
    val json = decodedBytes.decodeToString()
    return Json.decodeFromString<T>(json)
}

// Multiplatform Base64Url decoder (no padding, URL-safe)
@Throws
fun decodeBase64Url(str: String): ByteArray {
    val normalized = str.padEnd((str.length + 3) / 4 * 4, '=')
        .replace('-', '+').replace('_', '/')
    return Base64.decode(normalized)
}