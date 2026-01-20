package tech.dokus.features.ai.judgment

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import tech.dokus.features.ai.ensemble.ConflictReport
import tech.dokus.features.ai.ensemble.ConflictSeverity
import tech.dokus.features.ai.retry.RetryResult
import tech.dokus.features.ai.validation.AuditReport
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Layer 5: Judgment Agent - The Final Gate
 *
 * This is the key to the "Silence" philosophy.
 * The agent reviews all previous layer outputs and makes a final decision:
 *
 * - **AUTO_APPROVE**: Document processed silently, user never sees it (95%+ target)
 * - **NEEDS_REVIEW**: Show to user with specific issues highlighted
 * - **REJECT**: Cannot be processed automatically, requires manual handling
 *
 * ## Design Philosophy
 *
 * The agent has two modes:
 *
 * 1. **Deterministic Mode** (default): Uses rule-based JudgmentCriteria for fast,
 *    predictable decisions. No LLM required. Suitable for 90%+ of cases.
 *
 * 2. **LLM-Assisted Mode**: For edge cases, can optionally use a text model
 *    (qwen3:30b) to reason about complex situations. Does NOT re-read the
 *    document - only analyzes the reports.
 *
 * ## Performance Characteristics
 *
 * - Deterministic mode: < 1ms
 * - LLM-assisted mode: ~500ms (qwen3:30b on M4 Max)
 */
