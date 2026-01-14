package tech.dokus.features.ai.judgment

import kotlinx.coroutines.test.runTest
import tech.dokus.features.ai.ensemble.ConflictReport
import tech.dokus.features.ai.retry.RetryResult
import tech.dokus.features.ai.validation.AuditCheck
import tech.dokus.features.ai.validation.AuditReport
import tech.dokus.features.ai.validation.CheckType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JudgmentAgentTest {

    private val agent = JudgmentAgent.deterministic()

    // =========================================================================
    // Deterministic Mode Tests
    // =========================================================================

    @Test
    fun `deterministic agent auto-approves clean extraction`() = runTest {
        val decision = agent.judge(
            extractionConfidence = 0.9,
            consensusReport = null,
            auditReport = AuditReport.EMPTY,
            retryResult = null,
            documentType = "INVOICE",
            hasEssentialFields = true
        )

        assertEquals(JudgmentOutcome.AUTO_APPROVE, decision.outcome)
    }

    @Test
    fun `deterministic agent rejects missing fields`() = runTest {
        val decision = agent.judge(
            extractionConfidence = 0.9,
            consensusReport = null,
            auditReport = AuditReport.EMPTY,
            retryResult = null,
            documentType = "INVOICE",
            hasEssentialFields = false,
            missingEssentialFields = listOf("totalAmount")
        )

        assertEquals(JudgmentOutcome.REJECT, decision.outcome)
    }

    @Test
    fun `deterministic agent needs review for low confidence`() = runTest {
        val decision = agent.judge(
            extractionConfidence = 0.6,
            consensusReport = null,
            auditReport = AuditReport.EMPTY,
            retryResult = null,
            documentType = "BILL",
            hasEssentialFields = true
        )

        assertEquals(JudgmentOutcome.NEEDS_REVIEW, decision.outcome)
    }

    // =========================================================================
    // Context-Based Tests
    // =========================================================================

    @Test
    fun `processes full context correctly`() = runTest {
        val context = JudgmentContext(
            extractionConfidence = 0.85,
            consensusReport = ConflictReport(emptyList()),
            auditReport = AuditReport.fromChecks(
                listOf(AuditCheck.passed(CheckType.MATH, "total", "OK"))
            ),
            retryResult = RetryResult.NoRetryNeeded,
            documentType = "INVOICE",
            hasEssentialFields = true
        )

        val decision = agent.judge(context)

        assertEquals(JudgmentOutcome.AUTO_APPROVE, decision.outcome)
        assertTrue(decision.allCriticalChecksPassed)
        assertTrue(decision.hasModelConsensus)
    }

    @Test
    fun `includes corrected fields in decision`() = runTest {
        val context = JudgmentContext(
            extractionConfidence = 0.85,
            consensusReport = null,
            auditReport = AuditReport.EMPTY,
            retryResult = RetryResult.CorrectedOnRetry(
                data = "data",
                attempt = 1,
                correctedFields = listOf("iban", "totalAmount"),
                originalFailures = emptyList()
            ),
            documentType = "BILL",
            hasEssentialFields = true
        )

        val decision = agent.judge(context)

        assertEquals(JudgmentOutcome.AUTO_APPROVE, decision.outcome)
        assertEquals(listOf("iban", "totalAmount"), decision.correctedFields)
        assertEquals(1, decision.retryAttempts)
    }

    // =========================================================================
    // Factory Method Tests
    // =========================================================================

    @Test
    fun `deterministic factory creates agent without LLM`() = runTest {
        val deterministicAgent = JudgmentAgent.deterministic()

        val decision = deterministicAgent.judge(
            extractionConfidence = 0.9,
            consensusReport = null,
            auditReport = AuditReport.EMPTY,
            retryResult = null,
            documentType = "RECEIPT",
            hasEssentialFields = true
        )

        // Should work without LLM
        assertEquals(JudgmentOutcome.AUTO_APPROVE, decision.outcome)
    }

    @Test
    fun `deterministic agent with strict config`() = runTest {
        val strictAgent = JudgmentAgent.deterministic(JudgmentConfig.STRICT)

        // 85% confidence is below strict 90% threshold
        val decision = strictAgent.judge(
            extractionConfidence = 0.85,
            consensusReport = null,
            auditReport = AuditReport.EMPTY,
            retryResult = null,
            documentType = "INVOICE",
            hasEssentialFields = true
        )

        assertEquals(JudgmentOutcome.NEEDS_REVIEW, decision.outcome)
    }

    @Test
    fun `deterministic agent with lenient config`() = runTest {
        val lenientAgent = JudgmentAgent.deterministic(JudgmentConfig.LENIENT)

        // 72% confidence is above lenient 70% threshold
        val decision = lenientAgent.judge(
            extractionConfidence = 0.72,
            consensusReport = null,
            auditReport = AuditReport.EMPTY,
            retryResult = null,
            documentType = "INVOICE",
            hasEssentialFields = true
        )

        assertEquals(JudgmentOutcome.AUTO_APPROVE, decision.outcome)
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Test
    fun `handles all document types`() = runTest {
        val documentTypes = listOf("INVOICE", "BILL", "RECEIPT", "CREDIT_NOTE", "PRO_FORMA", "EXPENSE")

        for (docType in documentTypes) {
            val decision = agent.judge(
                extractionConfidence = 0.9,
                consensusReport = null,
                auditReport = AuditReport.EMPTY,
                retryResult = null,
                documentType = docType,
                hasEssentialFields = true
            )

            assertEquals(
                JudgmentOutcome.AUTO_APPROVE,
                decision.outcome,
                "Should auto-approve clean $docType"
            )
        }
    }

    @Test
    fun `rejects UNKNOWN document type`() = runTest {
        val decision = agent.judge(
            extractionConfidence = 0.95,
            consensusReport = null,
            auditReport = AuditReport.EMPTY,
            retryResult = null,
            documentType = "UNKNOWN",
            hasEssentialFields = true
        )

        assertEquals(JudgmentOutcome.REJECT, decision.outcome)
    }

    @Test
    fun `handles failed retry gracefully`() = runTest {
        val context = JudgmentContext(
            extractionConfidence = 0.7,
            consensusReport = null,
            auditReport = AuditReport.fromChecks(
                listOf(
                    AuditCheck.criticalFailure(CheckType.CHECKSUM_OGM, "paymentRef", "Invalid", "")
                )
            ),
            retryResult = RetryResult.StillFailing(
                data = "data",
                attempts = 2,
                remainingFailures = listOf(
                    AuditCheck.criticalFailure(CheckType.CHECKSUM_OGM, "paymentRef", "Invalid", "")
                )
            ),
            documentType = "BILL",
            hasEssentialFields = true
        )

        val decision = agent.judge(context)

        assertEquals(JudgmentOutcome.REJECT, decision.outcome)
        assertEquals(2, decision.retryAttempts)
    }

    // =========================================================================
    // Decision Model Tests
    // =========================================================================

    @Test
    fun `JudgmentDecision factory methods work correctly`() {
        val approve = JudgmentDecision.autoApprove(0.9, "All good")
        assertEquals(JudgmentOutcome.AUTO_APPROVE, approve.outcome)
        assertTrue(approve.allCriticalChecksPassed)

        val review = JudgmentDecision.needsReview(0.7, "Issues", listOf("Issue 1"))
        assertEquals(JudgmentOutcome.NEEDS_REVIEW, review.outcome)
        assertEquals(listOf("Issue 1"), review.issuesForUser)

        val reject = JudgmentDecision.reject(0.8, "Failed")
        assertEquals(JudgmentOutcome.REJECT, reject.outcome)
        assertFalse(reject.allCriticalChecksPassed)
    }

    // Helper
    private fun assertFalse(value: Boolean) = assertEquals(false, value)
}
