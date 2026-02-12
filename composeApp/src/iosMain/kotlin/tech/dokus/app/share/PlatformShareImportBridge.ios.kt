@file:Suppress("SwallowedException", "TooGenericExceptionCaught")

package tech.dokus.app.share

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

private const val AppGroupIdentifier = "group.vision.invoid.dokus.share"
private const val SharedImportsDirectory = "SharedImports"
private const val PdfExtension = "pdf"
private const val NameExtension = "name"
private const val CountExtension = "count"

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual object PlatformShareImportBridge {
    actual suspend fun consumeBatch(batchId: String?): Result<List<SharedImportFile>> = runCatching {
        val resolvedBatchId = batchId?.takeIf { it.isNotBlank() } ?: return@runCatching emptyList()
        val fileManager = NSFileManager.defaultManager
        val appGroupUrl = fileManager.containerURLForSecurityApplicationGroupIdentifier(AppGroupIdentifier)
            ?: return@runCatching emptyList()
        val basePath = "${appGroupUrl.path}/$SharedImportsDirectory"

        val indexedBatchCount = readBatchCount(basePath, resolvedBatchId)
        if (indexedBatchCount != null) {
            val indexedFiles = buildList {
                for (index in 0 until indexedBatchCount) {
                    readIndexedSharedFile(
                        fileManager = fileManager,
                        basePath = basePath,
                        batchId = resolvedBatchId,
                        index = index
                    )?.let(::add)
                }
            }
            runCatching {
                fileManager.removeItemAtPath(
                    "$basePath/$resolvedBatchId.$CountExtension",
                    error = null
                )
            }
            if (indexedFiles.isNotEmpty()) {
                return@runCatching indexedFiles
            }
        }

        val legacyFile = readLegacySharedFile(fileManager, basePath, resolvedBatchId)
        if (legacyFile != null) {
            listOf(legacyFile)
        } else {
            emptyList()
        }
    }

    private fun readBatchCount(basePath: String, batchId: String): Int? {
        val countPath = "$basePath/$batchId.$CountExtension"
        val countData = NSData.dataWithContentsOfFile(countPath) ?: return null
        val raw = NSString.create(countData, NSUTF8StringEncoding)?.toString()?.trim()
        return raw?.toIntOrNull()?.takeIf { it > 0 }
    }

    private fun readIndexedSharedFile(
        fileManager: NSFileManager,
        basePath: String,
        batchId: String,
        index: Int
    ): SharedImportFile? {
        val pdfPath = "$basePath/$batchId.$index.$PdfExtension"
        val pdfData = NSData.dataWithContentsOfFile(pdfPath) ?: return null

        val fileName = readIndexedFileName(basePath, batchId, index)
            ?: "shared-$batchId-$index.pdf"

        runCatching { fileManager.removeItemAtPath(pdfPath, error = null) }
        runCatching {
            fileManager.removeItemAtPath(
                "$basePath/$batchId.$index.$NameExtension",
                error = null
            )
        }

        return SharedImportFile(
            name = fileName,
            bytes = pdfData.toByteArray(),
            mimeType = "application/pdf"
        )
    }

    private fun readIndexedFileName(basePath: String, batchId: String, index: Int): String? {
        val namePath = "$basePath/$batchId.$index.$NameExtension"
        val nameData = NSData.dataWithContentsOfFile(namePath) ?: return null
        val name = NSString.create(nameData, NSUTF8StringEncoding)?.toString()
        return name?.takeIf { it.isNotBlank() }
    }

    private fun readLegacySharedFile(
        fileManager: NSFileManager,
        basePath: String,
        batchId: String
    ): SharedImportFile? {
        val pdfPath = "$basePath/$batchId.$PdfExtension"
        val pdfData = NSData.dataWithContentsOfFile(pdfPath) ?: return null

        val fileName = readLegacyFileName(basePath, batchId) ?: "shared-$batchId.pdf"

        runCatching { fileManager.removeItemAtPath(pdfPath, error = null) }
        runCatching { fileManager.removeItemAtPath("$basePath/$batchId.$NameExtension", error = null) }

        return SharedImportFile(
            name = fileName,
            bytes = pdfData.toByteArray(),
            mimeType = "application/pdf"
        )
    }

    private fun readLegacyFileName(basePath: String, batchId: String): String? {
        val namePath = "$basePath/$batchId.$NameExtension"
        val nameData = NSData.dataWithContentsOfFile(namePath) ?: return null
        val name = NSString.create(nameData, NSUTF8StringEncoding)?.toString()
        return name?.takeIf { it.isNotBlank() }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)

    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}
