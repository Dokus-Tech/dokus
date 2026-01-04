package tech.dokus.features.ai.config

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
        capabilities = emptyList(),
        contextLength = CONTEXT_32K,
        maxOutputTokens = null
    )

    /** Quality vision model for normal/cloud environments */
    val VISION_QUALITY = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3-vl:32b",
        capabilities = emptyList(),
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
        AIMode.NORMAL, AIMode.CLOUD -> VISION_QUALITY
    }

    /**
     * Get the chat model for RAG and text tasks.
     */
    fun chatModel(mode: AIMode): LLModel = when (mode) {
        AIMode.LIGHT -> CHAT_LIGHT
        AIMode.NORMAL, AIMode.CLOUD -> CHAT_QUALITY
    }
}
