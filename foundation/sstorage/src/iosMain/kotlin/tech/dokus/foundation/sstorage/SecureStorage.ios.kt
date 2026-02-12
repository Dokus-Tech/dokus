package tech.dokus.foundation.sstorage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.dokus.domain.model.common.Feature
import platform.Foundation.NSBundle

/**
 * iOS implementation of SecureStorage using the iOS Keychain Services.
 * Provides hardware-backed secure storage on iOS devices with memory fallback for testing.
 */
internal class IOSSecureStorage(
    serviceName: String
) : SecureStorage() {
    private val keychainAccessGroup = resolveSharedKeychainAccessGroup()
    private val sharedKeychainDelegate: IOSStorageDelegate = KeychainStorageDelegate(
        serviceName = serviceName,
        accessGroup = keychainAccessGroup
    )
    private val legacyKeychainDelegate: IOSStorageDelegate = KeychainStorageDelegate(
        serviceName = serviceName,
        accessGroup = null
    )
    private var didAttemptMigration = false
    private var storageDelegate: IOSStorageDelegate = sharedKeychainDelegate

    // Flow state holder for reactive updates
    private val stringFlows = mutableMapOf<String, MutableStateFlow<String?>>()

    override suspend fun putString(key: String, value: String) {
        migrateLegacyEntriesIfNeeded()
        val success = storageDelegate.store(key, value)
        // If keychain fails, switch to memory fallback
        if (!success && storageDelegate is KeychainStorageDelegate) {
            storageDelegate = MemoryStorageDelegate()
            storageDelegate.store(key, value)
        }
        updateStringFlow(key, value)
    }

    override suspend fun getString(key: String): String? {
        migrateLegacyEntriesIfNeeded()
        return storageDelegate.retrieve(key)
    }

    override fun observeString(key: String): Flow<String?> {
        migrateLegacyEntriesIfNeeded()
        val flow = stringFlows.getOrPut(key) {
            MutableStateFlow<String?>(null).apply {
                value = storageDelegate.retrieve(key)
            }
        }
        return flow.asStateFlow()
    }

    override suspend fun remove(key: String) {
        storageDelegate.remove(key)
        if (storageDelegate !== legacyKeychainDelegate) {
            legacyKeychainDelegate.remove(key)
        }
        updateStringFlow(key, null)
    }

    override suspend fun clear() {
        storageDelegate.clear()
        if (storageDelegate !== legacyKeychainDelegate) {
            legacyKeychainDelegate.clear()
        }
        // Clear all flows
        stringFlows.values.forEach { it.value = null }
    }

    override suspend fun contains(key: String): Boolean {
        migrateLegacyEntriesIfNeeded()
        return storageDelegate.contains(key)
    }

    override suspend fun getAllKeys(): Set<String> {
        migrateLegacyEntriesIfNeeded()
        return storageDelegate.getAllKeys()
    }

    // Flow update helper
    private fun updateStringFlow(key: String, value: String?) {
        stringFlows[key]?.value = value
    }

    private fun migrateLegacyEntriesIfNeeded() {
        if (didAttemptMigration || keychainAccessGroup == null || storageDelegate is MemoryStorageDelegate) {
            return
        }
        didAttemptMigration = true

        val legacyKeys = legacyKeychainDelegate.getAllKeys()
        legacyKeys.forEach { key ->
            if (sharedKeychainDelegate.contains(key)) return@forEach
            val legacyValue = legacyKeychainDelegate.retrieve(key) ?: return@forEach
            if (sharedKeychainDelegate.store(key, legacyValue)) {
                legacyKeychainDelegate.remove(key)
            }
        }
    }
}

/**
 * iOS-specific factory function
 */
actual fun createSecureStorage(context: Any?, feature: Feature): SecureStorage {
    return IOSSecureStorage(feature.serviceName)
}

private fun resolveSharedKeychainAccessGroup(): String? {
    return NSBundle.mainBundle
        .objectForInfoDictionaryKey("DokusSharedKeychainAccessGroup")
        ?.toString()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}
