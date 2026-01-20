package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import tech.dokus.features.ai.services.EmbeddingService

/**
 * Tool for generating vector embeddings from text.
 *
 * Uses Ollama's nomic-embed-text model to generate 768-dimensional
 * embeddings for semantic similarity search.
 */
class EmbedTextTool(
    private val embeddingService: EmbeddingService
) : SimpleTool<EmbedTextTool.Args>(
    argsSerializer = Args.serializer(),
    name = "embed_text",
    description = """
        Generates vector embeddings for semantic search.

        Uses nomic-embed-text model to create 768-dimensional embeddings.
        These embeddings enable similarity search for RAG retrieval.

        Returns the embedding vector as a comma-separated list of floats.
        The embedding can be stored alongside text chunks for later retrieval.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The text to generate embeddings for")
        val text: String
    )

    override suspend fun execute(args: Args): String {
        if (args.text.isBlank()) {
            return "ERROR: Empty text provided for embedding"
        }

        return try {
            val result = embeddingService.generateEmbedding(args.text)

            buildString {
                appendLine("SUCCESS: Generated embedding")
                appendLine("Model: ${result.model}")
                appendLine("Dimensions: ${result.dimensions}")
                appendLine()
                // Truncate embedding for display (full embedding would be too long)
                val preview = result.embedding.take(10).joinToString(", ")
                appendLine("Embedding preview (first 10 dims): [$preview, ...]")
                appendLine()
                appendLine("Full embedding (768 floats):")
                appendLine(result.embedding.joinToString(","))
            }
        } catch (e: Exception) {
            "ERROR: Failed to generate embedding: ${e.message}"
        }
    }
}
