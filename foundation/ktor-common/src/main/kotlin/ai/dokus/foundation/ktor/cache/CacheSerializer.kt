package ai.dokus.foundation.ktor.cache

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.time.Duration

/**
 * Serialization utilities for Redis cache operations
 */
object CacheSerializer {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Serialize an object to JSON string
     * @param value The object to serialize
     * @param serializer The serializer for the object type
     * @return JSON string representation
     */
    fun <T> serialize(value: T, serializer: KSerializer<T>): String {
        return json.encodeToString(serializer, value)
    }

    /**
     * Deserialize a JSON string to an object
     * @param value The JSON string
     * @param serializer The serializer for the object type
     * @return The deserialized object
     */
    fun <T> deserialize(value: String, serializer: KSerializer<T>): T {
        return json.decodeFromString(serializer, value)
    }

    /**
     * Extension function for RedisClient to get typed objects
     */
    suspend inline fun <reified T> RedisClient.getTyped(key: String): T? {
        val value = this.get(key) ?: return null
        return deserialize(value, serializer<T>())
    }

    /**
     * Extension function for RedisClient to set typed objects
     */
    suspend inline fun <reified T> RedisClient.setTyped(key: String, value: T, ttl: Duration? = null) {
        val serialized = serialize(value, serializer<T>())
        this.set(key, serialized, ttl)
    }

    /**
     * Extension function for RedisClient to set typed object only if absent
     */
    suspend inline fun <reified T> RedisClient.setTypedIfAbsent(key: String, value: T, ttl: Duration? = null): Boolean {
        val serialized = serialize(value, serializer<T>())
        return this.setIfAbsent(key, serialized, ttl)
    }

    /**
     * Extension function for RedisClient to get multiple typed objects
     */
    suspend inline fun <reified T> RedisClient.getManyTyped(vararg keys: String): Map<String, T> {
        val values = this.getMany(*keys)
        return values.mapValues { (_, value) ->
            deserialize(value, serializer<T>())
        }
    }

    /**
     * Extension function for RedisClient to set multiple typed objects
     */
    suspend inline fun <reified T> RedisClient.setManyTyped(values: Map<String, T>, ttl: Duration? = null) {
        val serialized = values.mapValues { (_, value) ->
            serialize(value, serializer<T>())
        }
        this.setMany(serialized, ttl)
    }
}