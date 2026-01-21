package tech.dokus.features.ai.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.foundation.backend.cache.RedisClient
import java.time.Duration
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class RedisDocumentImageCache(
    private val redisClient: RedisClient,
    private val ttl: kotlin.time.Duration = 30.minutes
) : DocumentImageCache {

    @Serializable
    private data class CacheEntry(
        val pageNumber: Int,
        val mimeType: String,
        val base64: String
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun store(
        documentId: String,
        runId: String?,
        images: List<DocumentImage>
    ): List<DocumentImageCache.ImageRef> {
        val ttlJava = Duration.ofSeconds(ttl.inWholeSeconds)
        val refs = mutableListOf<DocumentImageCache.ImageRef>()
        for (image in images) {
            val imageId = buildId(documentId, runId)
            val entry = CacheEntry(
                pageNumber = image.pageNumber,
                mimeType = image.mimeType,
                base64 = Base64.getEncoder().encodeToString(image.imageBytes)
            )
            redisClient.set(imageId, json.encodeToString(CacheEntry.serializer(), entry), ttlJava)
            refs += DocumentImageCache.ImageRef(pageNumber = image.pageNumber, imageId = imageId)
        }
        return refs
    }

    override suspend fun get(imageId: String): DocumentImage? {
        val raw = redisClient.get(imageId) ?: return null
        val entry = runCatching { json.decodeFromString(CacheEntry.serializer(), raw) }.getOrNull()
            ?: return null
        val bytes = runCatching { Base64.getDecoder().decode(entry.base64) }.getOrNull() ?: return null
        return DocumentImage(pageNumber = entry.pageNumber, imageBytes = bytes, mimeType = entry.mimeType)
    }

    private fun buildId(documentId: String, runId: String?): String {
        val base = UUID.randomUUID().toString()
        return if (runId.isNullOrBlank()) {
            "img_${documentId}_$base"
        } else {
            "img_${documentId}_${runId}_$base"
        }
    }
}
