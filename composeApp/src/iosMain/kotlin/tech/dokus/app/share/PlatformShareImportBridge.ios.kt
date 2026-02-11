@file:Suppress("SwallowedException", "TooGenericExceptionCaught")

package tech.dokus.app.share

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.posix.memcpy

private const val AppGroupIdentifier = "group.vision.invoid.dokus.share"
private const val SharedImportsDirectory = "SharedImports"
private const val PdfExtension = "pdf"
private const val NameExtension = "name"

@OptIn(ExperimentalForeignApi::class)
actual object PlatformShareImportBridge {
    actual suspend fun consumeBatch(batchId: String?): Result<SharedImportFile?> = runCatching {
        val resolvedBatchId = batchId?.takeIf { it.isNotBlank() } ?: return@runCatching null
        val fileManager = NSFileManager.defaultManager
        val appGroupUrl = fileManager.containerURLForSecurityApplicationGroupIdentifier(AppGroupIdentifier)
            ?: return@runCatching null
        val basePath = "${appGroupUrl.path}/$SharedImportsDirectory"
        val pdfPath = "$basePath/$resolvedBatchId.$PdfExtension"
        val pdfData = NSData.dataWithContentsOfFile(pdfPath) ?: return@runCatching null

        val bytes = ByteArray(pdfData.length.toInt())
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), pdfData.bytes, pdfData.length)
        }

        val fileName = readFileName(basePath, resolvedBatchId) ?: "shared-$resolvedBatchId.pdf"

        runCatching { fileManager.removeItemAtPath(pdfPath, error = null) }
        runCatching { fileManager.removeItemAtPath("$basePath/$resolvedBatchId.$NameExtension", error = null) }

        SharedImportFile(
            name = fileName,
            bytes = bytes,
            mimeType = "application/pdf"
        )
    }

    private fun readFileName(basePath: String, batchId: String): String? {
        val namePath = "$basePath/$batchId.$NameExtension"
        val nameData = NSData.dataWithContentsOfFile(namePath) ?: return null
        val name = NSString.create(data = nameData, encoding = NSUTF8StringEncoding)?.toString()
        return name?.takeIf { it.isNotBlank() }
    }
}
