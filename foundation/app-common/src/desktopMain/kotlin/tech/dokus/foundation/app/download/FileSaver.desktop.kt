@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package tech.dokus.foundation.app.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class FileSaver {

    actual suspend fun saveFile(filename: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            val downloadsDir = File(System.getProperty("user.home"), "Downloads")
            downloadsDir.mkdirs()
            val target = resolveUniqueFile(downloadsDir, filename)
            target.writeBytes(bytes)
        }
    }
}

private fun resolveUniqueFile(directory: File, filename: String): File {
    var candidate = File(directory, filename)
    if (!candidate.exists()) return candidate

    val nameWithoutExtension = filename.substringBeforeLast('.')
    val extension = filename.substringAfterLast('.', "")
    var counter = 1
    while (candidate.exists()) {
        val suffixed = if (extension.isNotEmpty()) {
            "$nameWithoutExtension ($counter).$extension"
        } else {
            "$nameWithoutExtension ($counter)"
        }
        candidate = File(directory, suffixed)
        counter++
    }
    return candidate
}
