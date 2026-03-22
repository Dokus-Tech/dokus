package tech.dokus.database.entity

import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId

/**
 * Database entity for document search hits.
 */
data class SearchDocumentHitEntity(
    val documentId: DocumentId,
    val filename: String,
    val documentType: DocumentType? = null,
    val status: DocumentStatus? = null,
    val counterpartyName: String? = null,
    val counterpartyVat: String? = null,
    val amount: Money? = null,
) {
    companion object
}

/**
 * Database entity for contact search hits.
 */
data class SearchContactHitEntity(
    val contactId: ContactId,
    val name: String,
    val email: String? = null,
    val vatNumber: String? = null,
    val companyNumber: String? = null,
    val isActive: Boolean = true,
) {
    companion object
}

/**
 * Database entity for transaction search hits.
 */
data class SearchTransactionHitEntity(
    val entryId: CashflowEntryId,
    val displayText: String,
    val status: CashflowEntryStatus,
    val date: LocalDate,
    val amount: Money,
    val direction: CashflowDirection,
    val contactName: String? = null,
    val documentFilename: String? = null,
    val documentId: DocumentId? = null,
) {
    companion object
}
