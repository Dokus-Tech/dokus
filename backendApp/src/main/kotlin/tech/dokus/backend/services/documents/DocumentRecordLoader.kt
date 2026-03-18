package tech.dokus.backend.services.documents

import tech.dokus.backend.routes.cashflow.documents.addDownloadUrl
import tech.dokus.backend.routes.cashflow.documents.confirmedEntityToDocDto
import tech.dokus.backend.routes.cashflow.documents.findConfirmedEntity
import tech.dokus.backend.routes.cashflow.documents.toDto
import tech.dokus.backend.routes.cashflow.documents.toSummaryDto
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DraftSummary
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.repository.cashflow.selectPreferredSource
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.repository.drafts.DraftRepository
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.contact.ContactSuggestionDto
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.model.toDocDto
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.foundation.backend.utils.loggerFor

@Suppress("LongParameterList")
internal class DocumentRecordLoader(
    private val documentRepository: DocumentRepository,
    private val ingestionRepository: DocumentIngestionRunRepository,
    private val invoiceRepository: InvoiceRepository,
    private val expenseRepository: ExpenseRepository,
    private val creditNoteRepository: CreditNoteRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository,
    private val contactRepository: ContactRepository,
    private val draftRepository: DraftRepository,
    private val truthService: DocumentTruthService,
    private val documentStorageService: DocumentStorageService,
) {
    private val logger = loggerFor<DocumentRecordLoader>()

    suspend fun load(
        tenantId: TenantId,
        documentId: DocumentId,
    ): DocumentDetailDto? {
        val document = documentRepository.getById(tenantId, documentId) ?: return null
        val sources = truthService.listSources(tenantId, documentId)
        val preferredSource = selectPreferredSource(sources)
        val effectiveDocument = if (preferredSource != null) {
            document.copy(
                filename = preferredSource.filename ?: document.filename,
                uploadedAt = preferredSource.arrivalAt,
            )
        } else {
            document
        }

        val documentWithUrl = addDownloadUrl(effectiveDocument, preferredSource?.storageKey, documentStorageService, logger)
        val draft = documentRepository.getDraftByDocumentId(documentId, tenantId)
        val resolvedContact = resolveContact(draft, tenantId)
        val contactSuggestions = buildContactSuggestions(draft)
        val latestIngestion = ingestionRepository.getLatestForDocument(documentId, tenantId)
        val pendingReview = truthService.getPendingReviewByDocument(tenantId, documentId)
        // Build DocDto content: Confirmed variant if confirmed, Draft variant otherwise
        val confirmedEntity = if (draft?.documentStatus == DocumentStatus.Confirmed) {
            findConfirmedEntity(
                documentId = documentId,
                documentType = draft.documentType,
                tenantId = tenantId,
                invoiceRepository = invoiceRepository,
                expenseRepository = expenseRepository,
                creditNoteRepository = creditNoteRepository,
            )
        } else {
            null
        }
        val content = if (confirmedEntity != null) {
            confirmedEntityToDocDto(confirmedEntity)
        } else {
            // Draft tables first, JSON blob fallback during migration
            draftRepository.getDraftAsDocDto(tenantId, documentId, draft?.documentType)
                ?: draft?.extractedData?.toDocDto()
        }

        val cashflowEntryId = if (draft?.documentStatus == DocumentStatus.Confirmed) {
            cashflowEntriesRepository.getByDocumentId(tenantId, documentId).getOrNull()?.id
        } else {
            null
        }

        return DocumentDetailDto(
            document = documentWithUrl,
            draft = draft?.toDto(resolvedContact, contactSuggestions, content),
            latestIngestion = latestIngestion?.toDto(
                includeRawExtraction = true,
                includeTrace = true,
            ),
            cashflowEntryId = cashflowEntryId,
            pendingMatchReview = pendingReview?.toSummaryDto(),
            sources = sources.map { it.toDto() },
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun resolveContact(
        draft: DraftSummary?,
        tenantId: TenantId,
    ): ResolvedContact {
        val counterparty = draft?.counterparty ?: return ResolvedContact.Unknown

        return when (counterparty) {
            is CounterpartyInfo.Linked -> {
                val contact = try {
                    contactRepository.getContact(counterparty.contactId, tenantId).getOrNull()
                } catch (e: Exception) {
                    logger.warn("Failed to load contact ${counterparty.contactId}: ${e.message}")
                    null
                }
                if (contact != null) {
                    ResolvedContact.Linked(
                        contactId = contact.id,
                        name = contact.name.value,
                        vatNumber = contact.vatNumber?.value,
                        email = contact.email?.value,
                        avatarPath = contact.avatar?.small,
                    )
                } else {
                    ResolvedContact.Unknown
                }
            }

            is CounterpartyInfo.Unresolved -> {
                val topSuggestion = counterparty.suggestions.firstOrNull()
                if (topSuggestion != null) {
                    ResolvedContact.Suggested(
                        contactId = topSuggestion.contactId,
                        name = topSuggestion.name,
                        vatNumber = topSuggestion.vatNumber?.value,
                    )
                } else {
                    val snapshot = counterparty.snapshot
                    val name = snapshot?.name?.trim()?.takeIf { it.isNotEmpty() }
                    if (name != null) {
                        ResolvedContact.Detected(
                            name = name,
                            vatNumber = snapshot.vatNumber?.value,
                            iban = snapshot.iban?.value,
                            address = formatAddress(snapshot),
                        )
                    } else {
                        ResolvedContact.Unknown
                    }
                }
            }
        }
    }

    private fun formatAddress(snapshot: CounterpartySnapshot): String? {
        val addr = snapshot.address
        val parts = listOfNotNull(
            addr.streetLine1?.trim()?.takeIf { it.isNotEmpty() },
            addr.streetLine2?.trim()?.takeIf { it.isNotEmpty() },
            listOfNotNull(
                addr.postalCode?.trim()?.takeIf { it.isNotEmpty() },
                addr.city?.trim()?.takeIf { it.isNotEmpty() },
            ).joinToString(" ").takeIf { it.isNotEmpty() },
            addr.country?.dbValue?.trim()?.takeIf { it.isNotEmpty() },
        )
        return parts.joinToString(", ").takeIf { it.isNotEmpty() }
    }

    private fun buildContactSuggestions(draft: DraftSummary?): List<ContactSuggestionDto> {
        val counterparty = draft?.counterparty as? CounterpartyInfo.Unresolved ?: return emptyList()
        return counterparty.suggestions.take(3).map { suggestion ->
            ContactSuggestionDto(
                contactId = suggestion.contactId,
                name = suggestion.name,
                vatNumber = suggestion.vatNumber?.value,
            )
        }
    }
}
