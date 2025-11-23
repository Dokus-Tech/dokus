package ai.dokus.foundation.network.resilient

interface RemoteServiceDelegate<T> {
    suspend fun <R> call(block: suspend (T) -> R): R

    fun get(): T
}

suspend operator fun <T, R> RemoteServiceDelegate<T>.invoke(block: suspend (T) -> R) = call(block)