package ai.dokus.ai.agents

import ai.dokus.ai.services.RAGService
import ai.dokus.foundation.domain.ids.DocumentProcessingId
import ai.dokus.foundation.domain.ids.TenantId
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Agent responsible for RAG-backed document Q&A with citations.
 *
 * This agent:
 * 1. Retrieves relevant document chunks using vector similarity search
 * 2. Constructs a context-aware prompt with the retrieved chunks
 * 3. Generates a response using the LLM
 * 4. Extracts and returns citations to source documents
 *
 * Supports two scopes:
 * - **Single Document**: Questions about a specific document
 * - **Cross-Document**: Questions across all confirmed documents for a tenant
 *
 * CRITICAL: All operations filter by tenantId for multi-tenant security.
 *
 * Usage:
 * ```kotlin
 * val chatAgent = ChatAgent(executor, model, ragService)
 *
 * // Single document chat
 * val response = chatAgent.chat(
 *     tenantId = tenantId,
 *     question = "What is the total amount on this invoice?",
 *     documentId = documentId
 * )
 *
 * // Cross-document chat
 * val response = chatAgent.chat(
 *     tenantId = tenantId,
 *     question = "How much did we spend at VendorX this year?"
 * )
 * ```
 */
class ChatAgent(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val ragService: RAGService
) {
    private val logger = LoggerFactory.getLogger(ChatAgent::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        /** Default number of chunks to retrieve for context */
        const val DEFAULT_TOP_K = 5

        /** Maximum tokens for context assembly */
        const val DEFAULT_MAX_CONTEXT_TOKENS = 2000

        /** Minimum similarity threshold for chunk retrieval */
        const val DEFAULT_MIN_SIMILARITY = 0.3f
    }

    /**
     * Result of a chat interaction.
     */
    @Serializable
    data class ChatResponse(
        /** The generated answer text */
        val answer: String,
        /** Citations to source documents/chunks */
        val citations: List<Citation>,
        /** Number of chunks retrieved for context */
        val chunksRetrieved: Int,
        /** Whether the response could use RAG context */
        val usedContext: Boolean,
        /** Response generation time in milliseconds */
        val generationTimeMs: Long,
        /** Confidence indicator (based on chunk similarity and context quality) */
        val confidence: Float
    )

    /**
     * A citation referencing a source document chunk.
     */
    @Serializable
    data class Citation(
        /** Chunk ID for reference */
        val chunkId: String,
        /** Document processing ID */
        val documentId: String,
        /** Document filename (if available) */
        val documentName: String?,
        /** Page number in source document (1-indexed, if available) */
        val pageNumber: Int?,
        /** Excerpt from the cited chunk */
        val excerpt: String,
        /** Similarity score of this chunk to the query */
        val similarityScore: Float
    )

    private val baseSystemPrompt = """
        You are a helpful document assistant that answers questions based on provided context.

        Guidelines:
        - Answer questions accurately based ONLY on the provided context
        - If the answer is not in the context, clearly state that you cannot find the information
        - Be concise and direct in your responses
        - When referencing information, cite the source using [Source N] format
        - If multiple sources support an answer, cite all relevant sources
        - Do not make up information that is not in the context
        - For financial/numerical data, quote exact values from the source documents
        - If you're uncertain, express that uncertainty clearly

        Response format:
        - Start with a direct answer to the question
        - Include [Source N] citations inline where information is used
        - Keep responses focused and relevant to the question
    """.trimIndent()

    /**
     * Process a chat question with RAG-backed context retrieval.
     *
     * @param tenantId The tenant ID (REQUIRED for multi-tenant isolation)
     * @param question The user's question
     * @param documentId Optional document ID to scope the search to a single document
     * @param conversationHistory Optional previous messages for context
     * @param topK Number of chunks to retrieve (default: 5)
     * @param minSimilarity Minimum similarity threshold (default: 0.3)
     * @return ChatResponse with the answer and citations
     */
    suspend fun chat(
        tenantId: TenantId,
        question: String,
        documentId: DocumentProcessingId? = null,
        conversationHistory: List<ConversationMessage>? = null,
        topK: Int = DEFAULT_TOP_K,
        minSimilarity: Float = DEFAULT_MIN_SIMILARITY
    ): ChatResponse {
        val startTime = System.currentTimeMillis()
        val scope = if (documentId != null) "single-doc" else "cross-doc"

        logger.debug("Chat request: tenantId=$tenantId, scope=$scope, question=${question.take(100)}...")

        // Step 1: Retrieve relevant chunks using RAG
        val retrievalResult = try {
            ragService.retrieveRelevantChunks(
                tenantId = tenantId,
                query = question,
                documentId = documentId,
                topK = topK,
                minSimilarity = minSimilarity
            )
        } catch (e: Exception) {
            logger.warn("RAG retrieval failed, proceeding without context", e)
            null
        }

        val chunks = retrievalResult?.chunks ?: emptyList()
        val usedContext = chunks.isNotEmpty()

        logger.debug("Retrieved ${chunks.size} chunks for context (scope: $scope)")

        // Step 2: Build context from retrieved chunks
        val context = if (usedContext) {
            ragService.assembleContext(
                chunks = chunks,
                maxTokens = DEFAULT_MAX_CONTEXT_TOKENS,
                includeMetadata = true
            )
        } else {
            "No relevant context found in the documents."
        }

        // Step 3: Construct the system prompt with RAG context
        val systemPrompt = ragService.formatRAGPrompt(
            basePrompt = baseSystemPrompt,
            context = context
        )

        // Step 4: Build user prompt with conversation history if provided
        val userPrompt = buildUserPrompt(question, conversationHistory)

        // Step 5: Generate response using LLM
        val answer = try {
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = model,
                strategy = singleRunStrategy(),
                toolRegistry = ToolRegistry.EMPTY,
                id = "chat-agent",
                systemPrompt = systemPrompt
            )

            agent.run(userPrompt)
        } catch (e: Exception) {
            logger.error("Failed to generate chat response", e)
            "I apologize, but I encountered an error while processing your question. Please try again."
        }

        // Step 6: Extract citations from the used chunks
        val citations = chunks.map { chunk ->
            Citation(
                chunkId = chunk.id,
                documentId = chunk.documentProcessingId,
                documentName = chunk.documentName,
                pageNumber = chunk.pageNumber,
                excerpt = chunk.content.take(200) + if (chunk.content.length > 200) "..." else "",
                similarityScore = chunk.similarityScore
            )
        }

        // Step 7: Calculate confidence based on context quality
        val confidence = calculateConfidence(chunks, usedContext)

        val generationTimeMs = System.currentTimeMillis() - startTime

        logger.debug("Chat response generated in ${generationTimeMs}ms, ${citations.size} citations")

        return ChatResponse(
            answer = answer,
            citations = citations,
            chunksRetrieved = chunks.size,
            usedContext = usedContext,
            generationTimeMs = generationTimeMs,
            confidence = confidence
        )
    }

    /**
     * Process a simple question without conversation history.
     * Convenience method for single-turn interactions.
     */
    suspend fun ask(
        tenantId: TenantId,
        question: String,
        documentId: DocumentProcessingId? = null
    ): ChatResponse {
        return chat(
            tenantId = tenantId,
            question = question,
            documentId = documentId,
            conversationHistory = null
        )
    }

    /**
     * Build the user prompt, optionally including conversation history.
     */
    private fun buildUserPrompt(
        question: String,
        conversationHistory: List<ConversationMessage>?
    ): String {
        if (conversationHistory.isNullOrEmpty()) {
            return question
        }

        val historyContext = buildString {
            appendLine("Previous conversation:")
            conversationHistory.takeLast(5).forEach { message ->
                val role = when (message.role) {
                    MessageRole.USER -> "User"
                    MessageRole.ASSISTANT -> "Assistant"
                    MessageRole.SYSTEM -> "System"
                }
                appendLine("$role: ${message.content}")
            }
            appendLine()
            appendLine("Current question: $question")
        }

        return historyContext
    }

    /**
     * Calculate confidence score based on retrieval quality.
     */
    private fun calculateConfidence(
        chunks: List<RAGService.RetrievedChunk>,
        usedContext: Boolean
    ): Float {
        if (!usedContext || chunks.isEmpty()) {
            return 0.0f
        }

        // Average similarity of top chunks, weighted towards higher similarities
        val avgSimilarity = chunks.map { it.similarityScore }.average().toFloat()

        // Boost confidence if we have multiple high-quality chunks
        val highQualityCount = chunks.count { it.similarityScore > 0.6f }
        val qualityBonus = (highQualityCount.toFloat() / chunks.size.coerceAtLeast(1)) * 0.2f

        return (avgSimilarity + qualityBonus).coerceIn(0.0f, 1.0f)
    }

    /**
     * Check if the chat agent is available (RAG service is configured and working).
     */
    suspend fun isAvailable(): Boolean {
        return try {
            ragService.isAvailable()
        } catch (e: Exception) {
            logger.warn("Chat agent availability check failed", e)
            false
        }
    }
}

/**
 * Represents a message in a conversation.
 */
@Serializable
data class ConversationMessage(
    val role: MessageRole,
    val content: String
)

/**
 * Role of a message in a conversation.
 */
@Serializable
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
