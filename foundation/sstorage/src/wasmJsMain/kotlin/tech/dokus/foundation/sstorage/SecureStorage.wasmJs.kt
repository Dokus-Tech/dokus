package tech.dokus.foundation.sstorage

import tech.dokus.domain.model.common.Feature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WASM/JS implementation of SecureStorage using localStorage.
 *
 * This implementation provides:
 * - Simple, reliable data persistence using localStorage
 * - Reactive Flow support for UI updates
 * - Basic obfuscation (not cryptographically secure)
 * - Error handling and fallback behavior
 */
internal class WasmSecureStorage(
    serviceName: String
) : SecureStorage() {

    private val storagePrefix = serviceName.replace(".", "_")

    // Flow state holder for reactive updates
    private val stringFlows = mutableMapOf<String, MutableStateFlow<String?>>()

    /**
     * Simple ROT13-style obfuscation for basic data protection.
     * Note: This is NOT cryptographically secure - it only prevents casual inspection.
     */
    private fun obfuscate(text: String): String {
        return text.map { char ->
            when {
                char.isLetter() -> {
                    val base = if (char.isLowerCase()) 'a' else 'A'
                    ((char - base + 13) % 26 + base.code).toChar()
                }

                char.isDigit() -> {
                    ((char - '0' + 5) % 10 + '0'.code).toChar()
                }

                else -> char
            }
        }.joinToString("")
    }

    /**
     * Reverse the obfuscation
     */
    private fun deobfuscate(obfuscatedText: String): String {
        return obfuscatedText.map { char ->
            when {
                char.isLetter() -> {
                    val base = if (char.isLowerCase()) 'a' else 'A'
                    ((char - base - 13 + 26) % 26 + base.code).toChar()
                }

                char.isDigit() -> {
                    ((char - '0' - 5 + 10) % 10 + '0'.code).toChar()
                }

                else -> char
            }
        }.joinToString("")
    }

    private fun getStorageKey(key: String): String = "$storagePrefix$key"

    /**
     * Safe storage operation with error handling
     */
    private fun safeStorageOperation(operation: () -> Unit) {
        try {
            operation()
        } catch (e: Exception) {
            console.log("Storage operation failed: ${e.message}")
        }
    }

    /**
     * Safe retrieval operation with error handling
     */
    private fun <T> safeRetrievalOperation(operation: () -> T?): T? {
        return try {
            operation()
        } catch (e: Exception) {
            console.log("Storage retrieval failed: ${e.message}")
            null
        }
    }

    override suspend fun putString(key: String, value: String) {
        safeStorageOperation {
            val obfuscated = obfuscate(value)
            localStorage.setItem(getStorageKey(key), obfuscated)
            // Update flow
            stringFlows[key]?.value = value
        }
    }

    override suspend fun getString(key: String): String? {
        return safeRetrievalOperation {
            localStorage.getItem(getStorageKey(key))?.let { deobfuscate(it) }
        }
    }

    override fun observeString(key: String): Flow<String?> {
        return stringFlows.getOrPut(key) {
            val initialValue = safeRetrievalOperation {
                localStorage.getItem(getStorageKey(key))?.let { deobfuscate(it) }
            }
            MutableStateFlow(initialValue)
        }.asStateFlow()
    }

    override suspend fun remove(key: String) {
        safeStorageOperation {
            localStorage.removeItem(getStorageKey(key))

            // Update flow to null
            stringFlows[key]?.value = null
        }
    }

    override suspend fun clear() {
        safeStorageOperation {
            // Find and remove all our prefixed keys
            val keysToRemove = mutableListOf<String>()

            try {
                for (i in 0 until localStorage.length) {
                    val key = localStorage.key(i)
                    if (key?.startsWith(storagePrefix) == true) {
                        keysToRemove.add(key)
                    }
                }
            } catch (e: Exception) {
                console.log("Error scanning localStorage keys: ${e.message}")
            }

            keysToRemove.forEach { key ->
                try {
                    localStorage.removeItem(key)
                } catch (e: Exception) {
                    console.log("Error removing key $key: ${e.message}")
                }
            }

            // Clear all flows
            stringFlows.values.forEach { it.value = null }
        }
    }

    override suspend fun contains(key: String): Boolean {
        return safeRetrievalOperation {
            localStorage.getItem(getStorageKey(key)) != null
        } ?: false
    }

    override suspend fun getAllKeys(): Set<String> {
        return safeRetrievalOperation {
            val keys = mutableSetOf<String>()

            try {
                for (i in 0 until localStorage.length) {
                    val key = localStorage.key(i)
                    if (key?.startsWith(storagePrefix) == true) {
                        keys.add(key.removePrefix(storagePrefix))
                    }
                }
            } catch (e: Exception) {
                console.log("Error getting all keys: ${e.message}")
            }

            keys
        } ?: emptySet()
    }
}

/**
 * WASM-specific factory function
 */
actual fun createSecureStorage(context: Any?, feature: Feature): SecureStorage {
    return WasmSecureStorage(feature.fullPackageName)
}