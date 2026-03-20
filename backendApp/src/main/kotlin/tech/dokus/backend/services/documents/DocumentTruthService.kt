package tech.dokus.backend.services.documents

import io.micrometer.core.instrument.Metrics
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.repository.cashflow.DocumentBlobCreatePayload
import tech.dokus.database.repository.cashflow.DocumentCreatePayload
import tech.dokus.database.repository.cashflow.DocumentBlobRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentMatchReviewRepository
import tech.dokus.database.entity.DocumentMatchReviewEntity
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentSourceRepository
import tech.dokus.database.repository.cashflow.selectPreferredSource
import tech.dokus.database.entity.DocumentSourceEntity
import tech.dokus.database.repository.drafts.DraftRepository
import tech.dokus.backend.mappers.from
import tech.dokus.domain.model.from
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.IntakeOutcome
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.domain.enums.SourceMatchKind
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.AnnualAccountsDraftData
import tech.dokus.domain.model.BankFeeDraftData
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BoardMinutesDraftData
import tech.dokus.domain.model.C4DraftData
import tech.dokus.domain.model.CompanyExtractDraftData
import tech.dokus.domain.model.ContractDraftData
import tech.dokus.domain.model.CorporateTaxAdvanceDraftData
import tech.dokus.domain.model.CorporateTaxDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.CustomsDeclarationDraftData
import tech.dokus.domain.model.DeliveryNoteDraftData
import tech.dokus.domain.model.DepreciationScheduleDraftData
import tech.dokus.domain.model.DimonaDraftData
import tech.dokus.domain.model.DividendDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.DocumentIntakeOutcomeDto
import tech.dokus.domain.model.DocumentMatchResolutionDecision
import tech.dokus.domain.model.EmploymentContractDraftData
import tech.dokus.domain.model.ExpenseClaimDraftData
import tech.dokus.domain.model.FineDraftData
import tech.dokus.domain.model.HolidayPayDraftData
import tech.dokus.domain.model.IcListingDraftData
import tech.dokus.domain.model.InsuranceDraftData
import tech.dokus.domain.model.InterestStatementDraftData
import tech.dokus.domain.model.IntrastatDraftData
import tech.dokus.domain.model.InventoryDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.LeaseDraftData
import tech.dokus.domain.model.LoanDraftData
import tech.dokus.domain.model.OrderConfirmationDraftData
import tech.dokus.domain.model.OssReturnDraftData
import tech.dokus.domain.model.OtherDraftData
import tech.dokus.domain.model.PaymentConfirmationDraftData
import tech.dokus.domain.model.PayrollSummaryDraftData
import tech.dokus.domain.model.PermitDraftData
import tech.dokus.domain.model.PersonalTaxDraftData
import tech.dokus.domain.model.ProFormaDraftData
import tech.dokus.domain.model.PurchaseOrderDraftData
import tech.dokus.domain.model.QuoteDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.ReminderDraftData
import tech.dokus.domain.model.SalarySlipDraftData
import tech.dokus.domain.model.SelfEmployedContributionDraftData
import tech.dokus.domain.model.ShareholderRegisterDraftData
import tech.dokus.domain.model.SocialContributionDraftData
import tech.dokus.domain.model.SocialFundDraftData
import tech.dokus.domain.model.StatementOfAccountDraftData
import tech.dokus.domain.model.SubsidyDraftData
import tech.dokus.domain.model.TaxAssessmentDraftData
import tech.dokus.domain.model.VapzDraftData
import tech.dokus.domain.model.VatAssessmentDraftData
import tech.dokus.domain.model.VatBreakdownEntryDto
import tech.dokus.domain.model.VatListingDraftData
import tech.dokus.domain.model.VatReturnDraftData
import tech.dokus.domain.model.WithholdingTaxDraftData
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentSourceDto
import tech.dokus.domain.model.toDocumentType
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.security.MessageDigest
import java.util.UUID

/**
 * Type-safe intake resolution that encodes the dedup decision tree.
 * Replaces the separate outcome + matchType enum combination.
 */
sealed interface IntakeResolution {
    /** Brand new document, no match found */
    data object NewDocument : IntakeResolution

