package ai.dokus.foundation.sstorage

import tech.dokus.domain.model.common.Feature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SecureStorage interface providing encrypted key-value storage with Flow support
 * for reactive data access across all platforms.
 *
 * This implementation uses platform-specific secure storage mechanisms:
 * - Android: DataStore with Android Keystore for encryption
 * - iOS: DataStore with Keychain for encryption
 * - JVM: DataStore with encrypted file storage
 * - WASM: localStorage with obfuscation (limited security)
 */
abstract class SecureStorage {

    // Internal string operations - platform implementations only need to handle strings
    abstract suspend fun putString(key: String, value: String)
    abstract suspend fun getString(key: String): String?
    abstract fun observeString(key: String): Flow<String?>

    // Generic type-safe operations for clients
    suspend inline fun <reified T> get(key: String, defaultValue: T? = null): T? {
        val value = getString(key)
        return when (T::class) {
            Int::class -> value?.toIntOrNull() ?: defaultValue
            Boolean::class -> value?.toBooleanStrictOrNull() ?: defaultValue
            Long::class -> value?.toLongOrNull() ?: defaultValue
            Float::class -> value?.toFloatOrNull() ?: defaultValue
            Double::class -> value?.toDoubleOrNull() ?: defaultValue
            String::class -> value ?: defaultValue
            else -> throw IllegalArgumentException("Unsupported type ${T::class.simpleName} for key: $key")
        } as? T?
    }

    suspend inline fun <reified T> set(key: String, value: T?) {
        if (value == null) {
            remove(key)
            return
        }

        val stringValue = when (T::class) {
            Int::class, Boolean::class, Long::class, Float::class, Double::class -> value.toString()
            String::class -> value as String
            else -> throw IllegalArgumentException("Unsupported type ${T::class.simpleName} for key: $key")
        }
        putString(key, stringValue)
    }

    inline fun <reified T> subscribe(key: String): Flow<T?> {
        return observeString(key).map { value ->
            when (T::class) {
                Int::class -> value?.toIntOrNull()
                Boolean::class -> value?.toBooleanStrictOrNull()
                Long::class -> value?.toLongOrNull()
                Float::class -> value?.toFloatOrNull()
                Double::class -> value?.toDoubleOrNull()
                String::class -> value
                else -> throw IllegalArgumentException("Unsupported type ${T::class.simpleName} for key: $key")
            } as? T?
        }
    }

    // Management operations
    abstract suspend fun remove(key: String)
    abstract suspend fun clear()
    abstract suspend fun contains(key: String): Boolean
    abstract suspend fun getAllKeys(): Set<String>
}

/**
 * Factory function to create platform-specific SecureStorage instance.
 *
 * @param context Platform-specific context (Android Context, or Any for other platforms)
 * @return SecureStorage instance
 */
expect fun createSecureStorage(
    context: Any? = null,
    feature: Feature,
): SecureStorage