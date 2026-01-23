package tech.dokus.foundation.backend.config

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
sealed interface IntelligenceMode {

    /* -------------------------------------------------------------------------
     * Identity
     * ---------------------------------------------------------------------- */

    val name: String
    val configValue: String

    /* -------------------------------------------------------------------------
     * Hardware profile
     * ---------------------------------------------------------------------- */

    /** Minimum RAM required for this mode to function safely */
    val minRamGb: Int

    /** Whether the hardware is capable of parallel inference */
    val parallelCapable: Boolean

    /* -------------------------------------------------------------------------
     * Model profile
     * ---------------------------------------------------------------------- */

    /** Model for orchestrator reasoning (text-only, tool calling) */
    val orchestratorModel: String

    /** Model for vision tasks (classification, extraction) */
    val visionModel: String

    /** Model for chat/RAG */
    val chatModel: String

    /* -------------------------------------------------------------------------
     * Processing strategy
     * ---------------------------------------------------------------------- */

    /** Whether self-correction loops are allowed */
    val enableSelfCorrection: Boolean

    /** Maximum retry attempts per document */
    val maxRetries: Int

    /* -------------------------------------------------------------------------
     * Concurrency budgets (CRITICAL for stability)
     * ---------------------------------------------------------------------- */

    /** Max concurrent LLM requests system-wide */
    val maxConcurrentRequests: Int

    /** Max parallel agents per single document */
    val maxParallelAgentsPerDocument: Int

    /* -------------------------------------------------------------------------
     * Autonomy
     * ---------------------------------------------------------------------- */

    val autonomyLevel: AutonomyLevel

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
    data object Assisted : IntelligenceMode {

        override val name = "Assisted"
        override val configValue = "assisted"

        // Hardware
        override val minRamGb = 8
        override val parallelCapable = false

        // Models (LM Studio format: qwen/qwen3-vl-8b)
        override val orchestratorModel = "qwen/qwen3-32b"
        override val visionModel = "qwen/qwen3-vl-8b"
        override val chatModel = "qwen/qwen3-32b"

        // Processing
        override val enableSelfCorrection = false
        override val maxRetries = 1

        // Concurrency
        override val maxConcurrentRequests = 1
        override val maxParallelAgentsPerDocument = 1

        // Autonomy
        override val autonomyLevel = AutonomyLevel.LOW
    }

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
    data object Autonomous : IntelligenceMode {

        override val name = "Autonomous"
        override val configValue = "autonomous"

        // Hardware
        override val minRamGb = 32
        override val parallelCapable = false

        // Models (LM Studio format: qwen/qwen3-vl-8b)
        override val orchestratorModel = "qwen/qwen3-32b"
        override val visionModel = "qwen/qwen3-vl-8b"
        override val chatModel = "qwen/qwen3-32b"

        // Processing
        override val enableSelfCorrection = true
        override val maxRetries = 2

        // Concurrency
        override val maxConcurrentRequests = 1
        override val maxParallelAgentsPerDocument = 1

        // Autonomy
        override val autonomyLevel = AutonomyLevel.MEDIUM
    }

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
    data object Sovereign : IntelligenceMode {

        override val name = "Sovereign"
        override val configValue = "sovereign"

        // Hardware - YOUR Mac Studio
        override val minRamGb = 128
        override val parallelCapable = true

        // Models (LM Studio format: qwen/qwen3-vl-8b)
        override val orchestratorModel = "openai/gpt-oss-20b"
        override val visionModel = "qwen/qwen3-vl-30b"
        override val chatModel = "openai/gpt-oss-20b"

        // Processing
        override val enableSelfCorrection = true
        override val maxRetries = 3

        // Concurrency
        override val maxConcurrentRequests = 5
        override val maxParallelAgentsPerDocument = 4

        override val autonomyLevel = AutonomyLevel.HIGH
    }

    companion object {

        fun fromConfigValue(value: String): IntelligenceMode =
            when (value.lowercase()) {
                "assisted" -> Assisted
                "autonomous" -> Autonomous
                "sovereign" -> Sovereign
                else -> error(
                    "Unknown IntelligenceMode '$value'. " +
                        "Valid values: assisted, autonomous, sovereign"
                )
            }

        val all: List<IntelligenceMode> =
            listOf(Assisted, Autonomous, Sovereign)
    }
}

/**
 * Describes how independently the system can operate.
 */
enum class AutonomyLevel {
    LOW,    // Assists, requires human control
    MEDIUM, // Operates with supervision
    HIGH    // Self-governing within boundaries
}
