package tech.dokus.features.ai.judgment

import tech.dokus.features.ai.ensemble.ConflictReport
import tech.dokus.features.ai.ensemble.ConflictSeverity
import tech.dokus.features.ai.ensemble.FieldConflict
import tech.dokus.features.ai.retry.RetryResult
import tech.dokus.features.ai.validation.AuditCheck
import tech.dokus.features.ai.validation.AuditReport
import tech.dokus.features.ai.validation.CheckType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JudgmentCriteriaTest {

    private val criteria = JudgmentCriteria()

    // =========================================================================
    // AUTO_APPROVE Tests
    // =========================================================================

    @Test
    fun `auto-approves clean extraction with high confidence`() {
        val context = createContext(
            confidence = 0.9,
            auditStatus = AuditReport.fromChecks(
                listOf(AuditCheck.passed(CheckType.MATH, "total", "Math verified"))
            )
        )

        val decision = criteria.evaluate(context)

        assertEquals(JudgmentOutcome.AUTO_APPROVE, decision.outcome)
        assertTrue(decision.confidence >= 0.85)
        assertTrue(decision.allCriticalChecksPassed)
    }

    @Test
    fun `auto-approves with warnings when configured`() {
        val context = createContext(
            confidence = 0.85,
            auditStatus = AuditReport.fromChecks(
                listOf(
                    AuditCheck.passed(CheckType.MATH, "total", "Math verified"),
                    AuditCheck.warning(CheckType.VAT_RATE, "vatRate", "Unusual rate")
                )
            )
        )

        val lenientCriteria = JudgmentCriteria(JudgmentConfig(autoApproveWithWarnings = true))
        val decision = lenientCriteria.evaluate(context)

        assertEquals(JudgmentOutcome.AUTO_APPROVE, decision.outcome)
    }

    @Test
    fun `auto-approves after successful retry`() {
        val context = createContext(
            confidence = 0.85,
            auditStatus = AuditReport.fromChecks(
                listOf(AuditCheck.passed(CheckType.MATH, "total", "Math verified"))
            ),
            retryResult = RetryResult.CorrectedOnRetry(
                data = "corrected",
                attempt = 1,
                correctedFields = listOf("totalAmount"),
                originalFailures = listOf(
                    AuditCheck.criticalFailure(CheckType.MATH, "total", "Math error", "")
                )
            )
        )

        val decision = criteria.evaluate(context)

        assertEquals(JudgmentOutcome.AUTO_APPROVE, decision.outcome)
        assertEquals(1, decision.retryAttempts)
        assertEquals(listOf("totalAmount"), decision.correctedFields)
    }

    // =========================================================================
    // NEEDS_REVIEW Tests
    // =========================================================================

    @Test
    fun `needs review when confidence below threshold`() {
        val context = createContext(
            confidence = 0.65,
            auditStatus = AuditReport.fromChecks(
                listOf(AuditCheck.passed(CheckType.MATH, "total", "Math verified"))
            )
        )

        val decision = criteria.evaluate(context)

        assertEquals(JudgmentOutcome.NEEDS_REVIEW, decision.outcome)
        assertTrue(decision.issuesForUser.any { it.contains("confidence") })
    }

    @Test
    fun `needs review with critical conflicts when consensus required`() {
        val context = createContext(
            confidence = 0.85,
            consensusReport = ConflictReport(
                listOf(
                    FieldConflict(
                        field = "totalAmount",
                        fastValue = "100.00",
                        expertValue = "1000.00",
                        chosenValue = "1000.00",
                        chosenSource = "expert",
                        severity = ConflictSeverity.CRITICAL
                    )
                )
            )
        )

        val strictCriteria = JudgmentCriteria(JudgmentConfig(requireConsensusForAutoApprove = true))
        val decision = strictCriteria.evaluate(context)

        assertEquals(JudgmentOutcome.NEEDS_REVIEW, decision.outcome)
        assertFalse(decision.hasModelConsensus)
    }

    @Test
    fun `needs review with too many warnings`() {
        val warnings = (1..5).map {
            AuditCheck.warning(CheckType.VAT_RATE, "field$it", "Warning $it")
        }

        val context = createContext(
            confidence = 0.85,
            auditStatus = AuditReport.fromChecks(warnings)
        )

        val strictCriteria = JudgmentCriteria(
            JudgmentConfig(autoApproveWithWarnings = false, maxWarningsForAutoApprove = 2)
        )
        val decision = strictCriteria.evaluate(context)

        assertEquals(JudgmentOutcome.NEEDS_REVIEW, decision.outcome)
    }

    // =========================================================================
    // REJECT Tests
    // =========================================================================

    @Test
    fun `rejects when essential fields missing`() {
        val context = createContext(
            confidence = 0.9,
            hasEssentialFields = false,
            missingEssentialFields = listOf("totalAmount", "vendorName")
        )

        val decision = criteria.evaluate(context)

        assertEquals(JudgmentOutcome.REJECT, decision.outcome)
        assertTrue(decision.reasoning.contains("Essential fields missing"))
        assertTrue(decision.issuesForUser.any { it.contains("totalAmount") })
    }

    @Test
    fun `rejects unknown document type`() {
        val context = createContext(
            confidence = 0.9,
            documentType = "UNKNOWN"
        )

        val decision = criteria.evaluate(context)

        assertEquals(JudgmentOutcome.REJECT, decision.outcome)
        assertTrue(decision.reasoning.contains("document type"))
    }

    @Test
    fun `rejects with critical failures after failed retry`() {
        val context = createContext(
            confidence = 0.7,
            auditStatus = AuditReport.fromChecks(
                listOf(
                    AuditCheck.criticalFailure(CheckType.MATH, "total", "Math error", "Check total")
                )
            ),
            retryResult = RetryResult.StillFailing(
                data = "failed",
                attempts = 2,
                remainingFailures = listOf(
                    AuditCheck.criticalFailure(CheckType.MATH, "total", "Math error", "Check total")
                )
            )
        )

        val decision = criteria.evaluate(context)

        assertEquals(JudgmentOutcome.REJECT, decision.outcome)
        assertEquals(2, decision.retryAttempts)
        assertTrue(decision.reasoning.contains("retry"))
    }

    @Test
    fun `rejects with very low confidence`() {
        val context = createContext(
            confidence = 0.3
        )

        val decision = criteria.evaluate(context)

        assertEquals(JudgmentOutcome.REJECT, decision.outcome)
        assertTrue(decision.reasoning.contains("confidence"))
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Test
    fun `handles null consensus report`() {
        val context = createContext(
            confidence = 0.9,
            consensusReport = null
        )

        val decision = criteria.evaluate(context)

        assertEquals(JudgmentOutcome.AUTO_APPROVE, decision.outcome)
        assertTrue(decision.hasModelConsensus) // No conflicts when single source
    }

    @Test
    fun `handles null retry result`() {
        val context = createContext(
            confidence = 0.9,
            retryResult = null
        )

        val decision = criteria.evaluate(context)

        assertEquals(JudgmentOutcome.AUTO_APPROVE, decision.outcome)
        assertEquals(0, decision.retryAttempts)
    }

    @Test
    fun `canPotentiallyAutoApprove returns false for failed audit`() {
        val context = createContext(
            confidence = 0.9,
            auditStatus = AuditReport.fromChecks(
                listOf(
                    AuditCheck.criticalFailure(CheckType.MATH, "total", "Error", "")
                )
            )
        )

        assertFalse(criteria.canPotentiallyAutoApprove(context))
    }

    @Test
    fun `canPotentiallyAutoApprove returns true for clean context`() {
        val context = createContext(
            confidence = 0.9,
            auditStatus = AuditReport.fromChecks(
                listOf(AuditCheck.passed(CheckType.MATH, "total", "OK"))
            )
        )

        assertTrue(criteria.canPotentiallyAutoApprove(context))
    }

    // =========================================================================
    // Config Tests
    // =========================================================================

    @Test
    fun `strict config requires higher confidence`() {
        val context = createContext(confidence = 0.82)

        val strictCriteria = JudgmentCriteria(JudgmentConfig.STRICT)
        val decision = strictCriteria.evaluate(context)

        // 0.82 is below strict threshold of 0.9
        assertEquals(JudgmentOutcome.NEEDS_REVIEW, decision.outcome)
    }

    @Test
    fun `lenient config approves with lower confidence`() {
        val context = createContext(confidence = 0.72)

        val lenientCriteria = JudgmentCriteria(JudgmentConfig.LENIENT)
        val decision = lenientCriteria.evaluate(context)

        // 0.72 is above lenient threshold of 0.7
        assertEquals(JudgmentOutcome.AUTO_APPROVE, decision.outcome)
    }

    // =========================================================================
    // Helper Functions
    // =========================================================================

    private fun createContext(
        confidence: Double = 0.9,
        consensusReport: ConflictReport? = null,
        auditStatus: AuditReport = AuditReport.EMPTY,
        retryResult: RetryResult<*>? = null,
        documentType: String = "INVOICE",
        hasEssentialFields: Boolean = true,
        missingEssentialFields: List<String> = emptyList()
    ) = JudgmentContext(
        extractionConfidence = confidence,
        consensusReport = consensusReport,
        auditReport = auditStatus,
        retryResult = retryResult,
        documentType = documentType,
        hasEssentialFields = hasEssentialFields,
        missingEssentialFields = missingEssentialFields
    )
}
