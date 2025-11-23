package ai.dokus.foundation.sstorage

import ai.dokus.foundation.domain.model.common.Feature

actual fun createSecureStorage(context: Any?, feature: Feature): SecureStorage {
    return try {
        // Try to use the OS keychain first (most secure)
        OSKeychainSecureStorage(feature.serviceName)
    } catch (e: Exception) {
        // Fall back to enhanced file-based storage
        println("OS Keychain not available, using file-based secure storage: ${e.message}")
        JVMSecureStorage(feature.serviceName)
    }
}
