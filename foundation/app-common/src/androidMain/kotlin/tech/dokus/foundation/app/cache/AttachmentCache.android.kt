package tech.dokus.foundation.app.cache

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

private object AndroidCacheHelper : KoinComponent {
    val context: Context by inject()
}

actual fun getCacheDirectory(): String {
    return AndroidCacheHelper.context.cacheDir.absolutePath
}

actual fun readFile(path: String): ByteArray? {
    return try {
        val file = File(path)
        if (file.exists()) file.readBytes() else null
    } catch (e: Exception) {
        null
    }
}

actual fun writeFile(path: String, content: ByteArray) {
    try {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeBytes(content)
    } catch (e: Exception) {
        // Log error if needed
    }
}

actual fun deleteFile(path: String) {
    try {
        File(path).delete()
    } catch (e: Exception) {
        // Ignore
    }
}

actual fun getFileSize(path: String): Long {
    return try {
        val file = File(path)
        if (file.exists()) file.length() else 0L
    } catch (e: Exception) {
        0L
    }
}

actual fun listFiles(directory: String): List<String> {
    return try {
        val dir = File(directory)
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.map { it.absolutePath } ?: emptyList()
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}
