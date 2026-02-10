package tech.dokus.backend.services.documents.confirmation

import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.FinancialDocumentDto

/**
 * Result of document confirmation containing the financial entity and cashflow entry ID.
 */
data class ConfirmationResult(
    val entity: FinancialDocumentDto,
    val cashflowEntryId: CashflowEntryId?,
    val documentId: DocumentId
)
