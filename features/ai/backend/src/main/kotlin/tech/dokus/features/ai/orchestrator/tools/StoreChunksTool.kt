package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ChunkWithEmbedding
import java.security.MessageDigest

/**
 * Tool for persisting RAG chunks with embeddings to the database.
 *
 * Stores chunks with their vector embeddings for later retrieval
 * during RAG-based chat and search.
 */
class StoreChunksTool(
    private val chunkRepository: ChunkRepository
) : SimpleTool<StoreChunksTool.Args>(
    argsSerializer = Args.serializer(),
    name = "store_chunks",
    description = """
        Persists RAG chunks with embeddings to the database.

        Stores document chunks with their vector embeddings for semantic search.
        Each chunk includes position, content, and embedding vector.

        Use this tool after generating chunks and embeddings.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The document ID to store chunks for")
        val documentId: String,

        @property:LLMDescription("The tenant ID")
        val tenantId: String,

        @property:LLMDescription(
            "JSON array of chunks with format: [{\"content\": \"...\", \"embedding\": [0.1, 0.2, ...], " +
                "\"startOffset\": 0, \"endOffset\": 100, \"pageNumber\": 1}]"
        )
        val chunks: String
    )

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    override suspend fun execute(args: Args): String {
        return try {
            val chunksArray = jsonFormat.parseToJsonElement(args.chunks).jsonArray

            if (chunksArray.isEmpty()) {
                return "SUCCESS: No chunks to store"
            }

            // Parse chunks
            val chunksToStore = chunksArray.mapIndexed { index, element ->
                val obj = element.jsonObject
                val content = obj["content"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Chunk $index missing content")
                val embedding = obj["embedding"]?.jsonArray?.map {
                    it.jsonPrimitive.content.toFloat()
                } ?: throw IllegalArgumentException("Chunk $index missing embedding")

                ChunkWithEmbedding(
                    content = content,
                    chunkIndex = index,
                    totalChunks = chunksArray.size,
                    embedding = embedding,
                    embeddingModel = "nomic-embed-text",
                    startOffset = obj["startOffset"]?.jsonPrimitive?.content?.toIntOrNull(),
                    endOffset = obj["endOffset"]?.jsonPrimitive?.content?.toIntOrNull(),
                    pageNumber = obj["pageNumber"]?.jsonPrimitive?.content?.toIntOrNull(),
                    metadata = null,
                    tokenCount = content.length / 4 // Rough estimate
                )
            }

            // Compute content hash for deduplication
            val combinedContent = chunksToStore.joinToString("\n") { it.content }
            val contentHash = sha256(combinedContent)

            // Store chunks
            chunkRepository.storeChunks(
                tenantId = TenantId.parse(args.tenantId),
                documentId = DocumentId.parse(args.documentId),
                contentHash = contentHash,
                chunks = chunksToStore
            )

            "SUCCESS: Stored ${chunksToStore.size} chunks for document ${args.documentId}"
        } catch (e: Exception) {
            "ERROR: Failed to store chunks: ${e.message}"
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
