package ai.dokus.foundation.domain.model.common

import kotlinx.serialization.Serializable

/**
 * Serializable Result wrapper for RPC communication.
 *
 * Kotlin's built-in Result type is not serializable, so we use this wrapper
 * for all RPC method return types.
 *
 * Usage in RPC interfaces:
 * ```
 * @Rpc
 * interface MyApi {
 *     suspend fun doSomething(): RpcResult<MyData>
 * }
 * ```
 */
@Serializable
sealed class RpcResult<out T> {

    @Serializable
    data class Success<T>(val value: T) : RpcResult<T>()

    @Serializable
    data class Failure(val error: RpcError) : RpcResult<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error.toException()
    }

    inline fun <R> map(transform: (T) -> R): RpcResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    inline fun onSuccess(action: (T) -> Unit): RpcResult<T> {
        if (this is Success) action(value)
        return this
    }

    inline fun onFailure(action: (RpcError) -> Unit): RpcResult<T> {
        if (this is Failure) action(error)
        return this
    }

    companion object {
        fun <T> success(value: T): RpcResult<T> = Success(value)
        fun <T> failure(error: RpcError): RpcResult<T> = Failure(error)
        fun <T> failure(message: String, code: String? = null): RpcResult<T> =
            Failure(RpcError(message, code))
    }
}

/**
 * Serializable error representation for RPC.
 */
@Serializable
data class RpcError(
    val message: String,
    val code: String? = null,
    val details: Map<String, String>? = null
) {
    fun toException(): Exception = Exception("[$code] $message")
}

/**
 * Convert Kotlin Result to RpcResult for RPC communication.
 */
fun <T> Result<T>.toRpcResult(): RpcResult<T> = fold(
    onSuccess = { RpcResult.success(it) },
    onFailure = {
        RpcResult.failure(
            RpcError(
                message = it.message ?: "Unknown error",
                code = it::class.simpleName
            )
        )
    }
)

/**
 * Convert RpcResult to Kotlin Result for internal use.
 */
fun <T> RpcResult<T>.toResult(): Result<T> = when (this) {
    is RpcResult.Success -> Result.success(value)
    is RpcResult.Failure -> Result.failure(error.toException())
}
