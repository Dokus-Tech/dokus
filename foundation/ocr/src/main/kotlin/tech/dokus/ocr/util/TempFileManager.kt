package tech.dokus.ocr.util

import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * Manages temporary files for OCR processing.
 * Creates isolated temp directories and ensures cleanup.
 */
internal object TempFileManager {

    private const val TEMP_PREFIX = "dokus-ocr-"

    /**
     * Create a unique temp directory for this OCR operation.
     */
    fun createTempDir(): Path {
        return Files.createTempDirectory(TEMP_PREFIX + UUID.randomUUID().toString().take(8))
    }

    /**
     * Delete a directory and all its contents.
     * Safe - ignores errors during cleanup.
     */
    fun deleteRecursively(dir: Path) {
        try {
            dir.toFile().deleteRecursively()
        } catch (_: Exception) {
            // Ignore cleanup errors - best effort
        }
    }

    /**
     * Execute a block with a temp directory, ensuring cleanup on exit.
     */
    inline fun <T> withTempDir(block: (Path) -> T): T {
        val tempDir = createTempDir()
        return try {
            block(tempDir)
        } finally {
            deleteRecursively(tempDir)
        }
    }
}
