package tech.dokus.foundation.app.state

import ai.dokus.foundation.domain.exceptions.DokusException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


@OptIn(ExperimentalContracts::class)
fun DokusState<*>.isIdle(): Boolean {
    contract {
        returns(true) implies (this@isIdle is DokusState.Idle)
    }
    return this is DokusState.Idle
}

@OptIn(ExperimentalContracts::class)
fun <DataType> DokusState<DataType>.isLoading(): Boolean {
    contract {
        returns(true) implies (this@isLoading is DokusState.Loading<DataType>)
    }
    return this is DokusState.Loading
}

@OptIn(ExperimentalContracts::class)
fun <DataType> DokusState<DataType>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is DokusState.Success<DataType>)
    }
    return this is DokusState.Success
}

@OptIn(ExperimentalContracts::class)
fun <DataType> DokusState<DataType>.isError(): Boolean {
    contract {
        returns(true) implies (this@isError is DokusState.Error<DataType>)
    }
    return this is DokusState.Error
}

fun DokusState<*>.exceptionIfError(): DokusException? {
    return if (isError()) exception else null
}