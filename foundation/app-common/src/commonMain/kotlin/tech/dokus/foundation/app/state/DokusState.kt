package tech.dokus.foundation.app.state

import ai.dokus.foundation.domain.asbtractions.RetryHandler
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
    /** Initial state before any operation starts */
    interface Idle<DataType> : DokusState<DataType>

    /** Operation in progress */
    interface Loading<DataType> : DokusState<DataType>

    /** Operation completed successfully with data */
    interface Success<DataType> : DokusState<DataType> {
        val data: DataType
    }

    /** Operation failed with exception and retry capability */
    interface Error<DataType> : DokusState<DataType> {
        val exception: DokusException
        val retryHandler: RetryHandler
    }

    companion object {
        fun <DataType> idle(): DokusStateSimple<DataType> = DokusStateSimple.Idle()
        fun <DataType> loading(): DokusStateSimple<DataType> = DokusStateSimple.Loading()
        fun <DataType> success(data: DataType): DokusStateSimple<DataType> =
            DokusStateSimple.Success(data)

        fun <DataType> error(
            exception: DokusException,
            retryHandler: RetryHandler
        ): DokusStateSimple<DataType> =
            DokusStateSimple.Error(exception, retryHandler)

        fun <DataType> error(
            exception: Throwable,
            retryHandler: RetryHandler
        ): DokusStateSimple<DataType> =
            DokusStateSimple.Error(exception.asDokusException, retryHandler)
    }
}