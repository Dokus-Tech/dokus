package tech.dokus.backend.services.documents.confirmation

import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId

/**
 * Result of document confirmation containing the financial entity and cashflow entry ID.
 *
 * [entity] is the confirmed entity (InvoiceEntity, ExpenseEntity, or CreditNoteEntity).
 * Its concrete type is checked at call sites that need type-specific behaviour.
 */
data class ConfirmationResult(
    val entity: Any,
    val cashflowEntryId: CashflowEntryId?,
    val documentId: DocumentId
)
