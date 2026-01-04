package tech.dokus.features.ai.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Service for generating text embeddings via Ollama.
 *
 * Uses `nomic-embed-text` model (768 dimensions) via `/api/embeddings` endpoint.
 * This model is hardcoded as it runs on all hardware and provides good quality.
 *
 * Usage:
 * ```kotlin
 * val embeddingService = EmbeddingService(httpClient, config)
 * val embeddings = embeddingService.generateEmbedding("Hello world")
 * // embeddings: List<Float> with 768 dimensions
 * ```
 */
class EmbeddingService(
    private val httpClient: HttpClient,
    private val config: AIConfig
) {
    private val logger = loggerFor()

    companion object {
        /** Embedding model (hardcoded, not configurable) */
        const val EMBEDDING_MODEL = "nomic-embed-text"

        /** Embedding dimensions for nomic-embed-text */
        const val EMBEDDING_DIMENSIONS = 768
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
        val model: String
    )

    /**
     * Generate embeddings for a single text input.
     *
     * @param text The text to embed
     * @return EmbeddingResult containing the embedding vector and metadata
     * @throws EmbeddingException if embedding generation fails
     */
    suspend fun generateEmbedding(text: String): EmbeddingResult {
        val baseUrl = config.ollamaHost.trimEnd('/')
        val url = "$baseUrl/api/embeddings"

        logger.debug("Generating embedding: model=$EMBEDDING_MODEL, textLength=${text.length}")

        try {
            val request = OllamaEmbeddingRequest(
                model = EMBEDDING_MODEL,
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
                    isRetryable = response.status.value >= 500
                )
            }

            val ollamaResponse = response.body<OllamaEmbeddingResponse>()
            val embedding = ollamaResponse.embedding

            if (embedding.isEmpty()) {
                throw EmbeddingException(
                    message = "Ollama returned empty embedding",
                    isRetryable = true
                )
            }

            logger.debug("Embedding generated: dimensions=${embedding.size}")

            return EmbeddingResult(
                embedding = embedding,
                dimensions = embedding.size,
                model = EMBEDDING_MODEL
            )
        } catch (e: EmbeddingException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to generate embedding", e)
            throw EmbeddingException(
                message = "Failed to generate embedding: ${e.message}",
                isRetryable = true,
                cause = e
            )
        }
    }

    /**
     * Generate embeddings for multiple text inputs.
     * Processes sequentially (Ollama doesn't support batch embeddings).
     *
     * @param texts The list of texts to embed
     * @return List of EmbeddingResults in the same order as input texts
     * @throws EmbeddingException if embedding generation fails
     */
    suspend fun generateEmbeddings(texts: List<String>): List<EmbeddingResult> {
        if (texts.isEmpty()) return emptyList()
        return texts.map { generateEmbedding(it) }
    }

    /**
     * Get the embedding dimensions (always 768 for nomic-embed-text).
     */
    fun getEmbeddingDimensions(): Int = EMBEDDING_DIMENSIONS

    /**
     * Check if the embedding service is available.
     * Always returns true since Ollama is always configured.
     */
    fun isAvailable(): Boolean = true
}

/**
 * Exception thrown when embedding generation fails.
 */
class EmbeddingException(
    message: String,
    val isRetryable: Boolean = false,
    cause: Throwable? = null
) : RuntimeException(message, cause)

@Serializable
private data class OllamaEmbeddingRequest(
    val model: String,
    val prompt: String
)

@Serializable
private data class OllamaEmbeddingResponse(
    val embedding: List<Float>
)
