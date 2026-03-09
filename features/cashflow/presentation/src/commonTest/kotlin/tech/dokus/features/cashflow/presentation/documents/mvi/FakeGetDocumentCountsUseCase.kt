package tech.dokus.features.cashflow.presentation.documents.mvi

import tech.dokus.domain.model.DocumentCountsResponse
import tech.dokus.features.cashflow.usecases.GetDocumentCountsUseCase

internal class FakeGetDocumentCountsUseCase : GetDocumentCountsUseCase {
    private val results: ArrayDeque<Result<DocumentCountsResponse>> = ArrayDeque()
    var callCount: Int = 0
        private set

    fun enqueueResult(response: DocumentCountsResponse) {
        results.addLast(Result.success(response))
    }

    override suspend fun invoke(): Result<DocumentCountsResponse> {
        callCount += 1
        return requireNotNull(results.removeFirstOrNull()) {
            "No remaining count responses queued"
        }
    }
}
