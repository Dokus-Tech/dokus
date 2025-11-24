package ai.dokus.media.backend.storage

import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.ids.OrganizationId
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

data class StoredMedia(
    val storageKey: String,
    val bucket: String
)

interface MediaStorage {
    fun validate(fileContent: ByteArray, filename: String, mimeType: String): String?
    suspend fun store(
        organizationId: OrganizationId,
        mediaId: MediaId,
        filename: String,
        mimeType: String,
        fileContent: ByteArray
    ): Result<StoredMedia>

    suspend fun delete(storageKey: String): Result<Boolean>
    fun generateDownloadUrl(storageKey: String): String
}

class LocalMediaStorage(
    private val storageBasePath: String = "./storage/media",
    private val maxFileSizeMb: Long = 20,
    private val allowedMimeTypes: Set<String> = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp",
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain",
        "text/csv"
    )
) : MediaStorage {
    private val logger = LoggerFactory.getLogger(LocalMediaStorage::class.java)
    private val bucketName = "local"

    init {
        File(storageBasePath).mkdirs()
        logger.info("Local media storage initialized at $storageBasePath")
    }

    override fun validate(fileContent: ByteArray, filename: String, mimeType: String): String? {
        if (filename.isBlank()) return "Filename cannot be empty"
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return "Invalid filename: path traversal not allowed"
        }

        val fileSizeMb = fileContent.size / (1024.0 * 1024.0)
        if (fileSizeMb > maxFileSizeMb) {
            return "File size (${"%.2f".format(fileSizeMb)}MB) exceeds max allowed ${maxFileSizeMb}MB"
        }

        if (mimeType.isBlank()) return "Mime type cannot be empty"
        if (mimeType !in allowedMimeTypes) {
            return "File type '$mimeType' is not allowed"
        }

        return null
    }

    override suspend fun store(
        organizationId: OrganizationId,
        mediaId: MediaId,
        filename: String,
        mimeType: String,
        fileContent: ByteArray
    ): Result<StoredMedia> = runCatching {
        val orgDir = File(storageBasePath, organizationId.toString())
        val mediaDir = File(orgDir, mediaId.toString())
        mediaDir.mkdirs()

        val safeFilename = "${UUID.randomUUID()}_$filename"
        val targetFile = File(mediaDir, safeFilename)

        targetFile.writeBytes(fileContent)

        val storageKey = targetFile.relativeTo(File(storageBasePath))
            .path
            .replace(File.separatorChar, '/')

        logger.info("Stored media locally at $storageKey (${fileContent.size} bytes)")

        StoredMedia(storageKey = storageKey, bucket = bucketName)
    }

    override suspend fun delete(storageKey: String): Result<Boolean> = runCatching {
        if (storageKey.contains("..")) {
            throw SecurityException("Invalid storage key")
        }
        val file = File(storageBasePath, storageKey.replace('/', File.separatorChar))
        if (!file.exists()) return@runCatching false
        file.delete()
    }

    override fun generateDownloadUrl(storageKey: String): String {
        return "/media/download/$storageKey"
    }
}
