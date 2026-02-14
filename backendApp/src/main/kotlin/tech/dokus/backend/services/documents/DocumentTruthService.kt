package tech.dokus.backend.services.documents

import io.micrometer.core.instrument.Metrics
import tech.dokus.database.repository.cashflow.DocumentBlobCreatePayload
import tech.dokus.database.repository.cashflow.DocumentBlobRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentMatchReviewRepository
import tech.dokus.database.repository.cashflow.DocumentMatchReviewSummary
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentSourceCreatePayload
import tech.dokus.database.repository.cashflow.DocumentSourceRepository
import tech.dokus.database.repository.cashflow.DocumentSourceSummary
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentIntakeOutcome
import tech.dokus.domain.enums.DocumentMatchReviewReasonType
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.domain.enums.DocumentMatchType
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.DocumentIntakeOutcomeDto
import tech.dokus.domain.model.DocumentMatchResolutionDecision
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.VatBreakdownEntry
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.foundation.backend.utils.loggerFor
import java.security.MessageDigest

data class DocumentIntakeServiceResult(
    val documentId: DocumentId,
    val sourceId: DocumentSourceId,
    val runId: tech.dokus.domain.ids.IngestionRunId?,
    val outcome: DocumentIntakeOutcome,
    val linkedDocumentId: DocumentId? = null,
    val reviewId: DocumentMatchReviewId? = null,
    val sourceCount: Int = 1,
    val matchType: DocumentMatchType? = null
) {
    fun toOutcomeDto(): DocumentIntakeOutcomeDto = DocumentIntakeOutcomeDto(
        outcome = outcome,
        sourceId = sourceId,
        documentId = documentId,
        linkedDocumentId = linkedDocumentId,
        reviewId = reviewId,
        sourceCount = sourceCount,
        matchType = matchType
    )
}

data class SourceDeleteResult(
    val deleted: Boolean,
    val cascadedDocumentDelete: Boolean = false,
    val requiresConfirmation: Boolean = false
)

/**
 * Source-centric intake and matching orchestration.
 *
 * This service preserves the current Document table for canonical truth while
 * introducing evidence-level sources and match-review decisions.
 */
