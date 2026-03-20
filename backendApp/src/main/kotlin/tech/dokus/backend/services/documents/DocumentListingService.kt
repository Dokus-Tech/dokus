package tech.dokus.backend.services.documents

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentSourceRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.repository.drafts.DraftRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentCountsResponse
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Service for document listing and count queries.
 * Centralizes the parallel enrichment logic previously spread across routes.
 */
class DocumentListingService(
    private val documentRepository: DocumentRepository,
    private val invoiceRepository: InvoiceRepository,
    private val expenseRepository: ExpenseRepository,
    private val creditNoteRepository: CreditNoteRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository,
    private val documentSourceRepository: DocumentSourceRepository,
    private val draftRepository: DraftRepository,
    private val truthService: DocumentTruthService,
    private val storageService: DocumentStorageService,
) {
    private val logger = loggerFor()

    suspend fun listDocuments(
        tenantId: TenantId,
        filter: DocumentListFilter?,
        documentStatus: DocumentStatus?,
        documentType: DocumentType?,
        ingestionStatus: IngestionStatus?,
        sortBy: String?,
        page: Int,
        limit: Int,
    ): PaginatedResponse<DocumentListItemDto> {
        val (documentsWithInfo, total) = documentRepository.listWithDraftsAndIngestion(
            tenantId = tenantId,
            filter = filter,
            documentStatus = documentStatus,
            documentType = documentType,
            ingestionStatus = ingestionStatus,
            sortBy = sortBy,
            page = page,
            limit = limit
        )

        val documentIds = documentsWithInfo.map { it.document.id }
        val confirmedDocumentIds = documentsWithInfo
            .filter { it.draft?.documentStatus == DocumentStatus.Confirmed }
            .map { it.document.id }

        // Run independent enrichment queries in parallel
        val (cashflowEntryIdsByDocumentId, pendingReviewsByDocumentId, sourceEnrichment) = coroutineScope {
            val cashflowDeferred = async {
                cashflowEntriesRepository.getIdsByDocumentIds(tenantId, confirmedDocumentIds).getOrThrow()
            }
            val reviewsDeferred = async {
                truthService.getPendingReviewsByDocuments(tenantId = tenantId, documentIds = documentIds)
            }
            val sourcesDeferred = async {
                documentSourceRepository.selectPreferredSourcesByDocumentIds(tenantId, documentIds)
            }
            Triple(cashflowDeferred.await(), reviewsDeferred.await(), sourcesDeferred.await())
        }

        // Generate download URLs in parallel
        val downloadUrlsByDocumentId = coroutineScope {
            sourceEnrichment.map { (docId, source) ->
                async {
                    val url = try {
                        storageService.getDownloadUrl(source.storageKey)
                    } catch (e: RuntimeException) {
                        logger.warn("Failed to get download URL for ${source.storageKey}: ${e.message}")
                        null
                    }
                    docId to url
                }
            }.awaitAll().toMap()
        }

        // Batch-fetch amounts: confirmed entities first, draft tables as fallback
        val confirmedAmounts = mutableMapOf<DocumentId, Pair<Money?, Currency?>>()

        if (confirmedDocumentIds.isNotEmpty()) {
            confirmedAmounts += invoiceRepository.batchGetAmountsByDocumentIds(tenantId, confirmedDocumentIds)
            confirmedAmounts += expenseRepository.batchGetAmountsByDocumentIds(tenantId, confirmedDocumentIds)
            confirmedAmounts += creditNoteRepository.batchGetAmountsByDocumentIds(tenantId, confirmedDocumentIds)
        }

        // For unconfirmed documents, fall back to draft tables
        val unconfirmedDocs = documentsWithInfo
            .filter { it.document.id !in confirmedAmounts }
            .map { it.document.id to it.draft?.documentType }
        val draftAmounts = draftRepository.batchGetAmounts(tenantId, unconfirmedDocs)

        // Merge: confirmed takes priority
        val allAmounts = draftAmounts + confirmedAmounts

        // Build flat list items
        val items = documentsWithInfo.map { docInfo ->
            val draft = docInfo.draft
            val preferredSource = sourceEnrichment[docInfo.document.id]
            val (amount, currency) = allAmounts[docInfo.document.id] ?: (null to null)
            DocumentListItemDto(
                documentId = docInfo.document.id,
                tenantId = docInfo.document.tenantId,
                filename = preferredSource?.filename ?: docInfo.document.filename,
                documentType = draft?.documentType,
                direction = draft?.direction,
                documentStatus = draft?.documentStatus,
                ingestionStatus = docInfo.latestIngestion?.status,
                effectiveOrigin = preferredSource?.sourceChannel ?: DocumentSource.Upload,
                uploadedAt = preferredSource?.arrivalAt ?: docInfo.document.uploadedAt,
                sortDate = docInfo.document.sortDate,
                counterpartyDisplayName = draft?.counterpartyDisplayName,
                purposeRendered = draft?.purposeRendered,
                totalAmount = amount,
                currency = currency,
                downloadUrl = downloadUrlsByDocumentId[docInfo.document.id],
                hasPendingMatchReview = pendingReviewsByDocumentId.containsKey(docInfo.document.id),
                cashflowEntryId = cashflowEntryIdsByDocumentId[docInfo.document.id],
            )
        }

        return PaginatedResponse(
            items = items,
            total = total,
            limit = limit,
            offset = page * limit
        )
    }

    suspend fun getDocumentCounts(tenantId: TenantId): DocumentCountsResponse {
        val counts = documentRepository.getOperationalCounts(tenantId)
        return DocumentCountsResponse(
            total = counts.total,
            needsAttention = counts.needsAttention,
            confirmed = counts.confirmed
        )
    }
}
