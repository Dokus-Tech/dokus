package tech.dokus.features.cashflow.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.foundation.app.state.DokusState

/**
 * Use case for listing cashflow documents with pagination.
 */
interface LoadCashflowDocumentsUseCase {
    suspend operator fun invoke(
        page: Int,
        pageSize: Int,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Result<PaginatedResponse<FinancialDocumentDto>>

    suspend fun loadAll(
        pageSize: Int,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Result<List<FinancialDocumentDto>>
}

/**
 * Use case for watching pending documents that need review.
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
