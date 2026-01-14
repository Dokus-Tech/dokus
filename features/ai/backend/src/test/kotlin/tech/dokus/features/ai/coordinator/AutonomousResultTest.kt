package tech.dokus.features.ai.coordinator

import tech.dokus.features.ai.ensemble.ConflictReport
import tech.dokus.features.ai.ensemble.ConflictSeverity
import tech.dokus.features.ai.ensemble.FieldConflict
import tech.dokus.features.ai.judgment.JudgmentDecision
import tech.dokus.features.ai.judgment.JudgmentOutcome
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.DocumentClassification
import tech.dokus.features.ai.retry.RetryResult
import tech.dokus.features.ai.validation.AuditReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutonomousResultTest {

    // =========================================================================
    // Success Result Tests
    // =========================================================================

    @Test
    fun `Success isAutoApproved returns true for AUTO_APPROVE judgment`() {
        val result = createSuccessResult(JudgmentOutcome.AUTO_APPROVE)

        assertTrue(result.isAutoApproved)
        assertFalse(result.needsReview)
        assertFalse(result.isRejected)
    }

    @Test
    fun `Success needsReview returns true for NEEDS_REVIEW judgment`() {
        val result = createSuccessResult(JudgmentOutcome.NEEDS_REVIEW)

        assertFalse(result.isAutoApproved)
        assertTrue(result.needsReview)
        assertFalse(result.isRejected)
    }

    @Test
    fun `Success isRejected returns true for REJECT judgment`() {
        val result = createSuccessResult(JudgmentOutcome.REJECT)

        assertFalse(result.isAutoApproved)
        assertFalse(result.needsReview)
        assertTrue(result.isRejected)
    }

    @Test
    fun `Success documentType returns classification type`() {
        val result = createSuccessResult(
            outcome = JudgmentOutcome.AUTO_APPROVE,
            docType = ClassifiedDocumentType.BILL
        )

        assertEquals(ClassifiedDocumentType.BILL, result.documentType)
    }

    @Test
    fun `Success wasCorrected returns true when CorrectedOnRetry`() {
        val result = createSuccessResult(
            outcome = JudgmentOutcome.AUTO_APPROVE,
            retryResult = RetryResult.CorrectedOnRetry(
                data = "data",
                attempt = 1,
                correctedFields = listOf("totalAmount"),
                originalFailures = emptyList()
            )
        )

        assertTrue(result.wasCorrected)
        assertEquals(listOf("totalAmount"), result.correctedFields)
        assertEquals(1, result.retryAttempts)
    }

    @Test
    fun `Success correctedFields returns empty list when not corrected`() {
        val result = createSuccessResult(
            outcome = JudgmentOutcome.AUTO_APPROVE,
            retryResult = RetryResult.NoRetryNeeded
        )

        assertFalse(result.wasCorrected)
        assertTrue(result.correctedFields.isEmpty())
        assertEquals(0, result.retryAttempts)
    }

    @Test
    fun `Success retryAttempts returns attempts from StillFailing`() {
        val result = createSuccessResult(
            outcome = JudgmentOutcome.NEEDS_REVIEW,
            retryResult = RetryResult.StillFailing(
                data = "data",
                attempts = 2,
                remainingFailures = emptyList()
            )
        )

        assertEquals(2, result.retryAttempts)
    }

    @Test
    fun `Success hadConflicts returns true when conflict report has conflicts`() {
        val result = createSuccessResult(
            outcome = JudgmentOutcome.AUTO_APPROVE,
            conflictReport = ConflictReport(
                listOf(
                    FieldConflict(
                        field = "vendorName",
                        fastValue = "A",
                        expertValue = "B",
                        chosenValue = "B",
                        chosenSource = "expert",
                        severity = ConflictSeverity.WARNING
                    )
                )
            )
        )

        assertTrue(result.hadConflicts)
    }

    @Test
    fun `Success hadConflicts returns false when no conflict report`() {
        val result = createSuccessResult(
            outcome = JudgmentOutcome.AUTO_APPROVE,
            conflictReport = null
        )

        assertFalse(result.hadConflicts)
    }

    @Test
    fun `Success confidence returns judgment confidence`() {
        val result = createSuccessResult(
            outcome = JudgmentOutcome.AUTO_APPROVE,
            confidence = 0.92
        )

        assertEquals(0.92, result.confidence)
    }

    // =========================================================================
    // Rejected Result Tests
    // =========================================================================

    @Test
    fun `Rejected unknownDocumentType creates correct rejection`() {
        val classification = DocumentClassification(
            documentType = ClassifiedDocumentType.UNKNOWN,
            confidence = 0.5,
            reasoning = "Could not determine"
        )

        val result = AutonomousResult.Rejected.unknownDocumentType(classification)

        assertEquals(RejectionStage.CLASSIFICATION, result.stage)
        assertTrue(result.reason.contains("document type"))
        assertEquals(ClassifiedDocumentType.UNKNOWN, result.documentType)
    }

    @Test
    fun `Rejected lowConfidence creates correct rejection`() {
        val classification = DocumentClassification(
            documentType = ClassifiedDocumentType.INVOICE,
            confidence = 0.25,
            reasoning = "Low confidence"
        )

        val result = AutonomousResult.Rejected.lowConfidence(classification, 0.3)

        assertEquals(RejectionStage.CLASSIFICATION, result.stage)
        assertTrue(result.reason.contains("25%"))
        assertTrue(result.reason.contains("30%"))
        assertEquals("0.25", result.details["confidence"])
        assertEquals("0.3", result.details["threshold"])
    }

    @Test
    fun `Rejected extractionFailed includes error details`() {
        val classification = DocumentClassification(
            documentType = ClassifiedDocumentType.INVOICE,
            confidence = 0.9,
            reasoning = "Invoice detected"
        )
        val fastError = RuntimeException("Fast model OOM")
        val expertError = RuntimeException("Expert model timeout")

        val result = AutonomousResult.Rejected.extractionFailed(classification, fastError, expertError)

        assertEquals(RejectionStage.EXTRACTION, result.stage)
        assertTrue(result.reason.contains("Both models failed"))
        assertEquals("Fast model OOM", result.details["fastError"])
        assertEquals("Expert model timeout", result.details["expertError"])
    }

    @Test
    fun `Rejected noDataExtracted creates correct rejection`() {
        val classification = DocumentClassification(
            documentType = ClassifiedDocumentType.BILL,
            confidence = 0.8,
            reasoning = "Bill detected"
        )

        val result = AutonomousResult.Rejected.noDataExtracted(classification)

        assertEquals(RejectionStage.EXTRACTION, result.stage)
        assertTrue(result.reason.contains("No data"))
        assertEquals(ClassifiedDocumentType.BILL, result.documentType)
    }

    @Test
    fun `Rejected documentType returns null when classification is null`() {
        val result = AutonomousResult.Rejected(
            reason = "Unknown error",
            classification = null,
            stage = RejectionStage.EXTRACTION
        )

        assertNull(result.documentType)
    }

    // =========================================================================
    // Statistics Tests
    // =========================================================================

    @Test
    fun `AutonomousProcessingStats calculates autoApproveRate correctly`() {
        val results = listOf(
            createSuccessResult(JudgmentOutcome.AUTO_APPROVE),
            createSuccessResult(JudgmentOutcome.AUTO_APPROVE),
            createSuccessResult(JudgmentOutcome.AUTO_APPROVE),
            createSuccessResult(JudgmentOutcome.NEEDS_REVIEW),
            createSuccessResult(JudgmentOutcome.REJECT)
        )

        val stats = AutonomousProcessingStats.from<Any>(results)

        assertEquals(5, stats.totalProcessed)
        assertEquals(3, stats.autoApproved)
        assertEquals(1, stats.needsReview)
        assertEquals(1, stats.rejected)
        assertEquals(0.6, stats.autoApproveRate)
    }

    @Test
    fun `AutonomousProcessingStats includes early rejections separately`() {
        val results = listOf(
            createSuccessResult(JudgmentOutcome.AUTO_APPROVE),
            createSuccessResult(JudgmentOutcome.AUTO_APPROVE),
            AutonomousResult.Rejected.unknownDocumentType(
                DocumentClassification(ClassifiedDocumentType.UNKNOWN, 0.5, "Unknown")
            ),
            AutonomousResult.Rejected.noDataExtracted(
                DocumentClassification(ClassifiedDocumentType.INVOICE, 0.9, "Invoice")
            )
        )

        val stats = AutonomousProcessingStats.from<Any>(results)

        assertEquals(4, stats.totalProcessed)
        assertEquals(2, stats.autoApproved)
        assertEquals(2, stats.earlyRejected)
        assertEquals(0.5, stats.autoApproveRate)
        assertEquals(0.5, stats.rejectionRate)
    }

    @Test
    fun `AutonomousProcessingStats meetsSilenceGoal returns true for 95 percent`() {
        val results = (1..95).map { createSuccessResult(JudgmentOutcome.AUTO_APPROVE) } +
            (1..5).map { createSuccessResult(JudgmentOutcome.NEEDS_REVIEW) }

        val stats = AutonomousProcessingStats.from<Any>(results)

        assertEquals(0.95, stats.autoApproveRate)
        assertTrue(stats.meetsSilenceGoal)
    }

    @Test
    fun `AutonomousProcessingStats meetsSilenceGoal returns false for 94 percent`() {
        val results = (1..94).map { createSuccessResult(JudgmentOutcome.AUTO_APPROVE) } +
            (1..6).map { createSuccessResult(JudgmentOutcome.NEEDS_REVIEW) }

        val stats = AutonomousProcessingStats.from<Any>(results)

        assertEquals(0.94, stats.autoApproveRate)
        assertFalse(stats.meetsSilenceGoal)
    }

    @Test
    fun `AutonomousProcessingStats calculates average confidence correctly`() {
        val results = listOf(
            createSuccessResult(JudgmentOutcome.AUTO_APPROVE, confidence = 0.9),
            createSuccessResult(JudgmentOutcome.AUTO_APPROVE, confidence = 0.8),
            createSuccessResult(JudgmentOutcome.NEEDS_REVIEW, confidence = 0.7)
        )

        val stats = AutonomousProcessingStats.from<Any>(results)

        assertEquals(0.8, stats.averageConfidence, 0.001)
    }

    @Test
    fun `AutonomousProcessingStats handles empty results`() {
        val stats = AutonomousProcessingStats.from<Any>(emptyList())

        assertEquals(0, stats.totalProcessed)
        assertEquals(0.0, stats.autoApproveRate)
        assertEquals(0.0, stats.averageConfidence)
        assertFalse(stats.meetsSilenceGoal)
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private fun createSuccessResult(
        outcome: JudgmentOutcome,
        docType: ClassifiedDocumentType = ClassifiedDocumentType.INVOICE,
        confidence: Double = 0.85,
        retryResult: RetryResult<String>? = null,
        conflictReport: ConflictReport? = null
    ): AutonomousResult.Success<String> {
        return AutonomousResult.Success(
            classification = DocumentClassification(
                documentType = docType,
                confidence = 0.9,
                reasoning = "Test classification"
            ),
            extraction = "test-extraction",
            conflictReport = conflictReport,
            auditReport = AuditReport.EMPTY,
            retryResult = retryResult,
            judgment = JudgmentDecision(
                outcome = outcome,
                confidence = confidence,
                reasoning = "Test decision",
                issuesForUser = emptyList(),
                allCriticalChecksPassed = outcome == JudgmentOutcome.AUTO_APPROVE,
                hasModelConsensus = true,
                retryAttempts = 0,
                correctedFields = emptyList()
            )
        )
    }
}
