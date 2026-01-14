package tech.dokus.features.ai.coordinator

import kotlinx.serialization.Serializable
import tech.dokus.features.ai.judgment.JudgmentConfig
import tech.dokus.features.ai.retry.RetryConfig
import tech.dokus.features.ai.validation.CheckType

/**
 * Configuration for the Autonomous Processing Pipeline.
 *
 * Controls behavior of all 5 layers:
 * - Layer 1: Perception Ensemble (fast + expert models)
 * - Layer 2: Consensus Engine (conflict resolution)
 * - Layer 3: Legally-Aware Auditor (compliance validation)
 * - Layer 4: Self-Correction Loop (feedback-driven retries)
 * - Layer 5: Judgment Agent (AUTO_APPROVE / NEEDS_REVIEW / REJECT)
 */
@Serializable
data class ProcessingConfig(
    // =========================================================================
    // Layer 1-2: Perception & Consensus
    // =========================================================================

    /**
     * Whether to run both models (ensemble) or just one.
     * When false, only the expert model is used.
     */
    val enableEnsemble: Boolean = true,

    /**
     * Whether to run models in parallel (faster) or sequential (less memory).
     * On systems with <64GB RAM, sequential may be necessary.
     */
    val parallelExtraction: Boolean = true,

    // =========================================================================
    // Layer 3: Validation
    // =========================================================================

    /**
     * Which audit checks to perform.
     * By default, all checks are enabled.
     */
    val enabledChecks: Set<CheckType> = CheckType.entries.toSet(),

    /**
     * Whether to validate against external services (CBE/KBO).
     * Disable for offline operation or testing.
     */
    val enableExternalValidation: Boolean = true,

    // =========================================================================
    // Layer 4: Self-Correction
    // =========================================================================

    /**
     * Whether to attempt self-correction on validation failures.
     */
    val enableSelfCorrection: Boolean = true,

    /**
     * Configuration for the retry agent.
     */
    val retryConfig: RetryConfig = RetryConfig.DEFAULT,

    // =========================================================================
    // Layer 5: Judgment
    // =========================================================================

    /**
     * Configuration for the judgment agent.
     */
    val judgmentConfig: JudgmentConfig = JudgmentConfig.DEFAULT,

    /**
     * Whether to use LLM for judgment edge cases.
     * When false, only deterministic rules are used.
     */
    val useLlmForJudgment: Boolean = false,

    // =========================================================================
    // Pipeline Behavior
    // =========================================================================

    /**
     * Whether to fail fast on classification failure.
     * When true, returns immediately if document type is UNKNOWN.
     * When false, attempts extraction anyway (may still fail).
     */
    val failFastOnUnknownType: Boolean = true,

    /**
     * Whether to include detailed provenance in results.
     * Useful for debugging but increases response size.
     */
    val includeProvenance: Boolean = false,

    /**
     * Minimum confidence to even attempt extraction.
     * Documents with classification confidence below this are rejected early.
     */
    val minClassificationConfidence: Double = 0.3
) {
    companion object {
        /**
         * Default configuration - balanced for most use cases.
         */
        val DEFAULT = ProcessingConfig()

        /**
         * Fast configuration - single model, minimal validation.
         * Use for high-volume, lower-stakes processing.
         */
        val FAST = ProcessingConfig(
            enableEnsemble = false,
            enableSelfCorrection = false,
            enableExternalValidation = false,
            judgmentConfig = JudgmentConfig.LENIENT
        )

        /**
         * Thorough configuration - full ensemble, all validation.
         * Use for high-value documents requiring maximum accuracy.
         */
        val THOROUGH = ProcessingConfig(
            enableEnsemble = true,
            parallelExtraction = true,
            enableSelfCorrection = true,
            retryConfig = RetryConfig.AGGRESSIVE,
            judgmentConfig = JudgmentConfig.STRICT,
            includeProvenance = true
        )

        /**
         * Offline configuration - no external API calls.
         * Use when network is unavailable or for testing.
         */
        val OFFLINE = ProcessingConfig(
            enableExternalValidation = false,
            enabledChecks = setOf(
                CheckType.MATH,
                CheckType.CHECKSUM_OGM,
                CheckType.CHECKSUM_IBAN,
                CheckType.VAT_RATE
                // Excludes COMPANY_EXISTS and COMPANY_NAME
            )
        )

        /**
         * Development/testing configuration.
         * Single model, no retries, lenient judgment.
         */
        val DEVELOPMENT = ProcessingConfig(
            enableEnsemble = false,
            enableSelfCorrection = false,
            enableExternalValidation = false,
            judgmentConfig = JudgmentConfig.LENIENT,
            includeProvenance = true
        )
    }

    /**
     * Validate configuration and return any issues.
     */
    fun validate(): List<String> {
        val issues = mutableListOf<String>()

        if (minClassificationConfidence < 0.0 || minClassificationConfidence > 1.0) {
            issues.add("minClassificationConfidence must be between 0.0 and 1.0")
        }

        if (retryConfig.maxRetries < 0) {
            issues.add("maxRetries cannot be negative")
        }

        if (enabledChecks.isEmpty()) {
            issues.add("At least one check type should be enabled")
        }

        return issues
    }

    /**
     * Check if external validation checks are included.
     */
    val hasExternalChecks: Boolean
        get() = enableExternalValidation &&
            (CheckType.COMPANY_EXISTS in enabledChecks || CheckType.COMPANY_NAME in enabledChecks)
}
