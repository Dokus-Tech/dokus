package ai.dokus.foundation.sstorage

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
external object console {
    fun log(message: String?)
}

/**
 * External localStorage reference
 */
@JsName("localStorage")
external val localStorage: Storage