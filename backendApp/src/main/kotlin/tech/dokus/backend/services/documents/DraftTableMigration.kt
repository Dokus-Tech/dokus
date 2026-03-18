package tech.dokus.backend.services.documents

import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.drafts.DraftRepository
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * One-time migration that populates per-type draft tables from the existing
 * `extracted_data` JSON blob on the documents table.
 *
 * Safe to run multiple times — skips documents that already have a draft table row.
 * Should be called at application startup after schema creation.
 */
class DraftTableMigration(
    private val documentRepository: DocumentRepository,
    private val draftRepository: DraftRepository,
) {
    private val logger = loggerFor<DraftTableMigration>()

    /**
     * Migrate all documents with `extracted_data` that don't yet have a draft table row.
     * Only processes documents in NeedsReview status (confirmed documents use confirmed tables).
     */
    suspend fun migrateIfNeeded() {
        val drafts = documentRepository.listAllDraftsWithExtractedData()
        if (drafts.isEmpty()) {
            logger.info("Draft table migration: no documents to migrate")
            return
        }

        logger.info("Draft table migration: found ${drafts.size} documents with extracted_data")

        var migrated = 0
        var skipped = 0
        var errors = 0

        for (draft in drafts) {
            // Only migrate NeedsReview documents — confirmed ones use confirmed entity tables
            if (draft.documentStatus != DocumentStatus.NeedsReview) {
                skipped++
                continue
            }

            val extractedData = draft.extractedData ?: continue

            // Check if draft table already has this document
            val existing = draftRepository.getDraftAsDocDto(
                draft.tenantId,
                draft.documentId,
                draft.documentType,
            )
            if (existing != null) {
                skipped++
                continue
            }

            try {
                draftRepository.saveDraftFromExtraction(
                    tenantId = draft.tenantId,
                    documentId = draft.documentId,
                    extractedData = extractedData,
                )
                migrated++
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                logger.warn("Failed to migrate draft for document ${draft.documentId}: ${e.message}")
                errors++
            }
        }

        logger.info("Draft table migration complete: migrated=$migrated, skipped=$skipped, errors=$errors")
    }
}
