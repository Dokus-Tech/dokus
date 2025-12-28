package tech.dokus.domain.model.ai

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// =============================================================================
// Chat-specific ID types
// =============================================================================

/**
 * Strongly typed ID for chat messages.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class ChatMessageId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ChatMessageId = ChatMessageId(Uuid.random())
        fun parse(value: String): ChatMessageId = ChatMessageId(Uuid.parse(value))
    }
}

/**
 * Strongly typed ID for chat sessions.
 * Groups messages into conversations.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class ChatSessionId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ChatSessionId = ChatSessionId(Uuid.random())
        fun parse(value: String): ChatSessionId = ChatSessionId(Uuid.parse(value))
    }
}

// =============================================================================
// Chat-specific enums
// =============================================================================

/**
 * Role of the message sender in the chat.
 */
@Serializable
enum class MessageRole(override val dbValue: String) : DbEnum {
    /** User-submitted question */
    User("USER"),

    /** AI-generated response */
    Assistant("ASSISTANT"),

    /** System message (for context injection) */
    System("SYSTEM");

    companion object {
        fun fromDbValue(value: String): MessageRole =
            entries.find { it.dbValue == value }
                ?: throw IllegalArgumentException("Unknown MessageRole: $value")
    }
}

/**
 * Scope of the chat conversation.
 */
@Serializable
enum class ChatScope(override val dbValue: String) : DbEnum {
    /** Questions about a specific document */
    SingleDoc("SINGLE_DOC"),

    /** Cross-document queries on confirmed documents */
    AllDocs("ALL_DOCS");

    companion object {
        fun fromDbValue(value: String): ChatScope =
            entries.find { it.dbValue == value }
                ?: throw IllegalArgumentException("Unknown ChatScope: $value")
    }
}

// =============================================================================
// Citation model
// =============================================================================

/**
 * Source citation for AI responses.
 * Links a response to the document chunk it was derived from.
 */
@Serializable
data class ChatCitation(
    /** ID of the document chunk this citation references */
    val chunkId: String,

    /** ID of the source document (DocumentProcessing) */
    val documentId: String,

    /** Human-readable document name for display */
    val documentName: String? = null,

    /** Page number in the original document (if applicable) */
    val pageNumber: Int? = null,

    /** Excerpt from the chunk that was used */
    val excerpt: String,

    /** Confidence/relevance score for this citation (0.0 - 1.0) */
    val relevanceScore: Float? = null
)

// =============================================================================
// Chat message DTOs
// =============================================================================

/**
 * Chat message DTO - represents a single message in a document Q&A conversation.
 *
 * Messages are grouped by sessionId into conversations. Each message has a role
 * (USER, ASSISTANT, SYSTEM) and a scope (SINGLE_DOC, ALL_DOCS).
 *
 * AI responses include source citations linking answers to document chunks.
 */
@Serializable
data class ChatMessageDto(
    /** Message ID */
    val id: ChatMessageId,

    /** Tenant for multi-tenant isolation */
    val tenantId: TenantId,

    /** User who initiated the conversation */
    val userId: UserId,

    /** Session ID grouping messages into a conversation */
    val sessionId: ChatSessionId,

    /** Role of the message sender (USER, ASSISTANT, SYSTEM) */
    val role: MessageRole,

    /** Message content (question or response) */
    val content: String,

    /** Chat scope (SINGLE_DOC, ALL_DOCS) */
    val scope: ChatScope,

    /** Document reference when scoped to single document */
    val documentId: DocumentId? = null,

    /** Source citations for ASSISTANT messages */
    val citations: List<ChatCitation>? = null,

    /** Number of chunks retrieved for RAG context */
    val chunksRetrieved: Int? = null,

    /** AI model used for response generation */
    val aiModel: String? = null,

    /** AI provider used (ollama, openai, anthropic) */
    val aiProvider: String? = null,

    /** Response generation time in milliseconds */
    val generationTimeMs: Int? = null,

    /** Prompt tokens used (for billing/monitoring) */
    val promptTokens: Int? = null,

    /** Completion tokens used (for billing/monitoring) */
    val completionTokens: Int? = null,

    /** Message ordering within session */
    val sequenceNumber: Int,

    /** When the message was created */
    val createdAt: LocalDateTime
)

/**
 * Summary view for chat message lists.
 * Lighter weight than full ChatMessageDto.
 */
@Serializable
data class ChatMessageSummary(
    val id: ChatMessageId,
    val sessionId: ChatSessionId,
    val role: MessageRole,
    val content: String,
    val scope: ChatScope,
    val documentId: DocumentId? = null,
    val hasCitations: Boolean = false,
    val createdAt: LocalDateTime
)

/**
 * Chat session summary for listing recent conversations.
 */
@Serializable
data class ChatSessionSummary(
    /** Session ID */
    val sessionId: ChatSessionId,

    /** Scope of the conversation */
    val scope: ChatScope,

    /** Document reference (if SINGLE_DOC scope) */
    val documentId: DocumentId? = null,

    /** Document name (if SINGLE_DOC scope) */
    val documentName: String? = null,

    /** Number of messages in the session */
    val messageCount: Int,

    /** Preview of the last message */
    val lastMessagePreview: String? = null,

    /** When the session was created */
    val createdAt: LocalDateTime,

    /** When the last message was added */
    val lastMessageAt: LocalDateTime
)

/**
 * Paginated response for chat message queries.
 */
@Serializable
data class ChatMessageListResponse(
    val items: List<ChatMessageDto>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)