class DocumentTruthService(
    private val storageService: DocumentStorageService,
    private val documentRepository: DocumentRepository,
    private val ingestionRepository: DocumentIngestionRunRepository,
    private val draftRepository: DocumentDraftRepository,
    private val blobRepository: DocumentBlobRepository,
    private val sourceRepository: DocumentSourceRepository,
    private val matchReviewRepository: DocumentMatchReviewRepository
) {
    private val logger = loggerFor()

    suspend fun intakeBytes(
        tenantId: TenantId,
        filename: String,
        contentType: String,
        prefix: String,
        fileBytes: ByteArray,
        sourceChannel: DocumentSource
    ): DocumentIntakeServiceResult {
        val inputHash = sha256Hex(fileBytes)

        val blob = blobRepository.getByInputHash(tenantId, inputHash) ?: run {
            val upload = storageService.uploadDocument(
                tenantId = tenantId,
                prefix = prefix,
                filename = filename,
                data = fileBytes,
                contentType = contentType
            )
            blobRepository.createIfAbsent(
                tenantId = tenantId,
                payload = DocumentBlobCreatePayload(
                    inputHash = inputHash,
                    storageKey = upload.key,
                    contentType = upload.contentType,
                    sizeBytes = upload.sizeBytes
                )
            )
        }

        val exactMatchDocumentId = sourceRepository.findLinkedDocumentByInputHash(tenantId, inputHash)
        if (exactMatchDocumentId != null) {
            val sourceId = sourceRepository.create(
                tenantId = tenantId,
                payload = DocumentSourceCreatePayload(
                    documentId = exactMatchDocumentId,
                    blobId = blob.id,
                    sourceChannel = sourceChannel,
                    status = DocumentSourceStatus.Linked,
                    matchType = DocumentMatchType.ExactFile,
                    filename = filename
                )
            )
            val sourceCount = sourceRepository.countLinkedSources(tenantId, exactMatchDocumentId)
            recordIntakeOutcome(
                sourceChannel = sourceChannel,
                outcome = DocumentIntakeOutcome.LinkedToExisting
            )
            return DocumentIntakeServiceResult(
                documentId = exactMatchDocumentId,
                sourceId = sourceId,
                runId = null,
                outcome = DocumentIntakeOutcome.LinkedToExisting,
                linkedDocumentId = exactMatchDocumentId,
                sourceCount = sourceCount,
                matchType = DocumentMatchType.ExactFile
            )
        }

        val documentId = documentRepository.create(
            tenantId = tenantId,
            payload = tech.dokus.database.repository.cashflow.DocumentCreatePayload(
                filename = filename,
                contentType = blob.contentType,
                sizeBytes = blob.sizeBytes,
                storageKey = blob.storageKey,
                contentHash = null,
                source = sourceChannel
            )
        )

        val sourceId = sourceRepository.create(
            tenantId = tenantId,
            payload = DocumentSourceCreatePayload(
                documentId = documentId,
                blobId = blob.id,
                sourceChannel = sourceChannel,
                status = DocumentSourceStatus.Linked,
                filename = filename
            )
        )

        val runId = ingestionRepository.createRun(
            documentId = documentId,
            tenantId = tenantId,
            sourceId = sourceId
        )

        return DocumentIntakeServiceResult(
            documentId = documentId,
            sourceId = sourceId,
            runId = runId,
            outcome = DocumentIntakeOutcome.NewDocument,
            sourceCount = 1
        )
    }

    suspend fun applyPostExtractionMatching(
        tenantId: TenantId,
        documentId: DocumentId,
        sourceId: DocumentSourceId?,
        draftData: DocumentDraftData,
        extractedSnapshotJson: String
    ): DocumentIntakeServiceResult {
        val resolvedSource = resolveSource(tenantId, documentId, sourceId)
            ?: error("No source found for document=$documentId")

        val extractedContentHash = sha256Hex(extractedSnapshotJson.encodeToByteArray())
        val identity = computeIdentity(tenantId, draftData)

        sourceRepository.updateMatchingFingerprint(
            tenantId = tenantId,
            sourceId = resolvedSource.id,
            contentHash = extractedContentHash,
            identityKeyHash = identity?.hash,
            normalizedSupplierVat = identity?.normalizedSupplierVat,
            normalizedDocumentNumber = identity?.normalizedDocumentNumber,
            documentType = identity?.documentType,
            direction = identity?.direction,
            extractedSnapshotJson = extractedSnapshotJson,
            matchType = null
        )
        documentRepository.updateContentHash(tenantId, documentId, extractedContentHash)

        val contentMatchDocumentId = sourceRepository.findLinkedDocumentByContentHash(
            tenantId = tenantId,
            contentHash = extractedContentHash,
            excludeSourceId = resolvedSource.id
        )
        if (contentMatchDocumentId != null && contentMatchDocumentId != documentId) {
            val result = linkSourceToExisting(
                tenantId = tenantId,
                sourceId = resolvedSource.id,
                sourceDocumentId = documentId,
                targetDocumentId = contentMatchDocumentId,
                matchType = DocumentMatchType.SameContent
            )
            recordIntakeOutcome(resolvedSource.sourceChannel, result.outcome)
            return result
        }

        if (identity != null) {
            val identityMatchDocumentId = sourceRepository.findLinkedDocumentByIdentityKeyHash(
                tenantId = tenantId,
                identityKeyHash = identity.hash,
                excludeDocumentId = documentId
            )
            if (identityMatchDocumentId != null) {
                val existingDraft = draftRepository.getByDocumentId(identityMatchDocumentId, tenantId)?.extractedData
                val hasMaterialConflict = existingDraft?.let { hasMaterialConflict(draftData, it) } ?: false
                return if (hasMaterialConflict) {
                    val result = createPendingReview(
                        tenantId = tenantId,
                        sourceId = resolvedSource.id,
                        sourceDocumentId = documentId,
                        targetDocumentId = identityMatchDocumentId,
                        reason = DocumentMatchReviewReasonType.MaterialConflict,
                        summary = "Possible match with conflicting financial facts",
                        matchType = DocumentMatchType.SameDocument
                    )
                    recordIntakeOutcome(resolvedSource.sourceChannel, result.outcome)
                    result
                } else {
                    val result = linkSourceToExisting(
                        tenantId = tenantId,
                        sourceId = resolvedSource.id,
                        sourceDocumentId = documentId,
                        targetDocumentId = identityMatchDocumentId,
                        matchType = DocumentMatchType.SameDocument
                    )
                    recordIntakeOutcome(resolvedSource.sourceChannel, result.outcome)
                    result
                }
            }

            val fuzzyCandidates = sourceRepository.findFuzzyCandidates(
                tenantId = tenantId,
                normalizedSupplierVat = identity.normalizedSupplierVat,
                normalizedDocumentNumber = identity.normalizedDocumentNumber,
                documentType = identity.documentType,
                direction = identity.direction,
                excludeDocumentId = documentId,
                maxDistance = 2
            )
            val bestFuzzy = fuzzyCandidates.firstOrNull()
            if (bestFuzzy != null) {
                val existingDraft = draftRepository.getByDocumentId(bestFuzzy.documentId, tenantId)?.extractedData
                val fuzzyAssessment = buildFuzzyAssessment(
                    incoming = draftData,
                    existing = existingDraft
                )
                val result = createPendingReview(
                    tenantId = tenantId,
                    sourceId = resolvedSource.id,
                    sourceDocumentId = documentId,
                    targetDocumentId = bestFuzzy.documentId,
                    reason = DocumentMatchReviewReasonType.FuzzyCandidate,
                    summary = fuzzyAssessment.summary,
                    aiConfidence = fuzzyAssessment.confidence,
                    matchType = DocumentMatchType.FuzzyCandidate
                )
                recordIntakeOutcome(resolvedSource.sourceChannel, result.outcome)
                return result
            }
        }

        if (identity != null) {
            documentRepository.updateIdentityKeyHash(tenantId, documentId, identity.hash)
        }

        val sourceCount = sourceRepository.countLinkedSources(tenantId, documentId)
        val result = DocumentIntakeServiceResult(
            documentId = documentId,
            sourceId = resolvedSource.id,
            runId = null,
            outcome = DocumentIntakeOutcome.NewDocument,
            sourceCount = sourceCount
        )
        recordIntakeOutcome(resolvedSource.sourceChannel, result.outcome)
        return result
    }

    suspend fun resolveMatchReview(
        tenantId: TenantId,
        userId: UserId,
        reviewId: DocumentMatchReviewId,
        decision: DocumentMatchResolutionDecision
    ): DocumentIntakeServiceResult? {
        val review = matchReviewRepository.getById(tenantId, reviewId) ?: return null
        val source = sourceRepository.getById(tenantId, review.incomingSourceId) ?: return null

        return when (decision) {
            DocumentMatchResolutionDecision.SAME -> {
                matchReviewRepository.resolve(
                    tenantId = tenantId,
                    reviewId = reviewId,
                    status = DocumentMatchReviewStatus.ResolvedSame,
                    resolvedBy = userId
                )
                sourceRepository.reassignToDocument(
                    tenantId = tenantId,
                    sourceId = source.id,
                    documentId = review.documentId,
                    status = DocumentSourceStatus.Linked,
                    matchType = source.matchType
                )
                val sourceCount = sourceRepository.countLinkedSources(tenantId, review.documentId)
                cleanupIfOrphan(tenantId, source.documentId)
                DocumentIntakeServiceResult(
                    documentId = review.documentId,
                    sourceId = source.id,
                    runId = null,
                    outcome = DocumentIntakeOutcome.LinkedToExisting,
                    linkedDocumentId = review.documentId,
                    reviewId = reviewId,
                    sourceCount = sourceCount,
                    matchType = source.matchType
                ).also {
                    Metrics.counter(
                        "dokus_review_resolution_count",
                        "decision",
                        DocumentMatchResolutionDecision.SAME.name
                    ).increment()
                    Metrics.counter(
                        "review_resolution_same_vs_different",
                        "decision",
                        DocumentMatchResolutionDecision.SAME.name
                    ).increment()
                }
            }

            DocumentMatchResolutionDecision.DIFFERENT -> {
                matchReviewRepository.resolve(
                    tenantId = tenantId,
                    reviewId = reviewId,
                    status = DocumentMatchReviewStatus.ResolvedDifferent,
                    resolvedBy = userId
                )

                val newDocumentId = documentRepository.create(
                    tenantId = tenantId,
                    payload = tech.dokus.database.repository.cashflow.DocumentCreatePayload(
                        filename = source.filename ?: "document",
                        contentType = source.contentType,
                        sizeBytes = source.sizeBytes,
                        storageKey = source.storageKey,
                        contentHash = source.contentHash,
                        identityKeyHash = source.identityKeyHash,
                        source = source.sourceChannel
                    )
                )
                sourceRepository.reassignToDocument(
                    tenantId = tenantId,
                    sourceId = source.id,
                    documentId = newDocumentId,
                    status = DocumentSourceStatus.Linked,
                    matchType = null
                )
                source.extractedSnapshotJson?.let { snapshot ->
                    runCatching {
                        val draftData = json.decodeFromString<DocumentDraftData>(snapshot)
                        val runId = ingestionRepository.createRun(
                            documentId = newDocumentId,
                            tenantId = tenantId,
                            sourceId = source.id
                        )
                        ingestionRepository.markAsSucceeded(
                            runId = runId,
                            rawText = null,
                            rawExtractionJson = snapshot,
                            confidence = 1.0
                        )
                        draftRepository.createOrUpdateFromIngestion(
                            documentId = newDocumentId,
                            tenantId = tenantId,
                            runId = runId,
                            extractedData = draftData,
                            documentType = draftData.toDocumentType(),
                            force = true
                        )
                    }.onFailure { error ->
                        logger.warn(
                            "Failed to seed draft on review split for document {} source {}",
                            newDocumentId,
                            source.id,
                            error
                        )
                    }
                }
                cleanupIfOrphan(tenantId, source.documentId)

                DocumentIntakeServiceResult(
                    documentId = newDocumentId,
                    sourceId = source.id,
                    runId = null,
                    outcome = DocumentIntakeOutcome.NewDocument,
                    reviewId = reviewId,
                    sourceCount = 1
                ).also {
                    Metrics.counter(
                        "dokus_review_resolution_count",
                        "decision",
                        DocumentMatchResolutionDecision.DIFFERENT.name
                    ).increment()
                    Metrics.counter(
                        "review_resolution_same_vs_different",
                        "decision",
                        DocumentMatchResolutionDecision.DIFFERENT.name
                    ).increment()
                }
            }
        }
    }

    suspend fun listSources(tenantId: TenantId, documentId: DocumentId): List<DocumentSourceSummary> {
        return sourceRepository.listByDocument(tenantId, documentId)
    }

    suspend fun getPendingReviewByDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ): DocumentMatchReviewSummary? {
        return matchReviewRepository.listPendingByDocumentIds(tenantId, listOf(documentId))[documentId]
    }

    suspend fun getPendingReviewsByDocuments(
        tenantId: TenantId,
        documentIds: List<DocumentId>
    ): Map<DocumentId, DocumentMatchReviewSummary> {
        return matchReviewRepository.listPendingByDocumentIds(tenantId, documentIds)
    }

    suspend fun deleteSource(
        tenantId: TenantId,
        documentId: DocumentId,
        sourceId: DocumentSourceId,
        confirmLastOnConfirmed: Boolean
    ): SourceDeleteResult {
        val source = sourceRepository.getById(tenantId, sourceId) ?: return SourceDeleteResult(deleted = false)
        if (source.documentId != documentId) return SourceDeleteResult(deleted = false)

        val linkedCount = sourceRepository.countLinkedSources(tenantId, documentId)
        val draft = draftRepository.getByDocumentId(documentId, tenantId)
        val isConfirmed = draft?.documentStatus == tech.dokus.domain.enums.DocumentStatus.Confirmed

        if (linkedCount <= 1 && isConfirmed && !confirmLastOnConfirmed) {
            return SourceDeleteResult(
                deleted = false,
                requiresConfirmation = true
            )
        }

        val deleted = sourceRepository.deleteById(tenantId, sourceId)
        if (!deleted) return SourceDeleteResult(deleted = false)
        Metrics.counter("dokus_source_detach_count").increment()
        Metrics.counter("source_detach_count").increment()
        if (linkedCount <= 1 && isConfirmed && confirmLastOnConfirmed) {
            Metrics.counter("dokus_last_source_delete_confirmed_count").increment()
            Metrics.counter("last_source_delete_confirmed_count").increment()
        }

        val remainingSources = sourceRepository.countSources(
            tenantId = tenantId,
            documentId = documentId,
            includeDetached = true
        )
        if (remainingSources == 0 && !isConfirmed) {
            documentRepository.delete(tenantId, documentId)
            return SourceDeleteResult(
                deleted = true,
                cascadedDocumentDelete = true
            )
        }

        return SourceDeleteResult(deleted = true)
    }

    private suspend fun resolveSource(
        tenantId: TenantId,
        documentId: DocumentId,
        sourceId: DocumentSourceId?
    ): DocumentSourceSummary? {
        if (sourceId != null) {
            return sourceRepository.getById(tenantId, sourceId)
        }
        return sourceRepository.listByDocument(tenantId, documentId)
            .maxByOrNull { it.arrivalAt }
    }

    private suspend fun linkSourceToExisting(
        tenantId: TenantId,
        sourceId: DocumentSourceId,
        sourceDocumentId: DocumentId,
        targetDocumentId: DocumentId,
        matchType: DocumentMatchType
    ): DocumentIntakeServiceResult {
        sourceRepository.reassignToDocument(
            tenantId = tenantId,
            sourceId = sourceId,
            documentId = targetDocumentId,
            status = DocumentSourceStatus.Linked,
            matchType = matchType
        )
        cleanupIfOrphan(tenantId, sourceDocumentId)
        val sourceCount = sourceRepository.countLinkedSources(tenantId, targetDocumentId)
        return DocumentIntakeServiceResult(
            documentId = targetDocumentId,
            sourceId = sourceId,
            runId = null,
            outcome = DocumentIntakeOutcome.LinkedToExisting,
            linkedDocumentId = targetDocumentId,
            sourceCount = sourceCount,
            matchType = matchType
        )
    }

    private suspend fun createPendingReview(
        tenantId: TenantId,
        sourceId: DocumentSourceId,
        sourceDocumentId: DocumentId,
        targetDocumentId: DocumentId,
        reason: DocumentMatchReviewReasonType,
        summary: String,
        aiConfidence: Double? = null,
        matchType: DocumentMatchType
    ): DocumentIntakeServiceResult {
        sourceRepository.reassignToDocument(
            tenantId = tenantId,
            sourceId = sourceId,
            documentId = targetDocumentId,
            status = DocumentSourceStatus.PendingReview,
            matchType = matchType
        )
        val reviewId = matchReviewRepository.createPending(
            tenantId = tenantId,
            documentId = targetDocumentId,
            sourceId = sourceId,
            reasonType = reason,
            aiSummary = summary,
            aiConfidence = aiConfidence
        )
        cleanupIfOrphan(tenantId, sourceDocumentId)
        val sourceCount = sourceRepository.countLinkedSources(tenantId, targetDocumentId)
        return DocumentIntakeServiceResult(
            documentId = targetDocumentId,
            sourceId = sourceId,
            runId = null,
            outcome = DocumentIntakeOutcome.PendingMatchReview,
            linkedDocumentId = targetDocumentId,
            reviewId = reviewId,
            sourceCount = sourceCount,
            matchType = matchType
        )
    }

    private suspend fun cleanupIfOrphan(tenantId: TenantId, documentId: DocumentId) {
        val remainingSources = sourceRepository.countSources(
            tenantId = tenantId,
            documentId = documentId,
            includeDetached = true
        )
        if (remainingSources == 0) {
            logger.info("Removing orphaned document after source reassignment: {}", documentId)
            documentRepository.delete(tenantId, documentId)
        }
    }

    private data class IdentityDescriptor(
        val hash: String,
        val normalizedSupplierVat: String,
        val normalizedDocumentNumber: String,
        val documentType: DocumentType,
        val direction: DocumentDirection
    )

    private fun computeIdentity(tenantId: TenantId, data: DocumentDraftData): IdentityDescriptor? {
        return when (data) {
            is InvoiceDraftData -> {
                if (data.direction == DocumentDirection.Unknown) return null
                val supplierVat = when (data.direction) {
                    DocumentDirection.Inbound -> data.seller.vat
                    DocumentDirection.Outbound -> data.buyer.vat
                    DocumentDirection.Unknown -> null
                }?.takeIf { it.isValid } ?: return null
                val number = normalizeDocumentNumber(data.invoiceNumber) ?: return null
                buildIdentityDescriptor(
                    tenantId = tenantId,
                    documentType = DocumentType.Invoice,
                    direction = data.direction,
                    supplierVat = supplierVat,
                    documentNumber = number
                )
            }

            is CreditNoteDraftData -> {
                if (data.direction == DocumentDirection.Unknown) return null
                val supplierVat = when (data.direction) {
                    DocumentDirection.Inbound -> data.seller.vat
                    DocumentDirection.Outbound -> data.buyer.vat
                    DocumentDirection.Unknown -> null
                }?.takeIf { it.isValid } ?: return null
                val number = normalizeDocumentNumber(data.creditNoteNumber) ?: return null
                buildIdentityDescriptor(
                    tenantId = tenantId,
                    documentType = DocumentType.CreditNote,
                    direction = data.direction,
                    supplierVat = supplierVat,
                    documentNumber = number
                )
            }

            else -> null
        }
    }

    private fun buildIdentityDescriptor(
        tenantId: TenantId,
        documentType: DocumentType,
        direction: DocumentDirection,
        supplierVat: VatNumber,
        documentNumber: String
    ): IdentityDescriptor {
        val normalizedVat = VatNumber.normalize(supplierVat.value)
        val identityRaw = listOf(
            tenantId.toString(),
            documentType.dbValue,
            direction.dbValue,
            normalizedVat,
            documentNumber
        ).joinToString("|")
        return IdentityDescriptor(
            hash = sha256Hex(identityRaw.encodeToByteArray()),
            normalizedSupplierVat = normalizedVat,
            normalizedDocumentNumber = documentNumber,
            documentType = documentType,
            direction = direction
        )
    }

    private fun normalizeDocumentNumber(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.trim()
            .uppercase()
            .replace(Regex("[\\s\\p{Punct}]"), "")
        return normalized.ifBlank { null }
    }

    private fun hasMaterialConflict(
        incoming: DocumentDraftData,
        existing: DocumentDraftData
    ): Boolean {
        return when {
            incoming is InvoiceDraftData && existing is InvoiceDraftData -> {
                moneyConflict(incoming.totalAmount, existing.totalAmount) ||
                    incoming.currency != existing.currency ||
                    incoming.issueDate != existing.issueDate ||
                    vatSummary(incoming.vatBreakdown) != vatSummary(existing.vatBreakdown)
            }

            incoming is CreditNoteDraftData && existing is CreditNoteDraftData -> {
                moneyConflict(incoming.totalAmount, existing.totalAmount) ||
                    incoming.currency != existing.currency ||
                    incoming.issueDate != existing.issueDate ||
                    vatSummary(incoming.vatBreakdown) != vatSummary(existing.vatBreakdown)
            }

            else -> true
        }
    }

    private fun moneyConflict(left: Money?, right: Money?): Boolean {
        if (left == null || right == null) return false
        return left.minor != right.minor
    }

    private fun vatSummary(entries: List<VatBreakdownEntry>): List<String> {
        return entries
            .map { "${it.rate}:${it.base}:${it.amount}" }
            .sorted()
    }

    private data class FuzzyAssessment(
        val summary: String,
        val confidence: Double?
    )

    private fun buildFuzzyAssessment(
        incoming: DocumentDraftData,
        existing: DocumentDraftData?
    ): FuzzyAssessment {
        if (existing == null) {
            return FuzzyAssessment(
                summary = "Possible match: supplier VAT matches and document number is similar",
                confidence = 0.55
            )
        }

        val score = when {
            incoming is InvoiceDraftData && existing is InvoiceDraftData -> {
                fuzzyScore(
                    sameCurrency = incoming.currency == existing.currency,
                    sameIssueDate = incoming.issueDate == existing.issueDate,
                    sameAmount = !moneyConflict(incoming.totalAmount, existing.totalAmount),
                    sameVatSummary = vatSummary(incoming.vatBreakdown) == vatSummary(existing.vatBreakdown),
                    sameLineCount = incoming.lineItems.size == existing.lineItems.size
                )
            }

            incoming is CreditNoteDraftData && existing is CreditNoteDraftData -> {
                fuzzyScore(
                    sameCurrency = incoming.currency == existing.currency,
                    sameIssueDate = incoming.issueDate == existing.issueDate,
                    sameAmount = !moneyConflict(incoming.totalAmount, existing.totalAmount),
                    sameVatSummary = vatSummary(incoming.vatBreakdown) == vatSummary(existing.vatBreakdown),
                    sameLineCount = incoming.lineItems.size == existing.lineItems.size
                )
            }

            else -> 0.55
        }

        val roundedScore = ((score * 100).toInt() / 100.0).coerceIn(0.0, 0.99)
        return FuzzyAssessment(
            summary = "Possible match: supplier VAT matches and document number is similar",
            confidence = roundedScore
        )
    }

    private fun fuzzyScore(
        sameCurrency: Boolean,
        sameIssueDate: Boolean,
        sameAmount: Boolean,
        sameVatSummary: Boolean,
        sameLineCount: Boolean
    ): Double {
        var score = 0.45
        if (sameAmount) score += 0.2
        if (sameIssueDate) score += 0.15
        if (sameCurrency) score += 0.1
        if (sameVatSummary) score += 0.08
        if (sameLineCount) score += 0.02
        return score.coerceAtMost(0.99)
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun recordIntakeOutcome(
        sourceChannel: DocumentSource,
        outcome: DocumentIntakeOutcome
    ) {
        Metrics.counter(
            "dokus_intake_outcome_count",
            "outcome",
            outcome.name,
            "channel",
            sourceChannel.name
        ).increment()
        Metrics.counter(
            "intake_outcome_count",
            "outcome",
            outcome.name,
            "channel",
            sourceChannel.name
        ).increment()
        when (outcome) {
            DocumentIntakeOutcome.LinkedToExisting -> {
                Metrics.counter("dokus_silent_link_count", "channel", sourceChannel.name).increment()
                Metrics.counter("silent_link_rate", "channel", sourceChannel.name).increment()
            }

            DocumentIntakeOutcome.PendingMatchReview -> {
                Metrics.counter("dokus_needs_review_count", "channel", sourceChannel.name).increment()
                Metrics.counter("needs_review_rate", "channel", sourceChannel.name).increment()
            }

            DocumentIntakeOutcome.NewDocument -> Unit
        }
    }

    private fun DocumentDraftData.toDocumentType(): DocumentType = when (this) {
        is InvoiceDraftData -> DocumentType.Invoice
        is CreditNoteDraftData -> DocumentType.CreditNote
        else -> DocumentType.Unknown
    }
}
