package tech.dokus.features.cashflow.usecases

import kotlinx.coroutines.flow.Flow
import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.foundation.app.state.DokusState

/**
 * Use case for watching pending documents that need review.
 *
 * Two operations are kept together because refresh invalidates the same stream
 * returned by invoke.
 */
interface WatchPendingDocumentsUseCase {
    operator fun invoke(limit: Int = 100): Flow<DokusState<List<DocumentListItemDto>>>

    fun refresh()
}

interface ObserveDocumentCollectionChangesUseCase {
    operator fun invoke(): Flow<Unit>
}

/**
 * Use case for submitting an invoice to the backend.
 */
interface SubmitInvoiceUseCase {
    suspend operator fun invoke(request: CreateInvoiceRequest): Result<DocDto.Invoice.Confirmed>
}

interface GetLatestInvoiceForContactUseCase {
    suspend operator fun invoke(contactId: ContactId): Result<DocDto.Invoice.Confirmed?>
}

interface GetContactPeppolStatusUseCase {
    suspend operator fun invoke(
        contactId: ContactId,
        refresh: Boolean = false
    ): Result<PeppolStatusResponse>
}

sealed interface SubmitInvoiceWithDeliveryResult {
    data class DraftSaved(val invoiceId: InvoiceId) : SubmitInvoiceWithDeliveryResult
    data class PeppolQueued(val invoiceId: InvoiceId) : SubmitInvoiceWithDeliveryResult
    data class PdfReady(
        val invoiceId: InvoiceId,
        val downloadUrl: String
    ) : SubmitInvoiceWithDeliveryResult
    data class DeliveryFailed(
        val invoiceId: InvoiceId,
        val error: String
    ) : SubmitInvoiceWithDeliveryResult
}

interface SubmitInvoiceWithDeliveryUseCase {
    suspend operator fun invoke(
        request: CreateInvoiceRequest,
        deliveryMethod: InvoiceDeliveryMethod?
    ): Result<SubmitInvoiceWithDeliveryResult>
}
