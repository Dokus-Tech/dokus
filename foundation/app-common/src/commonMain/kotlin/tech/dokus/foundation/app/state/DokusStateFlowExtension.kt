package tech.dokus.foundation.app.state

import kotlinx.coroutines.flow.MutableStateFlow
import tech.dokus.domain.asbtractions.RetryHandler

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
