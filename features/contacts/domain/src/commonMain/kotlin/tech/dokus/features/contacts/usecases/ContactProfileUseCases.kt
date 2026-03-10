package tech.dokus.features.contacts.usecases

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.PeppolStatusResponse

/**
 * Invoice-centric snapshot for contact detail metrics and recent documents.
 */
data class ContactInvoiceSnapshot(
    val documentsCount: Int,
    val totalVolume: Money,
    val outstanding: Money,
    val recentDocuments: List<ContactRecentInvoice>,
)

/**
 * Lightweight row model for recent contact invoices.
 */
data class ContactRecentInvoice(
    val invoiceId: InvoiceId,
    val issueDate: LocalDate,
    val updatedAt: LocalDateTime,
    val direction: DocumentDirection,
    val status: InvoiceStatus,
    val totalAmount: Money,
    val outstandingAmount: Money,
    val summary: String? = null,
    val reference: String? = null,
)

/**
 * Resolve PEPPOL directory status for a contact.
 */
interface GetContactPeppolStatusUseCase {
    suspend operator fun invoke(
        contactId: ContactId,
        refresh: Boolean = false
    ): Result<PeppolStatusResponse>
}

/**
 * Build invoice snapshot (count, totals, outstanding, recent docs) for a contact.
 */
interface GetContactInvoiceSnapshotUseCase {
    suspend operator fun invoke(contactId: ContactId): Result<ContactInvoiceSnapshot>
}
