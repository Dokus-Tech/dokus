package tech.dokus.features.ai.config

/**
 * Central registry for model metadata.
 *
 * RATIONALE: Using string heuristics like `id.contains("72b")` is brittle
 * and will break when new models are added. This lookup table ensures
 * deterministic, predictable behavior.
 */
object ModelRegistry {

    private const val CONTEXT_32K = 32_768L
    private const val CONTEXT_128K = 131_072L

    private val contextLengths = mapOf(
        // Vision models (LM Studio format)
        "qwen/qwen3-vl-8b" to CONTEXT_32K,
        "qwen/qwen3-vl-32b" to CONTEXT_128K,
        "qwen/qwen3-vl-30b" to CONTEXT_128K,
        // Chat models (LM Studio format)
        "qwen/qwen3-32b" to CONTEXT_128K
    )

    /**
     * Get context length for a model ID.
     * Returns 32K as safe default for unknown models.
     */
    fun contextLength(modelId: String): Long =
        contextLengths[modelId] ?: CONTEXT_32K

    /**
     * Check if a model has vision capability.
     */
    fun isVisionModel(modelId: String): Boolean =
        modelId.contains("-vl")
}
