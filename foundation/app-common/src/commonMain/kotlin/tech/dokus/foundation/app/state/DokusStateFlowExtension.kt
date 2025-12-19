package tech.dokus.foundation.app.state

import ai.dokus.foundation.domain.asbtractions.RetryHandler
import kotlinx.coroutines.flow.MutableStateFlow

suspend fun <T> MutableStateFlow<DokusState<T>>.emitIdle() {
    emit(DokusState.idle<T>())
}

suspend fun <T> MutableStateFlow<DokusState<T>>.emit(error: Throwable, retryHandler: RetryHandler) {
    emit(DokusState.error<T>(error, retryHandler))
}

suspend fun <T> MutableStateFlow<DokusState<T>>.emit(data: T) {
    emit(DokusState.success<T>(data))
}

suspend fun <T> MutableStateFlow<DokusState<T>>.emitLoading() {
    emit(DokusState.loading<T>())
}