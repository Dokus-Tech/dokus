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
 */
object AIModels {

    /** Embedding model name (for Ollama API calls) */
    const val EMBEDDING_MODEL_NAME = "nomic-embed-text"

    /** Embedding dimensions for nomic-embed-text */
    const val EMBEDDING_DIMENSIONS = 768

    /**
     * Get all models for a given intelligence mode.
     */
    fun forMode(mode: IntelligenceMode): ModelSet = ModelSet(
        classification = createModel(mode.classificationModel),
        fastExtraction = createModel(mode.fastExtractionModel),
        expertExtraction = createModel(mode.expertExtractionModel),
        chat = createModel(mode.chatModel)
    )

    /**
     * Create an LLModel from a model ID.
     * Uses ModelRegistry for deterministic context length lookup.
     */
    private fun createModel(id: String): LLModel = LLModel(
        provider = LLMProvider.Ollama,
        id = id,
        capabilities = if (ModelRegistry.isVisionModel(id))
            listOf(LLMCapability.Vision.Image) else emptyList(),
        contextLength = ModelRegistry.contextLength(id),
        maxOutputTokens = null
    )
}

/**
 * Complete set of models for a given intelligence mode.
 */
data class ModelSet(
    val classification: LLModel,
    val fastExtraction: LLModel,
    val expertExtraction: LLModel,
    val chat: LLModel
)
