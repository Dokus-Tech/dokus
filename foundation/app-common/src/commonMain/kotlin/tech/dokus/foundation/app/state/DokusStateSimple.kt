package tech.dokus.foundation.app.state

import ai.dokus.foundation.domain.asbtractions.RetryHandler
import ai.dokus.foundation.domain.exceptions.DokusException


/**
 * Sealed class implementation of DokusState for pattern matching and exhaustive when statements.
 *
 * Provides concrete implementations of the four DokusState variants. The base class exposes
 * `dataOrNull` for convenient access to data regardless of state (null in non-Success states).
 */
sealed class DokusStateSimple<DataType>(val dataOrNull: DataType? = null) : DokusState<DataType> {
    data class Idle<DataType>(private val nothing: Any? = null) : DokusStateSimple<DataType>(),
        DokusState.Idle<DataType>

    data class Loading<DataType>(private val nothing: Any? = null) : DokusStateSimple<DataType>(),
        DokusState.Loading<DataType>

    data class Success<DataType>(override val data: DataType) : DokusStateSimple<DataType>(data),
        DokusState.Success<DataType>

    data class Error<DataType>(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : DokusStateSimple<DataType>(), DokusState.Error<DataType>
}