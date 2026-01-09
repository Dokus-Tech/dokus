package tech.dokus.features.cashflow.usecases

import kotlinx.coroutines.flow.Flow
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.foundation.app.state.DokusState

/**
 * Use case for watching pending documents that need review.
 *
 * Two operations are kept together because refresh invalidates the same stream
 * returned by invoke.
 */
interface WatchPendingDocumentsUseCase {
    operator fun invoke(limit: Int = 100): Flow<DokusState<List<DocumentRecordDto>>>

    fun refresh()
}

/**
 * Use case for submitting an invoice to the backend.
 */
interface SubmitInvoiceUseCase {
    suspend operator fun invoke(request: CreateInvoiceRequest): Result<FinancialDocumentDto.InvoiceDto>
}
