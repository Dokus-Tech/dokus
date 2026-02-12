package tech.dokus.app.share

actual object PlatformShareImportBridge {
    actual suspend fun consumeBatch(batchId: String?): Result<List<SharedImportFile>> = Result.success(emptyList())
}
