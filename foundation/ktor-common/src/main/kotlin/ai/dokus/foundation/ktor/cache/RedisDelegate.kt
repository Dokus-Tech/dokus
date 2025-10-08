package ai.dokus.foundation.ktor.cache

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.time.Duration
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Property delegate for Redis-backed values
 * Similar to Android's SharedPreferences delegates or Exposed's column delegates
 */
class RedisDelegate<T>(
    private val client: RedisClient,
    private val key: String,
    private val serializer: KSerializer<T>,
    private val default: T? = null,
    private val ttl: Duration? = null
) : ReadWriteProperty<Any?, T?> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? = runBlocking {
        val value = client.get(key) ?: return@runBlocking default
        CacheSerializer.deserialize(value, serializer)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        runBlocking {
            if (value == null) {
                client.delete(key)
            } else {
                val serialized = CacheSerializer.serialize(value, serializer)
                client.set(key, serialized, ttl)
            }
        }
    }
}

/**
 * Delegate for Redis-backed string values
 */
class RedisStringDelegate(
    private val client: RedisClient,
    private val key: String,
    private val default: String? = null,
    private val ttl: Duration? = null
) : ReadWriteProperty<Any?, String?> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): String? = runBlocking {
        client.get(key) ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        runBlocking {
            if (value == null) {
                client.delete(key)
            } else {
                client.set(key, value, ttl)
            }
        }
    }
}

/**
 * Delegate for Redis-backed counter values
 */
class RedisCounterDelegate(
    private val client: RedisClient,
    private val key: String,
    private val initial: Long = 0
) : ReadWriteProperty<Any?, Long> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): Long = runBlocking {
        client.get(key)?.toLongOrNull() ?: initial
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
        runBlocking {
            client.set(key, value.toString())
        }
    }

    suspend fun increment(delta: Long = 1): Long = client.increment(key, delta)
    suspend fun decrement(delta: Long = 1): Long = client.decrement(key, delta)
}

/**
 * DSL for creating Redis delegates
 */
inline fun <reified T> RedisClient.cached(
    key: String,
    default: T? = null,
    ttl: Duration? = null
): RedisDelegate<T> = RedisDelegate(this, key, serializer(), default, ttl)

fun RedisClient.cachedString(
    key: String,
    default: String? = null,
    ttl: Duration? = null
): RedisStringDelegate = RedisStringDelegate(this, key, default, ttl)

fun RedisClient.counter(
    key: String,
    initial: Long = 0
): RedisCounterDelegate = RedisCounterDelegate(this, key, initial)

/**
 * Lazy Redis delegate that only connects when first accessed
 */
class LazyRedisDelegate<T>(
    private val clientProvider: () -> RedisClient,
    private val key: String,
    private val serializer: KSerializer<T>,
    private val default: T? = null,
    private val ttl: Duration? = null
) : ReadWriteProperty<Any?, T?> {

    private val client by lazy { clientProvider() }
    private val delegate by lazy { RedisDelegate(client, key, serializer, default, ttl) }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? =
        delegate.getValue(thisRef, property)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        delegate.setValue(thisRef, property, value)
    }
}