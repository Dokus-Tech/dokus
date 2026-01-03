package tech.dokus.foundation.sstorage

import tech.dokus.domain.model.common.Feature

actual fun createSecureStorage(context: Any?, feature: Feature): SecureStorage {
    return try {
        // Try to use the OS keychain first (most secure)
        OSKeychainSecureStorage(feature.serviceName)
    } catch (e: SecurityException) {
        // Fall back to enhanced file-based storage when keychain is unavailable
        println("OS Keychain not available, using file-based secure storage: ${e.message}")
        JVMSecureStorage(feature.serviceName)
    }
}
