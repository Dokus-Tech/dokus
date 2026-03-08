package tech.dokus.foundation.app.state

import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException

/**
 * Type-safe state machine for async operations in Dokus.
 *
 * Represents the four states of an async operation: Idle, Loading, Success, Error.
 * Used throughout the application for repository operations, API calls, and data fetching.
 * Emitted via Kotlin Flow for reactive UI updates.
 */
interface DokusState<DataType> {
    val lastData: DataType?

    /** Initial state before any operation starts */
    interface Idle<DataType> : DokusState<DataType>

    /** Operation in progress */
    interface Loading<DataType> : DokusState<DataType> {
        override val lastData: DataType?
    }

    /** Operation completed successfully with data */
    interface Success<DataType> : DokusState<DataType> {
        val data: DataType
    }

    /** Operation failed with exception and retry capability */
    interface Error<DataType> : DokusState<DataType> {
        override val lastData: DataType?
        val exception: DokusException
        val retryHandler: RetryHandler
    }

    val asLoading: DokusState<DataType>
        get() {
            return if (isSuccess()) loading(data)
            else if (isError()) loading(lastData)
            else if (isLoading()) this
            else loading(null)
        }

    companion object {
        fun <DataType> idle(): DokusStateSimple<DataType> = DokusStateSimple.Idle()
        fun <DataType> loading(lastData: DataType? = null): DokusStateSimple<DataType> =
            DokusStateSimple.Loading(lastData)

        fun <DataType> success(data: DataType): DokusStateSimple<DataType> =
            DokusStateSimple.Success(data)

        fun <DataType> error(
            exception: DokusException,
            retryHandler: RetryHandler,
            lastData: DataType? = null
        ): DokusStateSimple<DataType> =
            DokusStateSimple.Error(exception, retryHandler, lastData)

        fun <DataType> error(
            exception: Throwable,
            retryHandler: RetryHandler,
            lastData: DataType? = null
        ): DokusStateSimple<DataType> =
            DokusStateSimple.Error(exception.asDokusException, retryHandler, lastData)
    }
}
