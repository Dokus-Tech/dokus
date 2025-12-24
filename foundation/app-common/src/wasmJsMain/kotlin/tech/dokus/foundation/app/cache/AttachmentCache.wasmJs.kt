package tech.dokus.foundation.app.cache

/**
 * WASM/Web implementation of attachment cache functions.
 *
 * Note: Web browsers don't have direct file system access.
 * This is a stub implementation. In a real app, you would use:
 * - IndexedDB for larger binary storage
 * - LocalStorage for smaller data
 * - Cache API for HTTP responses
 *
 * For now, this uses in-memory storage which is cleared on page reload.
 */

private val inMemoryCache = mutableMapOf<String, ByteArray>()

actual fun getCacheDirectory(): String {
    return "/cache"
}

actual fun readFile(path: String): ByteArray? {
    return inMemoryCache[path]
}

actual fun writeFile(path: String, content: ByteArray) {
    inMemoryCache[path] = content
}

actual fun deleteFile(path: String) {
    inMemoryCache.remove(path)
}

actual fun getFileSize(path: String): Long {
    return inMemoryCache[path]?.size?.toLong() ?: 0L
}

actual fun listFiles(directory: String): List<String> {
    return inMemoryCache.keys.filter { it.startsWith(directory) }
}
