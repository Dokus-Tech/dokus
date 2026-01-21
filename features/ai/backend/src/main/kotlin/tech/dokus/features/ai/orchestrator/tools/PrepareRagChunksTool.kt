package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.dokus.features.ai.services.ChunkingService

/**
 * Tool for preparing RAG chunks from document text.
 *
 * Uses the ChunkingService to split document text into semantic chunks
 * suitable for vector embedding and retrieval.
 */
class PrepareRagChunksTool(
    private val chunkingService: ChunkingService
) : SimpleTool<PrepareRagChunksTool.Args>(
    argsSerializer = Args.serializer(),
    name = "prepare_rag_chunks",
    description = """
        Prepares document text for RAG (Retrieval Augmented Generation).

        Splits the raw text into semantic chunks that:
        - Respect sentence and paragraph boundaries
        - Are sized appropriately for embedding (typically 500-1000 chars)
        - Include overlap for context continuity
        - Track position for citation back to source

        Returns the chunks ready for embedding.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The raw document text to chunk")
        val rawText: String,

        @property:LLMDescription("Optional page number to associate with chunks (1-indexed)")
        val pageNumber: Int? = null
    )

    @Serializable
    data class ChunkOutput(
        val index: Int,
        val content: String,
        val startOffset: Int,
        val endOffset: Int,
        val estimatedTokens: Int,
        val pageNumber: Int?
    )

    private val jsonFormat = Json { prettyPrint = true }

    override suspend fun execute(args: Args): String {
        if (args.rawText.isBlank()) {
            return "ERROR: Empty text provided for chunking"
        }

        return try {
            val result = chunkingService.chunk(
                text = args.rawText,
                config = ChunkingService.DEFAULT_CONFIG
            )

            val chunks = result.chunks.map { chunk ->
                ChunkOutput(
                    index = chunk.index,
                    content = chunk.content,
                    startOffset = chunk.provenance.offsets?.start ?: 0,
                    endOffset = chunk.provenance.offsets?.end ?: chunk.content.length,
                    estimatedTokens = chunk.estimatedTokens,
                    pageNumber = args.pageNumber
                )
            }

            buildString {
                appendLine("SUCCESS: Created ${chunks.size} chunks")
                appendLine("Strategy: ${result.strategy}")
                appendLine("Total estimated tokens: ${chunks.sumOf { it.estimatedTokens }}")
                appendLine()
                appendLine("Chunks:")
                appendLine(jsonFormat.encodeToString(chunks))
            }
        } catch (e: Exception) {
            "ERROR: Failed to prepare chunks: ${e.message}"
        }
    }
}
