package tech.dokus.features.ai.config

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import tech.dokus.foundation.backend.config.AIMode
import tech.dokus.foundation.backend.config.ModelPurpose

/**
 * Pre-defined AI models for different purposes and modes.
 *
 * All models are defined as [LLModel] objects, eliminating string-based
 * lookups and providing type safety.
 *
 * ## Model Selection
 * - LIGHT mode: Smaller models for resource-constrained environments
 * - NORMAL/CLOUD mode: Larger models for quality
 *
 * ## Model Categories
 * - Vision models (qwen3-vl): Document classification and extraction
 * - Chat models (qwen3): RAG chat, categorization, suggestions
 * - Embedding model (nomic-embed-text): Vector embeddings for search
 */
object AIModels {

    // ==========================================================================
    // Context Length Constants
    // ==========================================================================

    private const val CONTEXT_32K = 32_768L
    private const val CONTEXT_128K = 131_072L

    // ==========================================================================
    // Vision Models (for document processing)
    // ==========================================================================

    /** Light vision model for resource-constrained environments */
    val VISION_LIGHT = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3-vl:2b",
        capabilities = listOf(LLMCapability.Vision.Image),
        contextLength = CONTEXT_32K,
        maxOutputTokens = null
    )

    /** Quality vision model for normal/cloud environments */
    val VISION_QUALITY = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3-vl:32b",
        capabilities = listOf(LLMCapability.Vision.Image),
        contextLength = CONTEXT_128K,
        maxOutputTokens = null
    )

    // ==========================================================================
    // Ensemble Models (for Perception Ensemble - Layer 1)
    // ==========================================================================

    /**
     * Fast vision model for ensemble extraction (quick pass).
     * Smaller model that provides baseline extraction with lower latency.
     */
    val VISION_FAST = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3-vl:8b",
        capabilities = listOf(LLMCapability.Vision.Image),
        contextLength = CONTEXT_32K,
        maxOutputTokens = null
    )

    /**
     * Medium vision model for 32-48GB systems (e.g., M4 Max 36GB).
     * Balances quality and memory usage for mid-range hardware.
     */
    val VISION_MEDIUM = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3-vl:32b",
        capabilities = listOf(LLMCapability.Vision.Image),
        contextLength = CONTEXT_128K,
        maxOutputTokens = null
    )

    /**
     * Expert vision model for ensemble extraction (deep pass).
     * Larger model for highest accuracy, catches subtle details.
     * Requires 64GB+ RAM for parallel execution with fast model.
     */
    val VISION_EXPERT = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3-vl:72b",
        capabilities = listOf(LLMCapability.Vision.Image),
        contextLength = CONTEXT_128K,
        maxOutputTokens = null
    )

    // ==========================================================================
    // Chat/Text Models (for RAG, categorization, suggestions)
    // ==========================================================================

    /** Light chat model for resource-constrained environments */
    val CHAT_LIGHT = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3:8b",
        capabilities = emptyList(),
        contextLength = CONTEXT_32K,
        maxOutputTokens = null
    )

    /** Quality chat model for normal/cloud environments */
    val CHAT_QUALITY = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3:30b-a3b",
        capabilities = emptyList(),
        contextLength = CONTEXT_128K,
        maxOutputTokens = null
    )

    // ==========================================================================
    // Embedding Model (always the same)
    // ==========================================================================

    /** Embedding model name (for Ollama API calls) */
    const val EMBEDDING_MODEL_NAME = "nomic-embed-text"

    /** Embedding dimensions for nomic-embed-text */
    const val EMBEDDING_DIMENSIONS = 768

    // ==========================================================================
    // Model Selection
    // ==========================================================================

    /**
     * Get the appropriate model for a given mode and purpose.
     *
     * @param mode The AI mode (LIGHT, NORMAL, CLOUD)
     * @param purpose The intended use of the model
     * @return The [LLModel] to use
     */
    fun forPurpose(mode: AIMode, purpose: ModelPurpose): LLModel {
        val isVisionTask = purpose == ModelPurpose.CLASSIFICATION ||
            purpose == ModelPurpose.DOCUMENT_EXTRACTION

        return if (isVisionTask) {
            visionModel(mode)
        } else {
            chatModel(mode)
        }
    }

    /**
     * Get the vision model for document processing.
     */
    fun visionModel(mode: AIMode): LLModel = when (mode) {
        AIMode.LIGHT -> VISION_LIGHT
        AIMode.MEDIUM, AIMode.NORMAL, AIMode.CLOUD -> VISION_QUALITY
    }

    /**
     * Get the chat model for RAG and text tasks.
     */
    fun chatModel(mode: AIMode): LLModel = when (mode) {
        AIMode.LIGHT -> CHAT_LIGHT
        AIMode.MEDIUM, AIMode.NORMAL, AIMode.CLOUD -> CHAT_QUALITY
    }

    // ==========================================================================
    // Ensemble Model Selection (for 5-Layer Pipeline)
    // ==========================================================================

    /**
     * Get the fast model for ensemble extraction based on AIMode.
     *
     * This model runs first in the perception ensemble (Layer 1) and provides
     * a quick baseline extraction.
     */
    fun ensembleFastModel(mode: AIMode): LLModel = when (mode) {
        AIMode.LIGHT -> VISION_LIGHT       // qwen3-vl:2b
        AIMode.MEDIUM -> VISION_FAST       // qwen3-vl:8b
        AIMode.NORMAL, AIMode.CLOUD -> VISION_FAST  // qwen3-vl:8b
    }

    /**
     * Get the expert model for ensemble extraction based on AIMode.
     *
     * This model provides the highest accuracy extraction and is used
     * to validate/enhance the fast model's output.
     */
    fun ensembleExpertModel(mode: AIMode): LLModel = when (mode) {
        AIMode.LIGHT -> VISION_FAST        // qwen3-vl:8b (largest for light)
        AIMode.MEDIUM -> VISION_MEDIUM     // qwen3-vl:32b (fits in 36GB)
        AIMode.NORMAL, AIMode.CLOUD -> VISION_EXPERT  // qwen3-vl:72b
    }

    /**
     * Whether to run ensemble models in parallel.
     *
     * Parallel execution is faster but requires more RAM (both models loaded).
     * Sequential execution uses less memory but takes longer.
     */
    fun shouldRunParallel(mode: AIMode): Boolean = when (mode) {
        AIMode.LIGHT, AIMode.MEDIUM -> false  // Sequential to fit in memory
        AIMode.NORMAL, AIMode.CLOUD -> true   // Parallel for speed
    }
}
