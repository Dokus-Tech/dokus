package tech.dokus.foundation.app.state

import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException

/**
 * Sealed class implementation of DokusState for pattern matching and exhaustive when statements.
 *
 * Provides concrete implementations of the four DokusState variants. The base class exposes
 * `dataOrNull` for convenient access to data regardless of state (null in non-Success states).
 */
sealed class DokusStateSimple<DataType> : DokusState<DataType> {
    data class Idle<DataType>(override val lastData: DataType? = null) :
        DokusStateSimple<DataType>(),
        DokusState.Idle<DataType>

    data class Loading<DataType>(override val lastData: DataType? = null) :
        DokusStateSimple<DataType>(),
        DokusState.Loading<DataType>

    data class Success<DataType>(override val data: DataType) :
        DokusStateSimple<DataType>(),
        DokusState.Success<DataType> {
        override val lastData: DataType? = data
    }

    data class Error<DataType>(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
        override val lastData: DataType?
    ) : DokusStateSimple<DataType>(), DokusState.Error<DataType>
}
