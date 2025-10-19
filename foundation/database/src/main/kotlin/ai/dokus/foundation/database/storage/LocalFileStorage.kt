package ai.dokus.foundation.database.storage

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Local filesystem implementation of FileStorage
 * Stores files in a directory on the local disk
 *
 * @param baseDirectory Root directory for file storage (e.g., "/var/dokus/storage")
 */
class LocalFileStorage(
    private val baseDirectory: String
) : FileStorage {
    private val logger = LoggerFactory.getLogger(LocalFileStorage::class.java)
    private val basePath: Path = Paths.get(baseDirectory)

    init {
        // Create base directory if it doesn't exist
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath)
            logger.info("Created storage directory: $baseDirectory")
        }
    }

    override suspend fun store(key: String, content: ByteArray, contentType: String): String {
        val filePath = basePath.resolve(key)

        // Create parent directories if they don't exist
        Files.createDirectories(filePath.parent)

        // Write file
        Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        logger.info("Stored file: $key (${content.size} bytes, $contentType)")

        return "file://$filePath"
    }

    override suspend fun retrieve(key: String): ByteArray {
        val filePath = basePath.resolve(key)

        if (!Files.exists(filePath)) {
            throw IllegalArgumentException("File not found: $key")
        }

        val content = Files.readAllBytes(filePath)
        logger.debug("Retrieved file: $key (${content.size} bytes)")

        return content
    }

    override suspend fun delete(key: String): Boolean {
        val filePath = basePath.resolve(key)

        if (!Files.exists(filePath)) {
            logger.warn("Attempted to delete non-existent file: $key")
            return false
        }

        Files.delete(filePath)
        logger.info("Deleted file: $key")

        // Clean up empty parent directories
        cleanupEmptyDirectories(filePath.parent)

        return true
    }

    override suspend fun exists(key: String): Boolean {
        val filePath = basePath.resolve(key)
        return Files.exists(filePath)
    }

    override suspend fun getPresignedDownloadUrl(key: String, expiresInSeconds: Int): String {
        // For local storage, return a file:// URL
        // In production, this would be handled by a separate HTTP endpoint
        val filePath = basePath.resolve(key)
        return "file://$filePath"
    }

    override suspend fun getPresignedUploadUrl(
        key: String,
        contentType: String,
        expiresInSeconds: Int
    ): String {
        // For local storage, return a placeholder URL
        // In production, this would be handled by a separate HTTP endpoint
        val filePath = basePath.resolve(key)
        return "file://$filePath"
    }

    override fun getBaseUrl(): String {
        return "file://$baseDirectory"
    }

    /**
     * Recursively removes empty directories up to the base path
     */
    private fun cleanupEmptyDirectories(directory: Path) {
        var current = directory

        while (current != basePath && Files.exists(current)) {
            val files = Files.list(current).use { it.count() }

            if (files == 0L) {
                Files.delete(current)
                logger.debug("Deleted empty directory: $current")
                current = current.parent
            } else {
                break
            }
        }
    }
}
