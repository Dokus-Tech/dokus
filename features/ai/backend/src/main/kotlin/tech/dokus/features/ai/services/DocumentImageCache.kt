package tech.dokus.features.ai.services

import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Cache for document images used by vision tools.
 *
 * Stores rendered PNG bytes keyed by an opaque ID to avoid sending
 * base64 image data through the orchestrator prompt.
 */
interface DocumentImageCache {
    data class ImageRef(
        val pageNumber: Int,
        val imageId: String
    )

    suspend fun store(
        documentId: String,
        runId: String?,
        images: List<DocumentImage>
    ): List<ImageRef>

    suspend fun get(imageId: String): DocumentImage?
}

class InMemoryDocumentImageCache(
    private val clock: Clock = Clock.System,
    private val ttl: Duration = 30.minutes
) : DocumentImageCache {
    private data class CachedImage(
        val image: DocumentImage,
        val expiresAt: Instant
    )

    private val cache = ConcurrentHashMap<String, CachedImage>()

    override suspend fun store(
        documentId: String,
        runId: String?,
        images: List<DocumentImage>
    ): List<DocumentImageCache.ImageRef> {
        pruneExpired()
        return images.map { image ->
            val imageId = buildId(documentId, runId)
            val expiresAt = Instant.fromEpochMilliseconds(
                clock.now().toEpochMilliseconds() + ttl.inWholeMilliseconds
            )
            cache[imageId] = CachedImage(image = image, expiresAt = expiresAt)
            DocumentImageCache.ImageRef(pageNumber = image.pageNumber, imageId = imageId)
        }
    }

    override suspend fun get(imageId: String): DocumentImage? {
        val cached = cache[imageId] ?: return null
        if (cached.expiresAt <= clock.now()) {
            cache.remove(imageId)
            return null
        }
        return cached.image
    }

    private fun pruneExpired() {
        val now = clock.now()
        cache.entries.forEach { entry ->
            if (entry.value.expiresAt <= now) {
                cache.remove(entry.key)
            }
        }
    }

    private fun buildId(documentId: String, runId: String?): String {
        val base = Uuid.random().toString()
        return if (runId.isNullOrBlank()) {
            "img_${documentId}_$base"
        } else {
            "img_${documentId}_${runId}_$base"
        }
    }
}
