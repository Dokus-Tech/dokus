package tech.dokus.app.share

/**
 * Platform-specific bridge for handing off share-extension payloads to the main app.
 *
 * Used on iOS for App Group share extension batches.
 */
expect object PlatformShareImportBridge {
    suspend fun consumeBatch(batchId: String?): Result<SharedImportFile?>
}