class JudgmentAgent(
    private val criteria: JudgmentCriteria = JudgmentCriteria(),
    private val executor: PromptExecutor? = null,
    private val model: LLModel? = null,
    private val config: JudgmentConfig = JudgmentConfig.DEFAULT
) {
    private val logger = loggerFor()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Make a judgment decision for the given context.
     *
     * @param context Aggregated information from all previous layers
     * @param useLlm Whether to use LLM for edge cases (default: false)
     * @return The final judgment decision
     */
    suspend fun judge(
        context: JudgmentContext,
        useLlm: Boolean = false
    ): JudgmentDecision {
        logger.info("Making judgment decision for ${context.documentType} document")

        // First, try deterministic evaluation
        val deterministicDecision = criteria.evaluate(context)

        // For clear-cut cases, return immediately
        if (isDecisionClearCut(deterministicDecision, context)) {
            logger.info(
                "Deterministic decision: ${deterministicDecision.outcome} " +
                    "(confidence: ${deterministicDecision.confidence})"
            )
            return deterministicDecision
        }

        // For edge cases, optionally use LLM
        if (useLlm && executor != null && model != null) {
            logger.info("Using LLM for edge case judgment")
            return try {
                judgWithLlm(context, deterministicDecision)
            } catch (e: Exception) {
                logger.warn("LLM judgment failed, falling back to deterministic", e)
                deterministicDecision
            }
        }

        return deterministicDecision
    }

    /**
     * Convenience method for common use case.
     */
    suspend fun judge(
        extractionConfidence: Double,
        consensusReport: ConflictReport?,
        auditReport: AuditReport,
        retryResult: RetryResult<*>?,
        documentType: String,
        hasEssentialFields: Boolean,
        missingEssentialFields: List<String> = emptyList(),
        useLlm: Boolean = false
    ): JudgmentDecision = judge(
        context = JudgmentContext(
            extractionConfidence = extractionConfidence,
            consensusReport = consensusReport,
            auditReport = auditReport,
            retryResult = retryResult,
            documentType = documentType,
            hasEssentialFields = hasEssentialFields,
            missingEssentialFields = missingEssentialFields
        ),
        useLlm = useLlm
    )

    /**
     * Check if the decision is clear-cut (doesn't need LLM reasoning).
     */
    private fun isDecisionClearCut(decision: JudgmentDecision, context: JudgmentContext): Boolean {
        return when (decision.outcome) {
            // REJECT is always clear-cut
            JudgmentOutcome.REJECT -> true

            // AUTO_APPROVE with high confidence is clear-cut
            JudgmentOutcome.AUTO_APPROVE -> decision.confidence >= 0.85

            // NEEDS_REVIEW with clear issues is clear-cut
            JudgmentOutcome.NEEDS_REVIEW -> {
                // Clear if there are explicit issues OR confidence is very low
                decision.issuesForUser.isNotEmpty() || decision.confidence < 0.6
            }
        }
    }

    /**
     * Use LLM to make a nuanced judgment decision.
     */
    private suspend fun judgWithLlm(
        context: JudgmentContext,
        fallbackDecision: JudgmentDecision
    ): JudgmentDecision {
        val prompt = buildJudgmentPrompt(context)

        val systemMessage = Message.System(
            parts = listOf(ContentPart.Text(JUDGMENT_SYSTEM_PROMPT)),
            metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
        )

        val userMessage = Message.User(
            parts = listOf(ContentPart.Text(prompt)),
            metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
        )

        val judgmentPrompt = Prompt(
            messages = listOf(systemMessage, userMessage),
            id = "judgment-agent"
        )

        val response = executor!!.execute(judgmentPrompt, model!!, emptyList()).first()

        return try {
            parseJudgmentResponse(response.content, context)
        } catch (e: Exception) {
            logger.warn("Failed to parse LLM judgment response", e)
            fallbackDecision
        }
    }

    /**
     * Build a prompt summarizing all layer outputs for the LLM.
     */
    private fun buildJudgmentPrompt(context: JudgmentContext): String = buildString {
        appendLine("# Document Analysis Report")
        appendLine()

        // Document info
        appendLine("## Document Summary")
        appendLine("- Document Type: ${context.documentType}")
        appendLine("- Extraction Confidence: ${formatPercent(context.extractionConfidence)}")
        appendLine("- Essential Fields Present: ${if (context.hasEssentialFields) "Yes" else "No"}")
        if (context.missingEssentialFields.isNotEmpty()) {
            appendLine("- Missing Fields: ${context.missingEssentialFields.joinToString(", ")}")
        }
        appendLine()

        // Consensus report (Layer 2)
        appendLine("## Model Consensus (Layer 2)")
        if (context.consensusReport == null || !context.consensusReport.hasConflicts) {
            appendLine("✅ No conflicts - models agreed on all fields")
        } else {
            val conflicts = context.consensusReport.conflicts
            appendLine("⚠️ ${conflicts.size} field conflict(s) detected:")
            conflicts.forEach { conflict ->
                val severity = if (conflict.severity == ConflictSeverity.CRITICAL) "CRITICAL" else "Warning"
                appendLine("  - [$severity] ${conflict.field}: '${conflict.fastValue}' vs '${conflict.expertValue}'")
            }
            appendLine(
                "  Critical conflicts: ${context.consensusReport.conflicts.count { it.severity == ConflictSeverity.CRITICAL }}"
            )
        }
        appendLine()

        // Audit report (Layer 3)
        appendLine("## Validation Audit (Layer 3)")
        appendLine("- Total Checks: ${context.auditReport.checks.size}")
        appendLine("- Passed: ${context.auditReport.passedCount}")
        appendLine("- Failed: ${context.auditReport.failedCount}")
        appendLine("- Status: ${context.auditReport.overallStatus}")
        if (context.auditReport.criticalFailures.isNotEmpty()) {
            appendLine("Critical Failures:")
            context.auditReport.criticalFailures.forEach { check ->
                appendLine("  ❌ ${check.type}: ${check.message}")
            }
        }
        if (context.auditReport.warnings.isNotEmpty()) {
            appendLine("Warnings:")
            context.auditReport.warnings.take(3).forEach { check ->
                appendLine("  ⚠️ ${check.type}: ${check.message}")
            }
            if (context.auditReport.warnings.size > 3) {
                appendLine("  ... and ${context.auditReport.warnings.size - 3} more")
            }
        }
        appendLine()

        // Retry result (Layer 4)
        appendLine("## Self-Correction (Layer 4)")
        when (val retry = context.retryResult) {
            is RetryResult.NoRetryNeeded -> {
                appendLine("✅ No retry needed - passed on first attempt")
            }
            is RetryResult.CorrectedOnRetry<*> -> {
                appendLine("✅ Corrected on retry attempt ${retry.attempt}")
                appendLine("   Corrected fields: ${retry.correctedFields.joinToString(", ")}")
            }
            is RetryResult.StillFailing<*> -> {
                appendLine("❌ Still failing after ${retry.attempts} retry attempts")
                appendLine("   Remaining failures: ${retry.remainingFailures.size}")
            }
            null -> {
                appendLine("ℹ️ Self-correction not attempted")
            }
        }
        appendLine()

        appendLine("## Your Decision")
        appendLine("Based on the above, decide: AUTO_APPROVE, NEEDS_REVIEW, or REJECT")
        appendLine("Consider the 'Silence' philosophy: approve when confident, escalate when uncertain.")
    }

    /**
     * Parse the LLM response into a JudgmentDecision.
     */
    private fun parseJudgmentResponse(response: String, context: JudgmentContext): JudgmentDecision {
        // Try to parse as JSON first
        val jsonMatch = Regex("""\{[\s\S]*}""").find(response)
        if (jsonMatch != null) {
            try {
                return json.decodeFromString<LlmJudgmentResponse>(jsonMatch.value).toDecision(context)
            } catch (_: Exception) {
                // Fall through to keyword parsing
            }
        }

        // Fallback: parse by keywords
        val upperResponse = response.uppercase()
        val outcome = when {
            "AUTO_APPROVE" in upperResponse || "AUTOAPPROVE" in upperResponse -> JudgmentOutcome.AUTO_APPROVE
            "NEEDS_REVIEW" in upperResponse || "NEEDSREVIEW" in upperResponse -> JudgmentOutcome.NEEDS_REVIEW
            "REJECT" in upperResponse -> JudgmentOutcome.REJECT
            else -> JudgmentOutcome.NEEDS_REVIEW // Default to review if unclear
        }

        return when (outcome) {
            JudgmentOutcome.AUTO_APPROVE -> JudgmentDecision.autoApprove(
                confidence = 0.8,
                reasoning = "LLM approved document"
            )
            JudgmentOutcome.NEEDS_REVIEW -> JudgmentDecision.needsReview(
                confidence = 0.6,
                reasoning = "LLM requested review",
                issues = listOf("Review requested by judgment model")
            )
            JudgmentOutcome.REJECT -> JudgmentDecision.reject(
                confidence = 0.8,
                reasoning = "LLM rejected document"
            )
        }
    }

    private fun formatPercent(value: Double): String = "${(value * 100).toInt()}%"

    companion object {
        /**
         * Create a deterministic-only judgment agent (no LLM).
         */
        fun deterministic(config: JudgmentConfig = JudgmentConfig.DEFAULT): JudgmentAgent {
            return JudgmentAgent(
                criteria = JudgmentCriteria(config),
                executor = null,
                model = null,
                config = config
            )
        }

        /**
         * Create an LLM-assisted judgment agent.
         */
        fun withLlm(
            executor: PromptExecutor,
            model: LLModel,
            config: JudgmentConfig = JudgmentConfig.DEFAULT
        ): JudgmentAgent {
            return JudgmentAgent(
                criteria = JudgmentCriteria(config),
                executor = executor,
                model = model,
                config = config
            )
        }

        private val JUDGMENT_SYSTEM_PROMPT = """
            You are the Judgment Agent, the final gatekeeper in a document processing system.

            Your role is to review analysis reports from previous processing layers and make a final decision.

            ## Decision Options

            ### AUTO_APPROVE (Target: 95%+ of documents)
            Choose when ALL of these are true:
            - No critical audit failures remaining
            - No unresolved critical conflicts between models
            - Extraction confidence >= 80%
            - Essential fields are present

            This means the document is processed SILENTLY. The user never sees it.

            ### NEEDS_REVIEW
            Choose when:
            - Warning-level issues present but no critical failures
            - Conflicts exist but were resolved with reasonable confidence
            - Some non-essential fields are missing
            - Extraction confidence is 50-80%

            The user will see the document with specific issues highlighted.

            ### REJECT
            Choose when:
            - Critical audit failures remain after retries
            - Essential fields (amount, vendor, date) are missing
            - Document type couldn't be determined
            - Extraction confidence < 50%

            The document requires manual processing.

            ## Philosophy: "Silence"
            The goal is for users to NEVER see documents unless there's a genuine issue.
            When in doubt, err on the side of AUTO_APPROVE for clean extractions.
            Only escalate when you genuinely cannot verify correctness.

            ## Output Format
            Respond with ONLY a JSON object:
            {
                "decision": "AUTO_APPROVE" | "NEEDS_REVIEW" | "REJECT",
                "confidence": 0.0-1.0,
                "reasoning": "Brief explanation (1-2 sentences)",
                "issuesForUser": ["Issue 1", "Issue 2"]
            }
        """.trimIndent()
    }
}

