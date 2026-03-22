@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package tech.dokus.foundation.app.download

/**
 * Platform-specific file saver for downloading files to the user's device.
 *
 * - Android: saves to the public Downloads directory via MediaStore
 * - Desktop: saves to ~/Downloads via java.io.File
 * - iOS: saves to the app's documents directory
 * - WASM: triggers a browser download via Blob URL
 */
expect class FileSaver() {
    suspend fun saveFile(filename: String, bytes: ByteArray)
}