    /** Source silently linked to existing document */
    sealed interface Linked : IntakeResolution {
        val linkedDocumentId: DocumentId
        data class ExactFile(override val linkedDocumentId: DocumentId) : Linked
        data class SameContent(override val linkedDocumentId: DocumentId) : Linked
        data class IdentityMatch(override val linkedDocumentId: DocumentId) : Linked
    }

    /** Source requires manual review before linking */
    sealed interface NeedsReview : IntakeResolution {
        val candidateDocumentId: DocumentId
        val reviewId: DocumentMatchReviewId
        data class MaterialConflict(
            override val candidateDocumentId: DocumentId,
            override val reviewId: DocumentMatchReviewId,
        ) : NeedsReview
        data class FuzzyCandidate(
            override val candidateDocumentId: DocumentId,
            override val reviewId: DocumentMatchReviewId,
        ) : NeedsReview
    }

}

data class DocumentIntakeServiceResult(
    val documentId: DocumentId,
    val sourceId: DocumentSourceId,
    val runId: IngestionRunId?,
    val resolution: IntakeResolution,
    val sourceDocumentId: DocumentId? = null,
    val sourceCount: Int = 1,
    val orphanedDocumentId: DocumentId? = null,
) {
    fun toOutcomeDto(): DocumentIntakeOutcomeDto {
        val outcome = when (resolution) {
            is IntakeResolution.NewDocument -> IntakeOutcome.NewDocument
            is IntakeResolution.Linked -> IntakeOutcome.LinkedToExisting
            is IntakeResolution.NeedsReview -> IntakeOutcome.PendingMatchReview
        }
        val matchType = when (resolution) {
            is IntakeResolution.NewDocument -> null
            is IntakeResolution.Linked.ExactFile -> SourceMatchKind.ExactFile
            is IntakeResolution.Linked.SameContent -> SourceMatchKind.SameContent
            is IntakeResolution.Linked.IdentityMatch -> SourceMatchKind.SameDocument
            is IntakeResolution.NeedsReview.MaterialConflict -> SourceMatchKind.SameDocument
            is IntakeResolution.NeedsReview.FuzzyCandidate -> SourceMatchKind.FuzzyCandidate
        }
        val linkedDocumentId = when (resolution) {
            is IntakeResolution.Linked -> resolution.linkedDocumentId
            is IntakeResolution.NeedsReview -> resolution.candidateDocumentId
            is IntakeResolution.NewDocument -> null
        }
        val reviewId = (resolution as? IntakeResolution.NeedsReview)?.reviewId
        return DocumentIntakeOutcomeDto(
            outcome = outcome,
            sourceId = sourceId,
            documentId = documentId,
            linkedDocumentId = linkedDocumentId,
            reviewId = reviewId,
            sourceCount = sourceCount,
            matchType = matchType
        )
    }
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
    private val blobRepository: DocumentBlobRepository,
    private val sourceRepository: DocumentSourceRepository,
    private val matchReviewRepository: DocumentMatchReviewRepository,
    private val draftRepository: DraftRepository,
) {
    private val logger = loggerFor()

    private companion object {
        // Product toggle: fuzzy candidate matching is controlled in code, not env/config.
        const val FuzzyMatchingEnabled = true
    }

    suspend fun intakeBytes(
        tenantId: TenantId,
        filename: String,
        contentType: String,
        prefix: String,
        fileBytes: ByteArray,
        sourceChannel: DocumentSource
    ): DocumentIntakeServiceResult {
        val inputHash = sha256Hex(fileBytes)

        // Phase 1: Ensure blob exists in storage + DB (idempotent, safe outside lock).
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

        // Phase 2: Atomic check-and-create within a single transaction + advisory lock.
        // This prevents concurrent uploads of the same file from creating duplicate documents.
        // We inline the SQL here (bypassing repos) because each repo method opens its own
        // transaction, which would break atomicity. The advisory lock is scoped to this
        // transaction and released on commit.
        return newSuspendedTransaction {
            val lockKey = ("intake:$tenantId:$inputHash").hashCode().toLong()
            exec("SELECT pg_advisory_xact_lock($lockKey)")

            val tenantUuid = UUID.fromString(tenantId.toString())

            // Check for existing linked source with same input hash
            val existingDocId = DocumentSourcesTable
                .join(DocumentBlobsTable, JoinType.INNER, DocumentSourcesTable.blobId, DocumentBlobsTable.id)
                .selectAll()
                .where {
                    (DocumentSourcesTable.tenantId eq tenantUuid) and
                        (DocumentBlobsTable.inputHash eq inputHash) and
                        (DocumentSourcesTable.status eq DocumentSourceStatus.Linked)
                }
                .orderBy(DocumentSourcesTable.arrivalAt, SortOrder.DESC)
                .firstOrNull()
                ?.let { DocumentId.parse(it[DocumentSourcesTable.documentId].toString()) }

            if (existingDocId != null) {
                val sourceId = DocumentSourceId.generate()
                DocumentSourcesTable.insert {
                    it[id] = UUID.fromString(sourceId.toString())
                    it[DocumentSourcesTable.tenantId] = tenantUuid
                    it[documentId] = UUID.fromString(existingDocId.toString())
                    it[blobId] = UUID.fromString(blob.id.toString())
                    it[DocumentSourcesTable.sourceChannel] = sourceChannel
                    it[status] = DocumentSourceStatus.Linked
                    it[matchType] = SourceMatchKind.ExactFile
                    it[DocumentSourcesTable.filename] = filename
                }
                val sourceCount = DocumentSourcesTable.selectAll()
                    .where {
                        (DocumentSourcesTable.tenantId eq tenantUuid) and
                            (DocumentSourcesTable.documentId eq UUID.fromString(existingDocId.toString())) and
                            (DocumentSourcesTable.status eq DocumentSourceStatus.Linked)
                    }
                    .count()
                    .toInt()

                val resolution = IntakeResolution.Linked.ExactFile(linkedDocumentId = existingDocId)
                recordIntakeOutcome(sourceChannel, resolution)
                DocumentIntakeServiceResult(
                    documentId = existingDocId,
                    sourceId = sourceId,
                    runId = null,
                    resolution = resolution,
                    sourceDocumentId = existingDocId,
                    sourceCount = sourceCount,
                )
            } else {
                val documentId = DocumentId.generate()
                DocumentsTable.insert {
                    it[DocumentsTable.id] = UUID.fromString(documentId.toString())
                    it[DocumentsTable.tenantId] = tenantUuid
                }

                val sourceId = DocumentSourceId.generate()
                DocumentSourcesTable.insert {
                    it[id] = UUID.fromString(sourceId.toString())
                    it[DocumentSourcesTable.tenantId] = tenantUuid
                    it[DocumentSourcesTable.documentId] = UUID.fromString(documentId.toString())
                    it[DocumentSourcesTable.blobId] = UUID.fromString(blob.id.toString())
                    it[DocumentSourcesTable.sourceChannel] = sourceChannel
                    it[status] = DocumentSourceStatus.Linked
                    it[DocumentSourcesTable.filename] = filename
                }

                val runId = IngestionRunId.generate()
                DocumentIngestionRunsTable.insert {
                    it[DocumentIngestionRunsTable.id] = UUID.fromString(runId.toString())
                    it[DocumentIngestionRunsTable.documentId] = UUID.fromString(documentId.toString())
                    it[DocumentIngestionRunsTable.tenantId] = tenantUuid
                    it[DocumentIngestionRunsTable.sourceId] = UUID.fromString(sourceId.toString())
                    it[DocumentIngestionRunsTable.status] = IngestionStatus.Queued
                }

                DocumentIntakeServiceResult(
                    documentId = documentId,
                    sourceId = sourceId,
                    runId = runId,
                    resolution = IntakeResolution.NewDocument,
                    sourceDocumentId = documentId,
                    sourceCount = 1
                )
            }
        }
    }

    suspend fun persistPeppolSourceEnvelope(
        tenantId: TenantId,
        sourceId: DocumentSourceId,
        structuredSnapshotJson: String,
        snapshotVersion: Int,
        rawUblXml: String?
    ): Boolean {
        val rawUblBlobId = rawUblXml
            ?.takeIf { it.isNotBlank() }
            ?.let { xml ->
                val xmlBytes = xml.encodeToByteArray()
                val inputHash = sha256Hex(xmlBytes)
                val existingBlob = blobRepository.getByInputHash(tenantId, inputHash)
                if (existingBlob != null) {
                    existingBlob.id
                } else {
                    val upload = storageService.uploadDocument(
                        tenantId = tenantId,
                        prefix = "peppol",
                        filename = "peppol-$sourceId.xml",
                        data = xmlBytes,
                        contentType = "application/xml"
                    )
                    blobRepository.createIfAbsent(
                        tenantId = tenantId,
                        payload = DocumentBlobCreatePayload(
                            inputHash = inputHash,
                            storageKey = upload.key,
                            contentType = upload.contentType,
                            sizeBytes = upload.sizeBytes
                        )
                    ).id
                }
            }

        return sourceRepository.updatePeppolEnvelope(
            tenantId = tenantId,
            sourceId = sourceId,
            peppolRawUblBlobId = rawUblBlobId,
            peppolStructuredSnapshotJson = structuredSnapshotJson,
            peppolSnapshotVersion = snapshotVersion
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
                resolution = IntakeResolution.Linked.SameContent(contentMatchDocumentId)
            )
            recordIntakeOutcome(resolvedSource.sourceChannel, result.resolution)
            return result
        }

        if (identity != null) {
            val identityMatchDocumentId = sourceRepository.findLinkedDocumentByIdentityKeyHash(
                tenantId = tenantId,
                identityKeyHash = identity.hash,
                excludeDocumentId = documentId
            )
            if (identityMatchDocumentId != null) {
                val existingDraftSummary = documentRepository.getDraftByDocumentId(identityMatchDocumentId, tenantId)
                val existingDraftData = existingDraftSummary?.let {
                    draftRepository.getDraftAsDocDto(tenantId, identityMatchDocumentId, it.documentType)?.let { docDto -> DocumentDraftData.from(docDto) }
                }
                val hasMaterialConflict = existingDraftData?.let { hasMaterialConflict(draftData, it) } ?: false
                return if (hasMaterialConflict) {
                    val result = createPendingReview(
                        tenantId = tenantId,
                        sourceId = resolvedSource.id,
                        sourceDocumentId = documentId,
                        targetDocumentId = identityMatchDocumentId,
                        reason = ReviewReason.MaterialConflict,
                        summary = "Possible match with conflicting financial facts",
                        matchType = SourceMatchKind.SameDocument
                    )
                    recordIntakeOutcome(resolvedSource.sourceChannel, result.resolution)
                    result
                } else {
                    val result = linkSourceToExisting(
                        tenantId = tenantId,
                        sourceId = resolvedSource.id,
                        sourceDocumentId = documentId,
                        targetDocumentId = identityMatchDocumentId,
                        resolution = IntakeResolution.Linked.IdentityMatch(identityMatchDocumentId)
                    )
                    recordIntakeOutcome(resolvedSource.sourceChannel, result.resolution)
                    result
                }
            }

            if (FuzzyMatchingEnabled) {
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
                    val fuzzyDraftSummary = documentRepository.getDraftByDocumentId(bestFuzzy.documentId, tenantId)
                    val existingDraftData = fuzzyDraftSummary?.let {
                        draftRepository.getDraftAsDocDto(tenantId, bestFuzzy.documentId, it.documentType)?.let { docDto -> DocumentDraftData.from(docDto) }
                    }
                    val fuzzyAssessment = buildFuzzyAssessment(
                        incoming = draftData,
                        existing = existingDraftData
                    )
                    val result = createPendingReview(
                        tenantId = tenantId,
                        sourceId = resolvedSource.id,
                        sourceDocumentId = documentId,
                        targetDocumentId = bestFuzzy.documentId,
                        reason = ReviewReason.FuzzyCandidate,
                        summary = fuzzyAssessment.summary,
                        aiConfidence = fuzzyAssessment.confidence,
                        matchType = SourceMatchKind.FuzzyCandidate
                    )
                    recordIntakeOutcome(resolvedSource.sourceChannel, result.resolution)
                    return result
                }
            }
        }

        if (identity != null) {
            documentRepository.updateCanonicalIdentityKey(tenantId, documentId, identity.hash)
        }

        val sourceCount = sourceRepository.countLinkedSources(tenantId, documentId)
        val result = DocumentIntakeServiceResult(
            documentId = documentId,
            sourceId = resolvedSource.id,
            runId = null,
            resolution = IntakeResolution.NewDocument,
            sourceDocumentId = documentId,
            sourceCount = sourceCount
        )
        recordIntakeOutcome(resolvedSource.sourceChannel, result.resolution)
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
                val orphanDeleted = deleteOrphanIfSafe(tenantId, source.documentId)
                // Map the DB-stored matchType to a Linked resolution variant
                val linkedResolution: IntakeResolution.Linked = when (source.matchType) {
                    SourceMatchKind.ExactFile ->
                        IntakeResolution.Linked.ExactFile(review.documentId)
                    SourceMatchKind.SameContent ->
                        IntakeResolution.Linked.SameContent(review.documentId)
                    SourceMatchKind.SameDocument, SourceMatchKind.FuzzyCandidate, null ->
                        IntakeResolution.Linked.IdentityMatch(review.documentId)
                }
                DocumentIntakeServiceResult(
                    documentId = review.documentId,
                    sourceId = source.id,
                    runId = null,
                    resolution = linkedResolution,
                    sourceDocumentId = source.documentId,
                    sourceCount = sourceCount,
                    orphanedDocumentId = source.documentId.takeIf { orphanDeleted }
                ).also {
                    logger.info(
                        "AUDIT review_resolved: decision=SAME tenant={} user={} review={} " +
                            "sourceId={} originalDocumentId={} mergedIntoDocumentId={} matchType={}",
                        tenantId,
                        userId,
                        reviewId,
                        source.id,
                        source.documentId,
                        review.documentId,
                        source.matchType
                    )
                    Metrics.counter(
                        "dokus_review_resolution_count",
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
                    payload = DocumentCreatePayload(
                        canonicalIdentityKey = source.identityKeyHash,
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
                    runSuspendCatching {
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
                        documentRepository.createOrUpdateFromIngestion(
                            documentId = newDocumentId,
                            tenantId = tenantId,
                            runId = runId,
                            extractedData = draftData,
                            documentType = draftData.toDocumentType(),
                            force = true
                        )
                        draftRepository.saveDraftFromExtraction(
                            tenantId = tenantId,
                            documentId = newDocumentId,
                            extractedData = draftData,
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
                val orphanDeleted = deleteOrphanIfSafe(tenantId, source.documentId)

                DocumentIntakeServiceResult(
                    documentId = newDocumentId,
                    sourceId = source.id,
                    runId = null,
                    resolution = IntakeResolution.NewDocument,
                    sourceDocumentId = source.documentId,
                    sourceCount = 1,
                    orphanedDocumentId = source.documentId.takeIf { orphanDeleted }
                ).also {
                    logger.info(
                        "AUDIT review_resolved: decision=DIFFERENT tenant={} user={} review={} " +
                            "sourceId={} originalDocumentId={} newDocumentId={}",
                        tenantId,
                        userId,
                        reviewId,
                        source.id,
                        source.documentId,
                        newDocumentId
                    )
                    Metrics.counter(
                        "dokus_review_resolution_count",
                        "decision",
                        DocumentMatchResolutionDecision.DIFFERENT.name
                    ).increment()
                }
            }
        }
    }

    suspend fun documentExists(tenantId: TenantId, documentId: DocumentId): Boolean =
        documentRepository.exists(tenantId, documentId)

    suspend fun isDocumentConfirmed(tenantId: TenantId, documentId: DocumentId): Boolean =
        documentRepository.isConfirmed(tenantId, documentId)

    suspend fun getDocument(tenantId: TenantId, documentId: DocumentId): DocumentDto? =
        documentRepository.getById(tenantId, documentId)

    data class PreviewSourceSelection(
        val storageKey: String,
        val contentType: String,
        val cacheScope: String,
    )

    suspend fun resolvePreviewSource(
        tenantId: TenantId,
        documentId: DocumentId,
    ): PreviewSourceSelection {
        if (!documentRepository.exists(tenantId, documentId)) {
            throw DokusException.NotFound("Document not found: $documentId")
        }
        val sources = sourceRepository.listByDocument(tenantId, documentId)
        val preferred = selectPreferredSource(sources)
            ?: throw DokusException.NotFound("No source available for document: $documentId")
        return PreviewSourceSelection(
            storageKey = preferred.storageKey,
            contentType = preferred.contentType,
            cacheScope = "source-${preferred.id}"
        )
    }

    suspend fun resolvePreviewSource(
        tenantId: TenantId,
        documentId: DocumentId,
        sourceId: DocumentSourceId,
    ): PreviewSourceSelection {
        if (!documentRepository.exists(tenantId, documentId)) {
            throw DokusException.NotFound("Document not found: $documentId")
        }
        val source = sourceRepository.getById(tenantId, sourceId)
            ?: throw DokusException.NotFound("Source not found: $sourceId")
        if (source.documentId != documentId || source.status != DocumentSourceStatus.Linked) {
            throw DokusException.NotFound("Source not found: $sourceId")
        }
        return PreviewSourceSelection(
            storageKey = source.storageKey,
            contentType = source.contentType,
            cacheScope = "source-${source.id}"
        )
    }

    suspend fun listSources(tenantId: TenantId, documentId: DocumentId): List<DocumentSourceDto> {
        return sourceRepository.listByDocument(tenantId, documentId)
            .map { DocumentSourceDto.from(it) }
    }

    suspend fun getPendingReviewByDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ): DocumentMatchReviewEntity? {
        return matchReviewRepository.listPendingByDocumentIds(tenantId, listOf(documentId))[documentId]
    }

    suspend fun getPendingReviewsByDocuments(
        tenantId: TenantId,
        documentIds: List<DocumentId>
    ): Map<DocumentId, DocumentMatchReviewEntity> {
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
        val draft = documentRepository.getDraftByDocumentId(documentId, tenantId)
        val isConfirmed = draft?.documentStatus == DocumentStatus.Confirmed

        if (linkedCount <= 1 && isConfirmed && !confirmLastOnConfirmed) {
            return SourceDeleteResult(
                deleted = false,
                requiresConfirmation = true
            )
        }

        val deleted = sourceRepository.deleteById(tenantId, sourceId)
        if (!deleted) return SourceDeleteResult(deleted = false)
        Metrics.counter("dokus_source_detach_count").increment()
        if (linkedCount <= 1 && isConfirmed && confirmLastOnConfirmed) {
            Metrics.counter("dokus_last_source_delete_confirmed_count").increment()
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
    ): DocumentSourceEntity? {
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
        resolution: IntakeResolution.Linked
    ): DocumentIntakeServiceResult {
        sourceRepository.reassignToDocument(
            tenantId = tenantId,
            sourceId = sourceId,
            documentId = targetDocumentId,
            status = DocumentSourceStatus.Linked,
            matchType = when (resolution) {
                is IntakeResolution.Linked.ExactFile -> SourceMatchKind.ExactFile
                is IntakeResolution.Linked.SameContent -> SourceMatchKind.SameContent
                is IntakeResolution.Linked.IdentityMatch -> SourceMatchKind.SameDocument
            }
        )
        val orphanDeleted = deleteOrphanIfSafe(tenantId, sourceDocumentId)
        val sourceCount = sourceRepository.countLinkedSources(tenantId, targetDocumentId)
        return DocumentIntakeServiceResult(
            documentId = targetDocumentId,
            sourceId = sourceId,
            runId = null,
            resolution = resolution,
            sourceDocumentId = sourceDocumentId,
            sourceCount = sourceCount,
            orphanedDocumentId = sourceDocumentId.takeIf { orphanDeleted }
        )
    }

    private suspend fun createPendingReview(
        tenantId: TenantId,
        sourceId: DocumentSourceId,
        sourceDocumentId: DocumentId,
        targetDocumentId: DocumentId,
        reason: ReviewReason,
        summary: String,
        aiConfidence: Double? = null,
        matchType: SourceMatchKind
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
        val orphanDeleted = deleteOrphanIfSafe(tenantId, sourceDocumentId)
        val sourceCount = sourceRepository.countLinkedSources(tenantId, targetDocumentId)
        val resolution = when (reason) {
            ReviewReason.MaterialConflict ->
                IntakeResolution.NeedsReview.MaterialConflict(targetDocumentId, reviewId)
            ReviewReason.FuzzyCandidate ->
                IntakeResolution.NeedsReview.FuzzyCandidate(targetDocumentId, reviewId)
        }
        return DocumentIntakeServiceResult(
            documentId = targetDocumentId,
            sourceId = sourceId,
            runId = null,
            resolution = resolution,
            sourceDocumentId = sourceDocumentId,
            sourceCount = sourceCount,
            orphanedDocumentId = sourceDocumentId.takeIf { orphanDeleted }
        )
    }

    /**
     * Deletes a document if it has no remaining sources.
     * Called after source reassignment and source deletion paths to avoid
     * leaving source-less documents visible in listing queries.
     */
    private suspend fun deleteOrphanIfSafe(tenantId: TenantId, documentId: DocumentId): Boolean {
        val remainingSources = sourceRepository.countSources(
            tenantId = tenantId,
            documentId = documentId,
            includeDetached = true
        )
        if (remainingSources == 0) {
            logger.info("Removing orphaned document after user action: {}", documentId)
            documentRepository.delete(tenantId, documentId)
            return true
        }
        return false
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
                if (data.direction == DocumentDirection.Unknown || data.direction == DocumentDirection.Neutral) return null
                val supplierVat = when (data.direction) {
                    DocumentDirection.Inbound -> data.seller.vat
                    DocumentDirection.Outbound -> data.buyer.vat
                    DocumentDirection.Neutral -> null
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
                if (data.direction == DocumentDirection.Unknown || data.direction == DocumentDirection.Neutral) return null
                val supplierVat = when (data.direction) {
                    DocumentDirection.Inbound -> data.seller.vat
                    DocumentDirection.Outbound -> data.buyer.vat
                    DocumentDirection.Neutral -> null
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

            is ReceiptDraftData -> null
            is BankStatementDraftData -> null
            is ProFormaDraftData -> null
            is QuoteDraftData -> null
            is OrderConfirmationDraftData -> null
            is DeliveryNoteDraftData -> null
            is ReminderDraftData -> null
            is StatementOfAccountDraftData -> null
            is PurchaseOrderDraftData -> null
            is ExpenseClaimDraftData -> null
            is BankFeeDraftData -> null
            is InterestStatementDraftData -> null
            is PaymentConfirmationDraftData -> null
            is VatReturnDraftData -> null
            is VatListingDraftData -> null
            is VatAssessmentDraftData -> null
            is IcListingDraftData -> null
            is OssReturnDraftData -> null
            is CorporateTaxDraftData -> null
            is CorporateTaxAdvanceDraftData -> null
            is TaxAssessmentDraftData -> null
            is PersonalTaxDraftData -> null
            is WithholdingTaxDraftData -> null
            is SocialContributionDraftData -> null
            is SocialFundDraftData -> null
            is SelfEmployedContributionDraftData -> null
            is VapzDraftData -> null
            is SalarySlipDraftData -> null
            is PayrollSummaryDraftData -> null
            is EmploymentContractDraftData -> null
            is DimonaDraftData -> null
            is C4DraftData -> null
            is HolidayPayDraftData -> null
            is ContractDraftData -> null
            is LeaseDraftData -> null
            is LoanDraftData -> null
            is InsuranceDraftData -> null
            is DividendDraftData -> null
            is ShareholderRegisterDraftData -> null
            is CompanyExtractDraftData -> null
            is AnnualAccountsDraftData -> null
            is BoardMinutesDraftData -> null
            is SubsidyDraftData -> null
            is FineDraftData -> null
            is PermitDraftData -> null
            is CustomsDeclarationDraftData -> null
            is IntrastatDraftData -> null
            is DepreciationScheduleDraftData -> null
            is InventoryDraftData -> null
            is OtherDraftData -> null
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

    private fun vatSummary(entries: List<VatBreakdownEntryDto>): List<String> {
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
        resolution: IntakeResolution
    ) {
        val metricName = when (resolution) {
            is IntakeResolution.NewDocument -> "NewDocument"
            is IntakeResolution.Linked -> "LinkedToExisting"
            is IntakeResolution.NeedsReview -> "PendingMatchReview"
        }
        Metrics.counter(
            "dokus_intake_outcome_count",
            "outcome",
            metricName,
            "channel",
            sourceChannel.name
        ).increment()
        when (resolution) {
            is IntakeResolution.Linked -> {
                Metrics.counter("dokus_silent_link_count", "channel", sourceChannel.name).increment()
            }

            is IntakeResolution.NeedsReview -> {
                Metrics.counter("dokus_needs_review_count", "channel", sourceChannel.name).increment()
            }

            is IntakeResolution.NewDocument -> Unit
        }
    }
}
