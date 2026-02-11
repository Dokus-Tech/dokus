package tech.dokus.app.share

/**
 * Payload for a file shared into Dokus from platform share sheets.
 */
data class SharedImportFile(
    val name: String,
    val bytes: ByteArray,
    val mimeType: String
)
