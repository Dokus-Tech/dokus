package tech.dokus.foundation.sstorage

/**
 * Internal storage delegate interface for iOS secure storage backends
 */
internal interface IOSStorageDelegate {
    fun store(key: String, value: String): Boolean
    fun retrieve(key: String): String?
    fun remove(key: String): Boolean
    fun clear(): Boolean
    fun contains(key: String): Boolean
    fun getAllKeys(): Set<String>
}
