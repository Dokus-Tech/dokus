package tech.dokus.database.tables.ai

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.documents.DocumentsTable

/**
 * Chat messages table - stores conversation history for document Q&A.
 *
 * OWNER: cashflow service
 * ACCESS: ai-backend (read for context retrieval)
 * CRITICAL: All queries MUST filter by tenant_id for multi-tenant security.
 *
 * This table stores:
 * - User questions and assistant responses
 * - Conversation grouping via sessionId
 * - Message scope (single document or cross-document)
 * - Source citations as JSON for AI responses
 * - Model and provider information for audit
 *
 * Chat scopes:
 * - SINGLE_DOC: Questions about a specific document
 * - ALL_DOCS: Cross-document queries on confirmed documents
 *
 * Message roles:
 * - USER: User-submitted question
 * - ASSISTANT: AI-generated response
 * - SYSTEM: System messages (optional, for context injection)
 */
object ChatMessagesTable : UuidTable("chat_messages") {

    // Multi-tenancy (CRITICAL for isolation)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // User who initiated the chat (for user messages, references the sender)
    val userId = uuid("user_id")
        .references(UsersTable.id, onDelete = ReferenceOption.CASCADE)

    // Session ID groups messages into conversations
    // Each chat session (e.g., opening document chat UI) gets a unique session
    val sessionId = uuid("session_id")

    // Message role: USER, ASSISTANT, SYSTEM
    val role = varchar("role", 20)

    // Message content (user question or AI response)
    val content = text("content")

    // Chat scope: SINGLE_DOC, ALL_DOCS
    val scope = varchar("scope", 20)

    // Optional: Document reference when scoped to single document
    val documentId = uuid("document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    // Source citations as JSON (for ASSISTANT messages)
    // Format: [{"chunkId": "uuid", "documentId": "uuid", "pageNumber": 1, "excerpt": "..."}]
    val citations = text("citations").nullable()

    // Number of chunks retrieved for RAG context (for ASSISTANT messages)
    val chunksRetrieved = integer("chunks_retrieved").nullable()

    // AI model used for response generation (for ASSISTANT messages)
    val aiModel = varchar("ai_model", 100).nullable()

    // AI provider used (ollama, openai, anthropic)
    val aiProvider = varchar("ai_provider", 50).nullable()

    // Response generation time in milliseconds (for ASSISTANT messages)
    val generationTimeMs = integer("generation_time_ms").nullable()

    // Token usage for the response (for billing/monitoring)
    val promptTokens = integer("prompt_tokens").nullable()
    val completionTokens = integer("completion_tokens").nullable()

    // Message ordering within session
    val sequenceNumber = integer("sequence_number")

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        // CRITICAL: Index tenant_id for all queries (tenant isolation)
        index(false, tenantId)

        // For loading conversation history
        index(false, tenantId, sessionId, sequenceNumber)

        // For finding all sessions by user
        index(false, tenantId, userId)

        // For finding messages about a specific document
        index(false, tenantId, documentId)

        // For recent conversations
        index(false, tenantId, createdAt)
    }
}
