package be.police.pulse.foundation.ktor.cache

/**
 * Utility for building consistent cache keys with namespace support
 */
object CacheKeyBuilder {

    /**
     * Build a cache key with multiple segments
     * @param segments Key segments that will be joined with ":"
     * @return The formatted cache key
     */
    fun build(vararg segments: String): String {
        return segments.filter { it.isNotBlank() }.joinToString(":")
    }

    /**
     * Build a cache key for user-specific data
     * @param userId The user identifier
     * @param resource The resource type (e.g., "profile", "settings")
     * @param additional Additional segments
     * @return The formatted cache key
     */
    fun forUser(userId: String, resource: String, vararg additional: String): String {
        val segments = listOf("user", userId, resource) + additional.toList()
        return build(*segments.toTypedArray())
    }

    /**
     * Build a cache key for session data
     * @param sessionId The session identifier
     * @param additional Additional segments
     * @return The formatted cache key
     */
    fun forSession(sessionId: String, vararg additional: String): String {
        val segments = listOf("session", sessionId) + additional.toList()
        return build(*segments.toTypedArray())
    }

    /**
     * Build a cache key for entity data
     * @param entityType The entity type (e.g., "product", "order")
     * @param entityId The entity identifier
     * @param additional Additional segments
     * @return The formatted cache key
     */
    fun forEntity(entityType: String, entityId: String, vararg additional: String): String {
        val segments = listOf(entityType, entityId) + additional.toList()
        return build(*segments.toTypedArray())
    }

    /**
     * Build a cache key for list/collection data
     * @param collectionType The collection type (e.g., "users", "products")
     * @param filters Optional filter segments for the collection
     * @return The formatted cache key
     */
    fun forCollection(collectionType: String, vararg filters: String): String {
        val segments = listOf("list", collectionType) + filters.toList()
        return build(*segments.toTypedArray())
    }

    /**
     * Build a cache key for query results
     * @param queryType The query type identifier
     * @param queryHash A hash of the query parameters
     * @return The formatted cache key
     */
    fun forQuery(queryType: String, queryHash: String): String {
        return build("query", queryType, queryHash)
    }

    /**
     * Build a cache key for rate limiting
     * @param resource The resource being rate limited
     * @param identifier The identifier (e.g., IP address, user ID)
     * @param window Optional time window identifier
     * @return The formatted cache key
     */
    fun forRateLimit(resource: String, identifier: String, window: String? = null): String {
        val segments = listOfNotNull("ratelimit", resource, identifier, window)
        return build(*segments.toTypedArray())
    }

    /**
     * Build a cache key for distributed locks
     * @param resource The resource to lock
     * @param identifier Optional lock identifier
     * @return The formatted cache key
     */
    fun forLock(resource: String, identifier: String? = null): String {
        val segments = listOfNotNull("lock", resource, identifier)
        return build(*segments.toTypedArray())
    }

    /**
     * Build a pattern for wildcard searches
     * @param prefix The key prefix
     * @return The pattern with wildcard
     */
    fun pattern(prefix: String): String {
        return "$prefix:*"
    }

    /**
     * Sanitize a string to be used as a key segment
     * Removes or replaces characters that might cause issues in keys
     * @param value The value to sanitize
     * @return The sanitized value
     */
    fun sanitize(value: String): String {
        return value
            .replace(":", "_")
            .replace(" ", "_")
            .replace("*", "_")
            .replace("?", "_")
            .replace("[", "_")
            .replace("]", "_")
            .lowercase()
    }

    /**
     * Generate a hash for complex objects to use as cache key
     * @param value The value to hash
     * @return A hash string suitable for cache keys
     */
    fun hash(value: String): String {
        return value.hashCode().toString(36)
    }
}