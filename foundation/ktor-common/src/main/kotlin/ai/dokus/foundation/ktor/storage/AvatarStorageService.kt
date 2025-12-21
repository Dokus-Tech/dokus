package ai.dokus.foundation.ktor.storage

import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.CompanyAvatar
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Service for handling company avatar uploads with automatic resizing.
 *
 * Generates multiple sizes for different display contexts:
 * - small: 64x64 for compact displays (navigation, lists)
 * - medium: 128x128 for standard displays (cards, tiles)
 * - large: 256x256 for detailed displays (settings, invoices)
 *
 * All images are converted to WebP for optimal file size.
 */
class AvatarStorageService(
    private val storage: ObjectStorage,
    private val defaultUrlExpiry: Duration = 24.hours // Longer expiry for avatars
) {
    private val logger = LoggerFactory.getLogger(AvatarStorageService::class.java)

    companion object {
        val AVATAR_SIZES = mapOf(
            "small" to 64,
            "medium" to 128,
            "large" to 256
        )

        private const val AVATAR_PREFIX = "avatars"
        private const val CONTENT_TYPE_WEBP = "image/webp"
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB

        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
        )
    }

    /**
     * Result of avatar upload operation.
     */
    data class AvatarUploadResult(
        val storageKeyPrefix: String,
        val avatar: CompanyAvatar,
        val sizeBytes: Long
    )

    /**
     * Upload an avatar image, generating multiple sizes.
     *
     * @param tenantId The tenant this avatar belongs to
     * @param imageData The original image bytes
     * @param contentType The MIME type of the original image
     * @return Upload result with storage key prefix and presigned URLs
     * @throws IllegalArgumentException if validation fails
     */
    suspend fun uploadAvatar(
        tenantId: TenantId,
        imageData: ByteArray,
        contentType: String
    ): AvatarUploadResult = withContext(Dispatchers.IO) {
        logger.info("Uploading avatar for tenant: $tenantId, size=${imageData.size}, contentType=$contentType")

        // Validate input
        validateImage(imageData, contentType)

        // Generate unique key prefix for this avatar
        val uuid = UUID.randomUUID().toString()
        val keyPrefix = "$AVATAR_PREFIX/$tenantId/$uuid"

        // Load and process image
        val originalImage = ImmutableImage.loader().fromBytes(imageData)

        // Generate and upload each size
        val urls = mutableMapOf<String, String>()
        var totalBytes = 0L

        for ((sizeName, dimension) in AVATAR_SIZES) {
            val resizedImage = originalImage.cover(dimension, dimension)

            val webpData = resizedImage.bytes(WebpWriter.DEFAULT)

            val key = "${keyPrefix}_$sizeName.webp"
            storage.put(key, webpData, CONTENT_TYPE_WEBP)

            val url = storage.getSignedUrl(key, defaultUrlExpiry)
            urls[sizeName] = url
            totalBytes += webpData.size

            logger.debug("Uploaded avatar size: $sizeName, key=$key, bytes=${webpData.size}")
        }

        val avatar = CompanyAvatar(
            small = urls["small"]!!,
            medium = urls["medium"]!!,
            large = urls["large"]!!
        )

        logger.info("Avatar uploaded successfully: tenant=$tenantId, keyPrefix=$keyPrefix, totalBytes=$totalBytes")

        AvatarUploadResult(
            storageKeyPrefix = keyPrefix,
            avatar = avatar,
            sizeBytes = totalBytes
        )
    }

    /**
     * Get fresh presigned URLs for an existing avatar.
     *
     * @param storageKeyPrefix The key prefix stored in the database
     * @return Fresh presigned URLs for all sizes, or null if avatar doesn't exist
     */
    suspend fun getAvatarUrls(storageKeyPrefix: String): CompanyAvatar? = withContext(Dispatchers.IO) {
        // Check if avatar exists by checking one of the sizes
        val smallKey = "${storageKeyPrefix}_small.webp"

        if (!storage.exists(smallKey)) {
            logger.debug("Avatar not found: $storageKeyPrefix")
            return@withContext null
        }

        val urls = AVATAR_SIZES.keys.associateWith { sizeName ->
            val key = "${storageKeyPrefix}_$sizeName.webp"
            storage.getSignedUrl(key, defaultUrlExpiry)
        }

        CompanyAvatar(
            small = urls["small"]!!,
            medium = urls["medium"]!!,
            large = urls["large"]!!
        )
    }

    /**
     * Delete an avatar and all its sizes.
     *
     * @param storageKeyPrefix The key prefix stored in the database
     */
    suspend fun deleteAvatar(storageKeyPrefix: String) = withContext(Dispatchers.IO) {
        logger.info("Deleting avatar: $storageKeyPrefix")

        for (sizeName in AVATAR_SIZES.keys) {
            val key = "${storageKeyPrefix}_$sizeName.webp"
            try {
                if (storage.exists(key)) {
                    storage.delete(key)
                    logger.debug("Deleted avatar size: $key")
                }
            } catch (e: Exception) {
                logger.warn("Failed to delete avatar size: $key", e)
            }
        }

        logger.info("Avatar deleted: $storageKeyPrefix")
    }

    /**
     * Validate image data before processing.
     *
     * @throws IllegalArgumentException if validation fails
     */
    private fun validateImage(imageData: ByteArray, contentType: String) {
        require(imageData.isNotEmpty()) { "Image data cannot be empty" }
        require(imageData.size <= MAX_FILE_SIZE) {
            "Image size ${imageData.size} exceeds maximum $MAX_FILE_SIZE bytes"
        }
        require(contentType in ALLOWED_CONTENT_TYPES) {
            "Content type $contentType is not allowed. Allowed types: $ALLOWED_CONTENT_TYPES"
        }
    }
}
