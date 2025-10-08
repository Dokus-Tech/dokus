package ai.dokus.foundation.ktor.cache

/**
 * Enum for Redis namespaces to ensure type-safe namespace usage
 * Each namespace isolates keys to prevent conflicts between modules
 */
enum class RedisNamespace(val value: String) {
    Auth("auth");

    override fun toString(): String = value
}