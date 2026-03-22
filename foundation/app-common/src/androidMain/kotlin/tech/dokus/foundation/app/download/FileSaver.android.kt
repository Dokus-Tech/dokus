@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package tech.dokus.foundation.app.download

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class FileSaver : KoinComponent {

    private val context: Context by inject()

    actual suspend fun saveFile(filename: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            val mimeType = mimeTypeFromFilename(filename)
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: error("Failed to create MediaStore entry for $filename")
            resolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
            } ?: error("Failed to open output stream for $filename")
        }
    }
}

private fun mimeTypeFromFilename(filename: String): String {
    val extension = filename.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "csv" -> "text/csv"
        "xml" -> "application/xml"
        else -> "application/octet-stream"
    }
}
