@file:Suppress(
    "TooGenericExceptionCaught", // Keychain operations can fail in many ways - graceful fallback
    "SwallowedException" // Intentionally swallowed for fallback behavior
)

package tech.dokus.foundation.sstorage

import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced secure storage using native OS keychains.
 * This provides better security than file-based storage for federal systems.
 *
 * Requires dependency: com.github.javakeyring:java-keyring:1.0.2
 *
 * Supports:
 * - Windows: Windows Credential Manager
 * - macOS: macOS Keychain
 * - Linux: GNOME Keyring / KWallet
 */
class OSKeychainSecureStorage(
    serviceName: String
) : SecureStorage() {

    companion object {
        private const val KEY_PREFIX = "tech.dokus"
        private const val SEPARATOR = "."
        private const val MAX_VALUE_LENGTH = 2048 // Some keychains have limits
        private const val DEFAULT_ACCOUNT = "dokus-secure-storage"
    }

    private val keyring: Keyring? = try {
        Keyring.create()
    } catch (e: Exception) {
        // Keychain not available, fallback to null
        null
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val keychainService = "${KEY_PREFIX}${SEPARATOR}$serviceName"
    private val keychainAccount = DEFAULT_ACCOUNT

    // In-memory cache for better performance
    private val cache = ConcurrentHashMap<String, MutableStateFlow<String?>>()

    // Track all keys for enumeration (since keychains don't always support listing)
    private val keySetIdentifier = "known_keys"
    private val knownKeys = loadKnownKeys().toMutableSet()

    init {
        if (keyring == null) {
            throw SecurityException("OS Keychain not available. Please ensure keychain service is running.")
        }
    }

    /**
     * Creates a service identifier for the keychain based on the key
     */
    private fun createServiceName(key: String): String {
        // Use a combination of prefix, service name, and key as the service identifier
        // This allows us to store multiple key-value pairs
        return "${keychainService}${SEPARATOR}$key"
    }

    /**
     * Loads the set of known keys from the keychain
     */
    private fun loadKnownKeys(): Set<String> {
        return try {
            val keysService = createServiceName(keySetIdentifier)
            keyring?.getPassword(keysService, keychainAccount)?.let {
                json.decodeFromString<Set<String>>(it)
            } ?: emptySet()
        } catch (e: PasswordAccessException) {
            // No keys stored yet
            emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Saves the set of known keys to the keychain
     */
    private fun saveKnownKeys() {
        try {
            val keysJson = json.encodeToString(knownKeys)
            val keysService = createServiceName(keySetIdentifier)
            keyring?.setPassword(keysService, keychainAccount, keysJson)
        } catch (e: Exception) {
            // Log error in production
        }
    }

    /**
     * Splits large values into chunks if necessary
     */
    private fun chunkValue(value: String): List<String> {
        return if (value.length <= MAX_VALUE_LENGTH) {
            listOf(value)
        } else {
            value.chunked(MAX_VALUE_LENGTH)
        }
    }

    /**
     * Reconstructs a value from chunks
     */
    private fun reconstructValue(key: String): String? {
        val service = createServiceName(key)

        // Try to get single value first
        return try {
            keyring?.getPassword(service, keychainAccount)
        } catch (e: PasswordAccessException) {
            // Try to reconstruct from chunks
            val chunks = collectChunks(service)
            chunks.takeIf { it.isNotEmpty() }?.joinToString("")
        }
    }

    /**
     * Collects all chunks for a given service key
     */
    private fun collectChunks(service: String): List<String> {
        val chunks = mutableListOf<String>()
        var index = 0
        var hasMoreChunks = true

        while (hasMoreChunks) {
            val chunkService = "${service}${SEPARATOR}chunk$index"
            hasMoreChunks = try {
                keyring?.getPassword(chunkService, keychainAccount)?.also {
                    chunks.add(it)
                    index++
                } != null
            } catch (e: PasswordAccessException) {
                false
            }
        }

        return chunks
    }

    /**
     * Stores a value, chunking if necessary
     */
    private fun storeValue(key: String, value: String) {
        val service = createServiceName(key)
        val chunks = chunkValue(value)

        if (chunks.size == 1) {
            // Store as single value
            keyring?.setPassword(service, keychainAccount, chunks[0])

            // Clean up any old chunks
            cleanupChunks(service)
        } else {
            // Store as chunks
            chunks.forEachIndexed { index, chunk ->
                val chunkService = "${service}${SEPARATOR}chunk$index"
                keyring?.setPassword(chunkService, keychainAccount, chunk)
            }

            // Delete the single value if it exists
            try {
                keyring?.deletePassword(service, keychainAccount)
            } catch (e: PasswordAccessException) {
                // Ignore if doesn't exist
            }
        }
    }

    /**
     * Removes all chunks for a key
     */
    private fun cleanupChunks(service: String) {
        var index = 0
        var hasMoreChunks = true

        while (hasMoreChunks) {
            val chunkService = "${service}${SEPARATOR}chunk$index"
            hasMoreChunks = try {
                keyring?.deletePassword(chunkService, keychainAccount)
                index++
                true
            } catch (e: PasswordAccessException) {
                false
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun putString(key: String, value: String) {
        requireNotNull(keyring) { "Keychain not available" }

        try {
            storeValue(key, value)

            // Update known keys
            knownKeys.add(key)
            saveKnownKeys()

            // Update cache
            cache.getOrPut(key) { MutableStateFlow(null) }.value = value
        } catch (e: Exception) {
            throw SecurityException("Failed to store secure value", e)
        }
    }

    override suspend fun getString(key: String): String? {
        requireNotNull(keyring) { "Keychain not available" }

        // Check cache first
        cache[key]?.value?.let { return it }

        return try {
            reconstructValue(key)?.also { value ->
                // Update cache
                cache.getOrPut(key) { MutableStateFlow(null) }.value = value
            }
        } catch (e: PasswordAccessException) {
            null
        } catch (e: Exception) {
            throw SecurityException("Failed to retrieve secure value", e)
        }
    }

    override fun observeString(key: String): Flow<String?> {
        return cache.getOrPut(key) {
            MutableStateFlow(
                runBlocking { getString(key) }
            )
        }.asStateFlow()
    }

    override suspend fun remove(key: String) {
        requireNotNull(keyring) { "Keychain not available" }

        val service = createServiceName(key)

        try {
            // Try to delete single value
            try {
                keyring.deletePassword(service, keychainAccount)
            } catch (e: PasswordAccessException) {
                // Ignore if doesn't exist
            }

            // Clean up any chunks
            cleanupChunks(service)

            // Update known keys
            knownKeys.remove(key)
            saveKnownKeys()

            // Update cache
            cache[key]?.value = null
        } catch (e: Exception) {
            throw SecurityException("Failed to remove secure value", e)
        }
    }

    override suspend fun clear() {
        requireNotNull(keyring) { "Keychain not available" }

        // Remove all known keys
        val keysToRemove = knownKeys.toList()
        keysToRemove.forEach { key ->
            remove(key)
        }

        // Clear the keys list itself
        try {
            val keysService = createServiceName(keySetIdentifier)
            keyring.deletePassword(keysService, keychainAccount)
        } catch (e: PasswordAccessException) {
            // Ignore if doesn't exist
        }

        knownKeys.clear()
        cache.clear()
    }

    override suspend fun contains(key: String): Boolean {
        return key in knownKeys || getString(key) != null
    }

    override suspend fun getAllKeys(): Set<String> {
        return knownKeys.toSet()
    }
}
