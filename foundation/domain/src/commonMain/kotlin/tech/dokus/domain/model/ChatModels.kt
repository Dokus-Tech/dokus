package tech.dokus.domain.model

import ai.dokus.foundation.domain.ids.DocumentProcessingId
import kotlinx.serialization.Serializable

// =============================================================================
// Chat API Request/Response Models
// =============================================================================

/**
 * Request to send a chat message and receive an AI response.
 *
 * Used for both single-document and cross-document chat queries.
 * The scope determines which documents are searched for context.
 */
@Serializable
data class ChatRequest(
    /** The user's question or message */
    val message: String,

    /** Chat scope (SINGLE_DOC or ALL_DOCS) */
    val scope: ChatScope,

    /**
     * Document ID for single-document chat.
     * Required when scope is SINGLE_DOC.
     */
    val documentProcessingId: DocumentProcessingId? = null,

    /**
     * Session ID to continue an existing conversation.
     * If null, a new session is created.
     */
    val sessionId: ChatSessionId? = null,

    /**
     * Maximum number of chunks to retrieve for context (RAG).
     * Defaults to 5 if not specified.
     */
    val maxChunks: Int? = null,

    /**
     * Minimum similarity score for retrieved chunks (0.0 - 1.0).
     * Chunks below this threshold are excluded from context.
     */
    val minSimilarity: Float? = null,

    /**
     * Preferred AI model for response generation.
     * If not specified, uses the default configured model.
     */
    val preferredModel: String? = null,

    /**
     * Whether to include source citations in the response.
     * Defaults to true.
     */
    val includeCitations: Boolean = true
)

/**
 * Response from the chat API containing the AI-generated answer.
 *
 * Includes the response message, source citations, and metadata
 * about the generation process.
 */
@Serializable
data class ChatResponse(
    /** The generated user message (echo) */
    val userMessage: ChatMessageDto,

    /** The AI-generated response message */
    val assistantMessage: ChatMessageDto,

    /** Session ID for continuing the conversation */
    val sessionId: ChatSessionId,

    /** Whether this is a new session */
    val isNewSession: Boolean,

    /** Generation metadata for monitoring/debugging */
    val metadata: ChatResponseMetadata? = null
)

/**
 * Metadata about the chat response generation.
 */
@Serializable
data class ChatResponseMetadata(
    /** Number of chunks retrieved for context */
    val chunksRetrieved: Int,

    /** Number of chunks used after filtering */
    val chunksUsed: Int,

    /** Total context length in tokens (approximate) */
    val contextTokens: Int? = null,

    /** Response generation time in milliseconds */
    val generationTimeMs: Int,

    /** AI model used */
    val model: String,

    /** AI provider used */
    val provider: String,

    /** Prompt tokens used */
    val promptTokens: Int? = null,

    /** Completion tokens used */
    val completionTokens: Int? = null,

    /** Total tokens used */
    val totalTokens: Int? = null
)

// =============================================================================
// Streaming Chat Models (for future SSE support)
// =============================================================================

/**
 * Streaming chat request for Server-Sent Events.
 */
@Serializable
data class StreamingChatRequest(
    /** The user's question or message */
    val message: String,

    /** Chat scope (SINGLE_DOC or ALL_DOCS) */
    val scope: ChatScope,

    /** Document ID for single-document chat */
    val documentProcessingId: DocumentProcessingId? = null,

    /** Session ID to continue an existing conversation */
    val sessionId: ChatSessionId? = null,

    /** Maximum number of chunks to retrieve */
    val maxChunks: Int? = null,

    /** Minimum similarity score for chunks */
    val minSimilarity: Float? = null
)

/**
 * Streaming chat event types.
 */
@Serializable
enum class ChatStreamEventType {
    /** Session info (sent first) */
    SESSION_START,

    /** Chunk of response content */
    CONTENT_DELTA,

    /** Citation information */
    CITATION,

    /** Generation complete */
    DONE,

    /** Error occurred */
    ERROR
}

/**
 * Streaming chat event for SSE responses.
 */