/**
 * Internal model for parsing LLM responses.
 */
@kotlinx.serialization.Serializable
private data class LlmJudgmentResponse(
    val decision: String,
    val confidence: Double = 0.8,
    val reasoning: String = "",
    val issuesForUser: List<String> = emptyList()
) {
    fun toDecision(context: JudgmentContext): JudgmentDecision {
        val outcome = when (decision.uppercase()) {
            "AUTO_APPROVE", "AUTOAPPROVE" -> JudgmentOutcome.AUTO_APPROVE
            "NEEDS_REVIEW", "NEEDSREVIEW" -> JudgmentOutcome.NEEDS_REVIEW
            "REJECT" -> JudgmentOutcome.REJECT
            else -> JudgmentOutcome.NEEDS_REVIEW
        }

        return when (outcome) {
            JudgmentOutcome.AUTO_APPROVE -> JudgmentDecision.autoApprove(
                confidence = confidence,
                reasoning = reasoning
            )
            JudgmentOutcome.NEEDS_REVIEW -> JudgmentDecision.needsReview(
                confidence = confidence,
                reasoning = reasoning,
                issues = issuesForUser,
                allCriticalChecksPassed = context.auditReport.criticalFailures.isEmpty(),
                hasModelConsensus = context.consensusReport?.hasConflicts != true
            )
            JudgmentOutcome.REJECT -> JudgmentDecision.reject(
                confidence = confidence,
                reasoning = reasoning,
                issues = issuesForUser
            )
        }
    }
}
