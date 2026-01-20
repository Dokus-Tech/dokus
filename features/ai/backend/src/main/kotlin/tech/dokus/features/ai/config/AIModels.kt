package tech.dokus.features.ai.config

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import tech.dokus.foundation.backend.config.IntelligenceMode

/**
 * AI model factory using IntelligenceMode as the single source of truth.
 *
 * Model selection is fully delegated to IntelligenceMode - this class only
 * handles the creation of LLModel instances with proper capabilities.
 *
 * Architecture: Single orchestrator with vision model for extraction.
 * - orchestrator: Text model for reasoning and tool calling
 * - vision: Vision model for document classification and extraction
 * - chat: Text model for RAG-based document Q&A
 */
object AIModels {

    /** Embedding model name (LM Studio format) */
    const val EMBEDDING_MODEL_NAME = "text-embedding-nomic-embed-text-v1.5"

    /** Embedding dimensions for nomic-embed-text */
    const val EMBEDDING_DIMENSIONS = 768

    /**
     * Get all models for a given intelligence mode.
     */
    fun forMode(mode: IntelligenceMode): ModelSet = ModelSet(
        orchestrator = createModel(mode.orchestratorModel),
        vision = createModel(mode.visionModel),
        chat = createModel(mode.chatModel)
    )

    /**
     * Create an LLModel from a model ID.
     * Uses ModelRegistry for deterministic context length lookup.
     *
     * Uses LLMProvider.OpenAI for LM Studio compatibility (OpenAI-compatible API).
     */
    private fun createModel(id: String): LLModel = LLModel(
        provider = LLMProvider.OpenAI,
        id = id,
        capabilities = buildList {
            add(LLMCapability.Completion)
            add(LLMCapability.OpenAIEndpoint.Completions)
            if (ModelRegistry.isVisionModel(id)) {
                add(LLMCapability.Vision.Image)
            }
        },
        contextLength = ModelRegistry.contextLength(id),
        maxOutputTokens = null
    )
}

/**
 * Complete set of models for a given intelligence mode.
 */
data class ModelSet(
    /** Model for orchestrator reasoning and tool calling (text-only) */
    val orchestrator: LLModel,
    /** Model for vision tasks (classification, extraction) */
    val vision: LLModel,
    /** Model for RAG-based chat */
    val chat: LLModel
)
