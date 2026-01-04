package tech.dokus.domain.utils

import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

@PublishedApi
internal const val JWT_PARTS_COUNT = 3
private const val BASE64_BLOCK_SIZE = 4
private const val BASE64_PADDING_OFFSET = 3

@Throws(IndexOutOfBoundsException::class)
inline fun <reified T : Any> decodeJwtPayload(token: String): T {
    val parts = token.split(".")
    require(parts.size == JWT_PARTS_COUNT) { "Invalid JWT format" }
    val payload = parts[1]
    val decodedBytes = decodeBase64Url(payload)
    val json = decodedBytes.decodeToString()
    return Json.decodeFromString<T>(json)
}

@Throws(IndexOutOfBoundsException::class)
fun decodeBase64Url(str: String): ByteArray {
    val normalized = str.padEnd(
        (str.length + BASE64_PADDING_OFFSET) / BASE64_BLOCK_SIZE * BASE64_BLOCK_SIZE,
        '='
    ).replace('-', '+').replace('_', '/')
    return Base64.decode(normalized)
}
