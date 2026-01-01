package tech.dokus.foundation.sstorage

import tech.dokus.domain.model.common.Feature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS implementation of SecureStorage using the iOS Keychain Services.
 * Provides hardware-backed secure storage on iOS devices with memory fallback for testing.
 */
internal class IOSSecureStorage(
    serviceName: String
) : SecureStorage() {
    private var storageDelegate: IOSStorageDelegate = KeychainStorageDelegate(serviceName)

    // Flow state holder for reactive updates
    private val stringFlows = mutableMapOf<String, MutableStateFlow<String?>>()

    override suspend fun putString(key: String, value: String) {
        val success = storageDelegate.store(key, value)
        // If keychain fails, switch to memory fallback
        if (!success && storageDelegate is KeychainStorageDelegate) {
            println("Keychain not available, switching to memory fallback")
            storageDelegate = MemoryStorageDelegate()
            storageDelegate.store(key, value)
        }
        updateStringFlow(key, value)
    }

    override suspend fun getString(key: String): String? {
        return storageDelegate.retrieve(key)
    }

    override fun observeString(key: String): Flow<String?> {
        val flow = stringFlows.getOrPut(key) {
            MutableStateFlow<String?>(null).apply {
                value = storageDelegate.retrieve(key)
            }
        }
        return flow.asStateFlow()
    }

    override suspend fun remove(key: String) {
        storageDelegate.remove(key)
        updateStringFlow(key, null)
    }

    override suspend fun clear() {
        storageDelegate.clear()
        // Clear all flows
        stringFlows.values.forEach { it.value = null }
    }

    override suspend fun contains(key: String): Boolean {
        return storageDelegate.contains(key)
    }

    override suspend fun getAllKeys(): Set<String> {
        return storageDelegate.getAllKeys()
    }

    // Flow update helper
    private fun updateStringFlow(key: String, value: String?) {
        stringFlows[key]?.value = value
    }

}

/**
 * iOS-specific factory function
 */
actual fun createSecureStorage(context: Any?, feature: Feature): SecureStorage {
    return IOSSecureStorage(feature.serviceName)
}