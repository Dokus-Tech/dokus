package tech.dokus.features.ai.judgment

import tech.dokus.features.ai.ensemble.ConflictSeverity
import tech.dokus.features.ai.retry.RetryResult
import tech.dokus.features.ai.validation.AuditStatus

/**
 * Deterministic judgment criteria for document processing.
 *
 * This class encapsulates the business rules for deciding whether a document
 * can be auto-approved. It provides a fast, deterministic fallback that doesn't
 * require LLM inference.
 *
 * ## Decision Tree
 *
 * ```
 * 1. Essential fields missing? → REJECT
 * 2. Document type unknown? → REJECT
 * 3. Critical audit failures after retry? → REJECT (or NEEDS_REVIEW if recoverable)
 * 4. Critical model conflicts unresolved? → NEEDS_REVIEW
 * 5. Confidence < threshold? → NEEDS_REVIEW
 * 6. Too many warnings? → NEEDS_REVIEW
 * 7. Otherwise → AUTO_APPROVE
 * ```
 */
class JudgmentCriteria(
    private val config: JudgmentConfig = JudgmentConfig.DEFAULT
) {
    /**
     * Evaluate the judgment context and return a decision.
     *
     * This is a deterministic evaluation based on configurable rules.
     * No LLM is required.
     */
    fun evaluate(context: JudgmentContext): JudgmentDecision {
        val issues = mutableListOf<String>()

        // =====================================================================
        // REJECT Conditions (hard failures)
        // =====================================================================

        // 1. Essential fields missing
        if (!context.hasEssentialFields) {
            return JudgmentDecision.reject(
                confidence = 0.9,
                reasoning = "Essential fields missing: ${context.missingEssentialFields.joinToString(", ")}",
                issues = context.missingEssentialFields.map { "Missing required field: $it" },
                retryAttempts = context.retryResult?.let { getRetryAttempts(it) } ?: 0
            )
        }

        // 2. Document type unknown
        if (context.documentType == "UNKNOWN") {
            return JudgmentDecision.reject(
                confidence = 0.95,
                reasoning = "Could not determine document type",
                issues = listOf("Document type could not be classified"),
                retryAttempts = context.retryResult?.let { getRetryAttempts(it) } ?: 0
            )
        }

        // 3. Critical audit failures remain after retry
        val hasCriticalFailures = context.auditReport.criticalFailures.isNotEmpty()
        val retryFailed = context.retryResult is RetryResult.StillFailing

        if (hasCriticalFailures && retryFailed) {
            val failures = context.auditReport.criticalFailures
            return JudgmentDecision.reject(
                confidence = 0.85,
                reasoning = "Critical validation failures remain after ${getRetryAttempts(
                    context.retryResult!!
                )} retry attempts",
                issues = failures.map { "${it.type}: ${it.message}" },
                retryAttempts = getRetryAttempts(context.retryResult)
            )
        }

        // 4. Confidence too low for any processing
        if (context.extractionConfidence < config.needsReviewMinConfidence) {
            return JudgmentDecision.reject(
                confidence = 0.8,
                reasoning = "Extraction confidence too low: ${formatPercent(context.extractionConfidence)}",
                issues = listOf("Low confidence extraction - manual review required"),
                retryAttempts = context.retryResult?.let { getRetryAttempts(it) } ?: 0
            )
        }

        // =====================================================================
        // NEEDS_REVIEW Conditions (soft failures)
        // =====================================================================

        // 5. Critical audit failures (but no retry attempted or retry pending)
        if (hasCriticalFailures) {
            val failures = context.auditReport.criticalFailures
            issues.addAll(failures.map { "${it.type}: ${it.message}" })
        }

        // 6. Critical model conflicts
        val hasCriticalConflicts = context.consensusReport?.conflicts?.any {
            it.severity == ConflictSeverity.CRITICAL
        } ?: false

        if (hasCriticalConflicts && config.requireConsensusForAutoApprove) {
            val conflicts = context.consensusReport!!.conflicts
                .filter { it.severity == ConflictSeverity.CRITICAL }
            issues.addAll(
                conflicts.map { "Model disagreement on ${it.field}: '${it.fastValue}' vs '${it.expertValue}'" }
            )
        }

        // 7. Too many warnings
        val warningCount = context.auditReport.warnings.size
        if (warningCount > config.maxWarningsForAutoApprove && !config.autoApproveWithWarnings) {
            issues.addAll(context.auditReport.warnings.take(3).map { it.message })
            if (warningCount > 3) {
                issues.add("...and ${warningCount - 3} more warnings")
            }
        }

        // 8. Confidence below auto-approve threshold
        if (context.extractionConfidence < config.autoApproveMinConfidence) {
            issues.add(
                "Extraction confidence ${formatPercent(context.extractionConfidence)} below auto-approve threshold"
            )
        }

        // =====================================================================
        // Decision
        // =====================================================================

        // If we have issues, needs review
        if (issues.isNotEmpty()) {
            return JudgmentDecision.needsReview(
                confidence = calculateDecisionConfidence(context),
                reasoning = buildReviewReasoning(context, issues),
                issues = issues,
                allCriticalChecksPassed = !hasCriticalFailures,
                hasModelConsensus = !hasCriticalConflicts,
                retryAttempts = context.retryResult?.let { getRetryAttempts(it) } ?: 0
            )
        }

        // Otherwise, auto-approve
        val correctedFields = when (val retry = context.retryResult) {
            is RetryResult.CorrectedOnRetry -> retry.correctedFields
            else -> emptyList()
        }

        return JudgmentDecision.autoApprove(
            confidence = calculateDecisionConfidence(context),
            reasoning = buildApproveReasoning(context),
            correctedFields = correctedFields,
            retryAttempts = context.retryResult?.let { getRetryAttempts(it) } ?: 0
        )
    }

    /**
     * Quick check if document can potentially be auto-approved.
     * Use this for early filtering before full evaluation.
     */
    fun canPotentiallyAutoApprove(context: JudgmentContext): Boolean {
        return context.hasEssentialFields &&
            context.documentType != "UNKNOWN" &&
            context.extractionConfidence >= config.needsReviewMinConfidence &&
            context.auditReport.overallStatus != AuditStatus.FAILED
    }

    private fun calculateDecisionConfidence(context: JudgmentContext): Double {
        var confidence = context.extractionConfidence

        // Boost confidence if audit passed
        if (context.auditReport.overallStatus == AuditStatus.PASSED) {
            confidence = minOf(1.0, confidence + 0.05)
        }

        // Boost confidence if models agreed
        if (context.consensusReport == null || !context.consensusReport.hasConflicts) {
            confidence = minOf(1.0, confidence + 0.05)
        }

        // Reduce confidence for warnings
        val warningPenalty = context.auditReport.warnings.size * 0.02
        confidence = maxOf(0.0, confidence - warningPenalty)

        return confidence
    }

    private fun buildApproveReasoning(context: JudgmentContext): String = buildString {
        append("Document auto-approved: ")

        val reasons = mutableListOf<String>()

        if (context.auditReport.overallStatus == AuditStatus.PASSED) {
            reasons.add("all validations passed")
        }

        if (context.consensusReport == null || !context.consensusReport.hasConflicts) {
            reasons.add("model consensus achieved")
        }

        reasons.add("confidence ${formatPercent(context.extractionConfidence)}")

        when (val retry = context.retryResult) {
            is RetryResult.CorrectedOnRetry -> {
                reasons.add("self-corrected on attempt ${retry.attempt}")
            }
            is RetryResult.NoRetryNeeded -> {
                reasons.add("passed on first attempt")
            }
            else -> {}
        }

        append(reasons.joinToString(", "))
    }

    private fun buildReviewReasoning(context: JudgmentContext, issues: List<String>): String = buildString {
        append("Document needs review: ")
        append("${issues.size} issue(s) detected. ")
        append("Confidence: ${formatPercent(context.extractionConfidence)}.")
    }

    private fun getRetryAttempts(result: RetryResult<*>): Int = when (result) {
        is RetryResult.NoRetryNeeded -> 0
        is RetryResult.CorrectedOnRetry -> result.attempt
        is RetryResult.StillFailing -> result.attempts
    }

    private fun formatPercent(value: Double): String = "${(value * 100).toInt()}%"
}
