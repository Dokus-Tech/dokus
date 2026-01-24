package tech.dokus.foundation.backend.config

import tech.dokus.domain.database.DbEnum

/**
 * IntelligenceMode defines the system's level of autonomy, compute expectations,
 * and AI processing strategy.
 *
 * This is the SINGLE SOURCE OF TRUTH for:
 * - Hardware assumptions
 * - Model selection (orchestrator, vision, chat)
 * - Self-correction and retry behavior
 * - Concurrency budgets
 *
 * Philosophy:
 * "How much can the system be trusted to operate without human intervention?"
 *
 * Architecture: Single orchestrator with tool calling, no ensemble.
 */
enum class IntelligenceMode(
    override val dbValue: String,
    val orchestratorModel: String,
    val visionModel: String,
    val chatModel: String,
    val maxConcurrentRequests: Int,
) : DbEnum {

    /* =======================================================================
     * MODES
     * ===================================================================== */

    /**
     * ASSISTED
     *
     * Target: Raspberry Pi / low-power edge devices (≤16GB RAM)
     * Philosophy: "I help you, you stay in control."
     *
     * Characteristics:
     * - Single-pass extraction
     * - No self-correction loops
     * - No parallelism
     * - Optimized for survival, not perfection
     */
    Assisted(
        dbValue = "ASSISTED",
        orchestratorModel = "qwen/qwen3-32b",
        visionModel = "qwen/qwen3-vl-8b",
        chatModel = "qwen/qwen3-32b",
        maxConcurrentRequests = 1,
    ),

    /**
     * AUTONOMOUS
     *
     * Target: MacBook Pro M4 Max (32–48GB RAM)
     * Philosophy: "I run the operation, you supervise."
     *
     * Characteristics:
     * - Single orchestrator with tool calling
     * - Validation and self-correction
     * - Example-based few-shot learning
     * - Deterministic, stable behavior
     */
    Autonomous(
        dbValue = "AUTONOMOUS",
        orchestratorModel = "qwen/qwen3-32b",
        visionModel = "qwen/qwen3-vl-8b",
        chatModel = "qwen/qwen3-32b",
        maxConcurrentRequests = 1,
    ),

    /**
     * SOVEREIGN
     *
     * Target: Mac Studio M4 Max (128GB RAM)
     * Philosophy: "The system governs itself."
     *
     * Characteristics:
     * - Orchestrator with advanced tool calling
     * - Deep self-correction (up to 3 retries)
     * - Example-based few-shot learning
     * - Cross-document reasoning
     * - Highest autonomy and throughput
     */
    Sovereign(
        dbValue = "SOVEREIGN",
        orchestratorModel = "openai/gpt-oss-20b",
        visionModel = "qwen/qwen3-vl-30b",
        chatModel = "openai/gpt-oss-20b",
        maxConcurrentRequests = 5,
    );

    companion object {
        fun fromDbValue(value: String): IntelligenceMode {
            return requireNotNull(entries.find { it.dbValue == value }) { "Unknown IntelligenceMode: $value" }
        }
    }
}