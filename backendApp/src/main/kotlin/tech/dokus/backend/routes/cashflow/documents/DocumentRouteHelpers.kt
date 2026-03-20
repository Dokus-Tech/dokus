package tech.dokus.backend.routes.cashflow.documents

import tech.dokus.backend.mappers.from
import tech.dokus.database.entity.BankStatementEntity
import tech.dokus.database.entity.BankTransactionEntity
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.repository.banking.BankStatementRepository
import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.database.entity.CreditNoteEntity
import tech.dokus.database.entity.ExpenseEntity
import tech.dokus.database.entity.InvoiceEntity
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.foundation.backend.storage.DocumentStorageService as MinioDocumentStorageService

/**
 * Add download URL to document DTO.
 * [storageKey] is resolved from the preferred source's blob, not from the document itself.
 */
@Suppress("TooGenericExceptionCaught")
internal suspend fun addDownloadUrl(
    document: DocumentDto,
    storageKey: String?,
    minioStorage: MinioDocumentStorageService,
    logger: org.slf4j.Logger
): DocumentDto {
    if (storageKey == null) return document
    val downloadUrl = try {
        minioStorage.getDownloadUrl(storageKey)
    } catch (e: RuntimeException) {
        logger.warn("Failed to get download URL for $storageKey: ${e.message}")
        null
    }
    return document.copy(downloadUrl = downloadUrl)
}

/**
 * Find a confirmed financial entity by document ID.
 * Returns the raw entity or aggregate record required to rebuild confirmed content.
 */
@Suppress("LongParameterList")
internal suspend fun findConfirmedEntity(
    documentId: DocumentId,
    documentType: DocumentType?,
    tenantId: TenantId,
    invoiceRepository: InvoiceRepository,
    expenseRepository: ExpenseRepository,
    creditNoteRepository: CreditNoteRepository,
    bankStatementRepository: BankStatementRepository,
    bankTransactionRepository: BankTransactionRepository,
): Any? {
    return when (documentType) {
        DocumentType.Invoice -> invoiceRepository.findByDocumentId(tenantId, documentId)
        DocumentType.CreditNote -> creditNoteRepository.findByDocumentId(tenantId, documentId)
        DocumentType.BankStatement -> findConfirmedBankStatement(
            tenantId = tenantId,
            documentId = documentId,
            bankStatementRepository = bankStatementRepository,
            bankTransactionRepository = bankTransactionRepository,
        )
        else -> {
            // Try all types
            invoiceRepository.findByDocumentId(tenantId, documentId)
                ?: expenseRepository.findByDocumentId(tenantId, documentId)
                ?: creditNoteRepository.findByDocumentId(tenantId, documentId)
                ?: findConfirmedBankStatement(
                    tenantId = tenantId,
                    documentId = documentId,
                    bankStatementRepository = bankStatementRepository,
                    bankTransactionRepository = bankTransactionRepository,
                )
        }
    }
}

/**
 * Convert a confirmed entity aggregate to [DocDto].
 */
internal fun confirmedEntityToDocDto(entity: Any): DocDto = when (entity) {
    is InvoiceEntity -> DocDto.Invoice.Confirmed.from(entity)
    is ExpenseEntity -> DocDto.Receipt.Confirmed.from(entity)
    is CreditNoteEntity -> DocDto.CreditNote.Confirmed.from(entity)
    is ConfirmedBankStatement -> DocDto.BankStatement.Draft.from(entity)
    else -> error("Unsupported entity type for DocDto conversion: ${entity::class.simpleName}")
}

internal data class ConfirmedBankStatement(
    val statement: BankStatementEntity,
    val transactions: List<BankTransactionEntity>,
)

@Suppress("LongParameterList")
private suspend fun findConfirmedBankStatement(
    tenantId: TenantId,
    documentId: DocumentId,
    bankStatementRepository: BankStatementRepository,
    bankTransactionRepository: BankTransactionRepository,
): ConfirmedBankStatement? {
    val statement = bankStatementRepository.findByDocumentId(tenantId, documentId) ?: return null
    return ConfirmedBankStatement(
        statement = statement,
        transactions = bankTransactionRepository.listByDocument(tenantId, documentId),
    )
}

/**
 * Update draft counterparty (contact ID and intent).
 */
internal suspend fun updateDraftCounterparty(
    documentRepository: DocumentRepository,
    documentId: DocumentId,
    tenantId: TenantId,
    request: UpdateDraftRequest
) {
    val contactId = request.contactId?.let { ContactId.parse(it) }
    val counterparty: CounterpartyInfo = if (contactId != null) {
        CounterpartyInfo.Linked(
            contactId = contactId,
            source = ContactLinkSource.User,
        )
    } else {
        CounterpartyInfo.Unresolved(
            pendingCreation = request.pendingCreation == true,
        )
    }
    val updated = documentRepository.updateCounterparty(
        documentId = documentId,
        tenantId = tenantId,
        counterparty = counterparty,
    )
    if (!updated) {
        throw DokusException.InternalError("Failed to update document counterparty")
    }
}
