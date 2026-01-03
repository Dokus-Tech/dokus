package tech.dokus.features.ai.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tech.dokus.domain.model.ai.AiProvider
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Service for generating text embeddings using AI providers.
 *
 * Supports multiple embedding providers:
 * - **Ollama**: Uses `nomic-embed-text` model (768 dimensions) via `/api/embeddings` endpoint
 * - **OpenAI**: Uses `text-embedding-3-small` model (1536 dimensions) via `/v1/embeddings` endpoint
 *
 * Usage:
 * ```kotlin
 * val embeddingService = EmbeddingService(httpClient, config)
 * val embeddings = embeddingService.generateEmbedding("Hello world")
 * // embeddings: List<Float> with 768 or 1536 dimensions
 * ```
 */
class EmbeddingService(
    private val httpClient: HttpClient,
    private val config: AIConfig
) {
    private val logger = loggerFor()

    companion object {
        /** Default embedding model for Ollama (768 dimensions) */
        const val OLLAMA_EMBEDDING_MODEL = "nomic-embed-text"

        /** Default embedding model for OpenAI (1536 dimensions) */
        const val OPENAI_EMBEDDING_MODEL = "text-embedding-3-small"

        /** Embedding dimensions for Ollama nomic-embed-text */
        const val OLLAMA_DIMENSIONS = 768

        /** Embedding dimensions for OpenAI text-embedding-3-small */
        const val OPENAI_DIMENSIONS = 1536
    }

    /**
     * Result of embedding generation.
     */
    data class EmbeddingResult(
        /** The embedding vector */
        val embedding: List<Float>,
        /** Number of dimensions in the embedding */
        val dimensions: Int,
        /** Model used for generation */
        val model: String,
        /** Provider used (ollama or openai) */
        val provider: String,
        /** Token count for the input text (if available) */
        val tokenCount: Int? = null
    )

    /**
     * Generate embeddings for a single text input.
     *
     * @param text The text to embed
     * @param model Optional model override (defaults to provider's default model)
     * @return EmbeddingResult containing the embedding vector and metadata
     * @throws EmbeddingException if embedding generation fails
     */
    suspend fun generateEmbedding(
        text: String,
        model: String? = null
    ): EmbeddingResult {
        return when (config.defaultProvider) {
            AiProvider.Ollama -> generateOllamaEmbedding(
                text,
                model ?: OLLAMA_EMBEDDING_MODEL
            )

            AiProvider.OpenAi -> generateOpenAIEmbedding(
                text,
                model ?: OPENAI_EMBEDDING_MODEL
            )
        }
    }

    /**
     * Generate embeddings for multiple text inputs in batch.
     *
     * For Ollama, this makes sequential calls (Ollama doesn't support batch embeddings).
     * For OpenAI, this uses a single batch API call for efficiency.
     *
     * @param texts The list of texts to embed
     * @param model Optional model override
     * @return List of EmbeddingResults in the same order as input texts
     * @throws EmbeddingException if embedding generation fails
     */
    suspend fun generateEmbeddings(
        texts: List<String>,
        model: String? = null
    ): List<EmbeddingResult> {
        if (texts.isEmpty()) return emptyList()

        return when (config.defaultProvider) {
            AiProvider.Ollama -> {
                // Ollama doesn't support batch embeddings, process sequentially
                texts.map { text -> generateOllamaEmbedding(text, model ?: OLLAMA_EMBEDDING_MODEL) }
            }

            AiProvider.OpenAi -> {
                // OpenAI supports batch embeddings
                generateOpenAIEmbeddingsBatch(texts, model ?: OPENAI_EMBEDDING_MODEL)
            }
        }
    }

    /**
     * Get the embedding dimensions for the current provider's default model.
     */
    fun getEmbeddingDimensions(): Int {
        return when (config.defaultProvider) {
            AiProvider.Ollama -> OLLAMA_DIMENSIONS
            AiProvider.OpenAi -> OPENAI_DIMENSIONS
        }
    }

    /**
     * Check if the embedding service is available and configured.
     */
    suspend fun isAvailable(): Boolean {
        return try {
            when (config.defaultProvider) {
                AiProvider.Ollama -> config.ollama.enabled
                AiProvider.OpenAi -> config.openai.enabled && config.openai.apiKey.isNotBlank()
            }
        } catch (e: Exception) {
            logger.warn("Embedding service availability check failed", e)
            false
        }
    }

    // =========================================================================
    // Ollama Implementation
    // =========================================================================

    private suspend fun generateOllamaEmbedding(text: String, model: String): EmbeddingResult {
        val baseUrl = config.ollama.baseUrl.trimEnd('/')
        val url = "$baseUrl/api/embeddings"

        logger.debug("Generating Ollama embedding: model=$model, textLength=${text.length}")

        try {
            val request = OllamaEmbeddingRequest(
                model = model,
                prompt = text
            )

            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(OllamaEmbeddingRequest.serializer(), request))
            }

            if (response.status.value !in 200..299) {
                val errorBody = response.body<String>()
                logger.error("Ollama embedding API error: ${response.status} - $errorBody")
                throw EmbeddingException(
                    message = "Ollama API returned ${response.status}",
                    provider = "ollama",
                    isRetryable = response.status.value >= 500
                )
            }

            val ollamaResponse = response.body<OllamaEmbeddingResponse>()
            val embedding = ollamaResponse.embedding

            if (embedding.isEmpty()) {
                throw EmbeddingException(
                    message = "Ollama returned empty embedding",
                    provider = "ollama",
                    isRetryable = true
                )
            }

            logger.debug("Ollama embedding generated: dimensions=${embedding.size}")

            return EmbeddingResult(
                embedding = embedding,
                dimensions = embedding.size,
                model = model,
                provider = "ollama"
            )
        } catch (e: EmbeddingException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to generate Ollama embedding", e)
            throw EmbeddingException(
                message = "Failed to generate embedding: ${e.message}",
                provider = "ollama",
                isRetryable = true,
                cause = e
            )
        }
    }

    // =========================================================================
    // OpenAI Implementation
    // =========================================================================

    private suspend fun generateOpenAIEmbedding(text: String, model: String): EmbeddingResult {
        val results = generateOpenAIEmbeddingsBatch(listOf(text), model)
        return results.first()
    }

    private suspend fun generateOpenAIEmbeddingsBatch(
        texts: List<String>,
        model: String
    ): List<EmbeddingResult> {
        val baseUrl = "https://api.openai.com/v1"
        val url = "$baseUrl/embeddings"

        logger.debug("Generating OpenAI embeddings: model=$model, count=${texts.size}")

        try {
            val request = OpenAIEmbeddingRequest(
                model = model,
                input = texts
            )

            val response = httpClient.post(url) {
                header("Authorization", "Bearer ${config.openai.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(OpenAIEmbeddingRequest.serializer(), request))
            }

            if (response.status.value !in 200..299) {
                val errorBody = response.body<String>()
                logger.error("OpenAI embedding API error: ${response.status} - $errorBody")
                throw EmbeddingException(
                    message = "OpenAI API returned ${response.status}",
                    provider = "openai",
                    isRetryable = response.status.value >= 500 || response.status.value == 429
                )
            }

            val openAIResponse = response.body<OpenAIEmbeddingResponse>()

            // Sort by index to ensure order matches input
            val sortedData = openAIResponse.data.sortedBy { it.index }

            val results = sortedData.map { embeddingData ->
                EmbeddingResult(
                    embedding = embeddingData.embedding,
                    dimensions = embeddingData.embedding.size,
                    model = model,
                    provider = "openai",
                    tokenCount = openAIResponse.usage?.totalTokens?.let { it / texts.size }
                )
            }

            logger.debug(
                "OpenAI embeddings generated: count=${results.size}, dimensions=${results.firstOrNull()?.dimensions}"
            )

            return results
        } catch (e: EmbeddingException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to generate OpenAI embeddings", e)
            throw EmbeddingException(
                message = "Failed to generate embeddings: ${e.message}",
                provider = "openai",
                isRetryable = true,
                cause = e
            )
        }
    }
}

/**
 * Exception thrown when embedding generation fails.
 */
class EmbeddingException(
    message: String,
    val provider: String,
    val isRetryable: Boolean = false,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// =========================================================================
// Ollama API Types
// =========================================================================

@Serializable
private data class OllamaEmbeddingRequest(
    val model: String,
    val prompt: String
)

@Serializable
private data class OllamaEmbeddingResponse(
    val embedding: List<Float>
)

// =========================================================================
// OpenAI API Types
// =========================================================================

@Serializable
private data class OpenAIEmbeddingRequest(
    val model: String,
    val input: List<String>
)

@Serializable
private data class OpenAIEmbeddingResponse(
    val data: List<OpenAIEmbeddingData>,
    val model: String,
    val usage: OpenAIUsage? = null
)

@Serializable
private data class OpenAIEmbeddingData(
    val embedding: List<Float>,
    val index: Int,
    @SerialName("object")
    val objectType: String = "embedding"
)

@Serializable
private data class OpenAIUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)
