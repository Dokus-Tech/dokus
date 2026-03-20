package tech.dokus.database.entity

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.ChunkMetadata
import tech.dokus.domain.model.DocumentChunkId
import tech.dokus.domain.model.ai.ChatCitationDto
import tech.dokus.domain.model.ai.ChatContentBlock
import tech.dokus.domain.model.ai.ChatMessageId
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.MessageRole

data class ChatMessageEntity(
    val id: ChatMessageId,
    val tenantId: TenantId,
    val userId: UserId,
    val sessionId: ChatSessionId,
    val role: MessageRole,
    val content: String,
    val scope: ChatScope,
    val documentId: DocumentId? = null,
    val citations: List<ChatCitationDto>? = null,
    val contentBlocks: List<ChatContentBlock>? = null,
    val chunksRetrieved: Int? = null,
    val aiModel: String? = null,
    val aiProvider: String? = null,
    val generationTimeMs: Int? = null,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val sequenceNumber: Int,
    val createdAt: LocalDateTime,
) {
    companion object
}

data class DocumentChunkEntity(
    val id: DocumentChunkId,
    val documentId: DocumentId,
    val tenantId: TenantId,
    val content: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val startOffset: Int? = null,
    val endOffset: Int? = null,
    val pageNumber: Int? = null,
    val embeddingModel: String? = null,
    val tokenCount: Int? = null,
    val metadata: ChunkMetadata? = null,
    val createdAt: LocalDateTime,
) {
    companion object
}