@Serializable
data class ChatStreamEvent(
    /** Event type */
    val type: ChatStreamEventType,

    /** Content delta (for CONTENT_DELTA events) */
    val content: String? = null,

    /** Session ID (for SESSION_START events) */
    val sessionId: ChatSessionId? = null,

    /** Citation data (for CITATION events) */
    val citation: ChatCitation? = null,

    /** Metadata (for DONE events) */
    val metadata: ChatResponseMetadata? = null,

    /** Error message (for ERROR events) */
    val error: String? = null
)

// =============================================================================
// Chat Session Management
// =============================================================================

/**
 * Request to list chat sessions.
 */
@Serializable
data class ChatSessionListRequest(
    /** Filter by scope */
    val scope: ChatScope? = null,

    /** Filter by document ID */
    val documentProcessingId: DocumentProcessingId? = null,

    /** Pagination: page number (0-indexed) */
    val page: Int = 0,

    /** Pagination: items per page */
    val limit: Int = 20
)

/**
 * Response containing a list of chat sessions.
 */
@Serializable
data class ChatSessionListResponse(
    val items: List<ChatSessionSummary>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

/**
 * Request to get chat history for a session.
 */
@Serializable
data class ChatHistoryRequest(
    /** Session to retrieve history for */
    val sessionId: ChatSessionId,

    /** Pagination: page number (0-indexed) */
    val page: Int = 0,

    /** Pagination: items per page */
    val limit: Int = 50,

    /** Order: true for newest first */
    val descending: Boolean = false
)

/**
 * Full chat history for a session.
 */
@Serializable
data class ChatHistoryResponse(
    /** Session ID */
    val sessionId: ChatSessionId,

    /** Session metadata */
    val session: ChatSessionSummary,

    /** Messages in the session */
    val messages: List<ChatMessageDto>,

    /** Total message count */
    val total: Long,

    /** Current page */
    val page: Int,

    /** Page size */
    val limit: Int,

    /** More messages available */
    val hasMore: Boolean
)

// =============================================================================
// Chat Analytics and Feedback
// =============================================================================

/**
 * User feedback on a chat response.
 */
@Serializable
data class ChatFeedbackRequest(
    /** Message ID being rated */
    val messageId: ChatMessageId,

    /** Rating (1-5 or thumbs up/down as 1/0) */
    val rating: Int,

    /** Optional feedback text */
    val feedback: String? = null,

    /** Specific issues (if negative feedback) */
    val issues: List<ChatFeedbackIssue>? = null
)

/**
 * Types of issues users can report.
 */
@Serializable
enum class ChatFeedbackIssue {
    /** Response was factually incorrect */
    INCORRECT,

    /** Response was not relevant to the question */
    NOT_RELEVANT,

    /** Response was incomplete */
    INCOMPLETE,

    /** Citations were wrong or missing */
    WRONG_CITATIONS,

    /** Response was too slow */
    TOO_SLOW,

    /** Other issue */
    OTHER
}

/**
 * Confirmation response for feedback submission.
 */
@Serializable
data class ChatFeedbackResponse(
    /** Whether feedback was recorded */
    val success: Boolean,

    /** Confirmation message */
    val message: String
)

// =============================================================================
// Chat Configuration
// =============================================================================

/**
 * Chat configuration for UI/client.
 * Returned from a config endpoint to inform UI capabilities.
 */
@Serializable
data class ChatConfiguration(
    /** Maximum message length in characters */
    val maxMessageLength: Int = 4000,

    /** Maximum chunks retrievable per query */
    val maxChunksPerQuery: Int = 10,

    /** Default chunks per query */
    val defaultChunksPerQuery: Int = 5,

    /** Whether streaming is supported */
    val streamingEnabled: Boolean = false,

    /** Available AI models for chat */
    val availableModels: List<String> = emptyList(),

    /** Default AI model */
    val defaultModel: String? = null,

    /** Whether cross-document chat is enabled */
    val crossDocumentChatEnabled: Boolean = true,

    /** Minimum documents required for cross-document chat */
    val minDocumentsForCrossDocChat: Int = 1
)
