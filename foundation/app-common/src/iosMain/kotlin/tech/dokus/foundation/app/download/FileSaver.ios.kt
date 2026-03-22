@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package tech.dokus.foundation.app.download

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

actual class FileSaver {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun saveFile(filename: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            val fileManager = NSFileManager.defaultManager
            val documentsUrl = fileManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask
            ).firstOrNull() as? NSURL ?: error("Could not find documents directory")

            val fileUrl = documentsUrl.URLByAppendingPathComponent(filename)
                ?: error("Could not create file URL for $filename")

            val nsData = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            val success = nsData.writeToURL(fileUrl, atomically = true)
            if (!success) error("Failed to write file to $fileUrl")
        }
    }
}
