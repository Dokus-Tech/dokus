package ai.dokus.foundation.domain.api

enum class OperationResult(val error: Error? = null) {
    Success,
    Failure,
    OperationNotAvailable(NotImplementedError())
}