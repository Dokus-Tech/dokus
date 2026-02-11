package tech.dokus.app.share

actual object PlatformShareImportBridge {
    actual suspend fun consumeBatch(batchId: String?): Result<SharedImportFile?> = Result.success(null)
}
