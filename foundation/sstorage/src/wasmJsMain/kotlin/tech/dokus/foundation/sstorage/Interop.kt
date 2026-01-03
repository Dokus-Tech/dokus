package tech.dokus.foundation.sstorage

/**
 * External interface for localStorage API
 */
external interface Storage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
    fun clear()
    val length: Int
    fun key(index: Int): String?
}

/**
 * External console for debugging
 */
@JsName("console")
external object Console {
    @Suppress("UnusedParameter") // External JS declaration - parameter used by JS runtime
    fun log(message: String?)
}

/**
 * External localStorage reference
 */
@JsName("localStorage")
external val localStorage: Storage
