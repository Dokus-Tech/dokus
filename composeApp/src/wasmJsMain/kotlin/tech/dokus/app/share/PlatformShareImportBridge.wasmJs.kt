package tech.dokus.app.share

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object PlatformShareImportBridge {
    actual suspend fun consumeBatch(batchId: String?): Result<List<SharedImportFile>> = Result.success(emptyList())
}
