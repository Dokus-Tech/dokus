package ai.dokus.foundation.sstorage

/**
 * Memory-based storage delegate for fallback when keychain is unavailable.
 * Provides in-memory storage for testing environments like iOS Simulator.
 */
internal class MemoryStorageDelegate : IOSStorageDelegate {
    private val storage = mutableMapOf<String, String>()

    override fun store(key: String, value: String): Boolean {
        storage[key] = value
        return true
    }

    override fun retrieve(key: String): String? {
        return storage[key]
    }

    override fun remove(key: String): Boolean {
        return storage.remove(key) != null
    }

    override fun clear(): Boolean {
        storage.clear()
        return true
    }

    override fun contains(key: String): Boolean {
        return key in storage
    }

    override fun getAllKeys(): Set<String> {
        return storage.keys.toSet()
    }
}