package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.ChatMessageEntity
import tech.dokus.database.entity.DocumentChunkEntity
import tech.dokus.database.tables.ai.ChatMessagesTable
import tech.dokus.database.tables.ai.DocumentChunksTable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.ChunkMetadata
import tech.dokus.domain.model.DocumentChunkDto
import tech.dokus.domain.model.DocumentChunkId
import tech.dokus.domain.model.ai.ChatCitation
import tech.dokus.domain.model.ai.ChatContentBlock
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.ChatMessageId
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.MessageRole
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.utils.loggerFor

private val logger = loggerFor("AiMappers")

fun ChatMessageEntity.Companion.from(row: ResultRow): ChatMessageEntity {
    val citationsJson = row[ChatMessagesTable.citations]
    val citations = citationsJson?.let {
        try {
            json.decodeFromString<List<ChatCitation>>(it)
        } catch (e: Exception) {
            logger.warn("Failed to parse citations JSON: ${e.message}")
            null
        }
    }

    val contentBlocksJson = row[ChatMessagesTable.contentBlocks]
    val contentBlocks = contentBlocksJson?.let {
        try {
            json.decodeFromString<List<ChatContentBlock>>(it)
        } catch (e: Exception) {
            logger.warn("Failed to parse content blocks JSON: ${e.message}")
            null
        }
    }

    return ChatMessageEntity(
        id = ChatMessageId.parse(row[ChatMessagesTable.id].value.toString()),
        tenantId = TenantId.parse(row[ChatMessagesTable.tenantId].toString()),
        userId = UserId(row[ChatMessagesTable.userId].toString()),
        sessionId = ChatSessionId.parse(row[ChatMessagesTable.sessionId].toString()),
        role = MessageRole.fromDbValue(row[ChatMessagesTable.role]),
        content = row[ChatMessagesTable.content],
        scope = ChatScope.fromDbValue(row[ChatMessagesTable.scope]),
        documentId = row[ChatMessagesTable.documentId]?.let {
            DocumentId.parse(it.toString())
        },
        citations = citations,
        contentBlocks = contentBlocks,
        chunksRetrieved = row[ChatMessagesTable.chunksRetrieved],
        aiModel = row[ChatMessagesTable.aiModel],
        aiProvider = row[ChatMessagesTable.aiProvider],
        generationTimeMs = row[ChatMessagesTable.generationTimeMs],
        promptTokens = row[ChatMessagesTable.promptTokens],
        completionTokens = row[ChatMessagesTable.completionTokens],
        sequenceNumber = row[ChatMessagesTable.sequenceNumber],
        createdAt = row[ChatMessagesTable.createdAt],
    )
}

fun ChatMessageDto.Companion.from(entity: ChatMessageEntity) = ChatMessageDto(
    id = entity.id,
    tenantId = entity.tenantId,
    userId = entity.userId,
    sessionId = entity.sessionId,
    role = entity.role,
    content = entity.content,
    scope = entity.scope,
    documentId = entity.documentId,
    citations = entity.citations,
    contentBlocks = entity.contentBlocks,
    chunksRetrieved = entity.chunksRetrieved,
    aiModel = entity.aiModel,
    aiProvider = entity.aiProvider,
    generationTimeMs = entity.generationTimeMs,
    promptTokens = entity.promptTokens,
    completionTokens = entity.completionTokens,
    sequenceNumber = entity.sequenceNumber,
    createdAt = entity.createdAt,
)

fun DocumentChunkEntity.Companion.from(row: ResultRow): DocumentChunkEntity {
    val metadataJson = row[DocumentChunksTable.metadata]
    val metadata = metadataJson?.let {
        try {
            json.decodeFromString<ChunkMetadata>(it)
        } catch (e: Exception) {
            logger.warn("Failed to parse chunk metadata: ${e.message}")
            null
        }
    }

    return DocumentChunkEntity(
        id = DocumentChunkId.parse(row[DocumentChunksTable.id].value.toString()),
        documentId = DocumentId.parse(row[DocumentChunksTable.documentId].toString()),
        tenantId = TenantId.parse(row[DocumentChunksTable.tenantId].toString()),
        content = row[DocumentChunksTable.content],
        chunkIndex = row[DocumentChunksTable.chunkIndex],
        totalChunks = row[DocumentChunksTable.totalChunks],
        startOffset = row[DocumentChunksTable.startOffset],
        endOffset = row[DocumentChunksTable.endOffset],
        pageNumber = row[DocumentChunksTable.pageNumber],
        embeddingModel = row[DocumentChunksTable.embeddingModel],
        tokenCount = row[DocumentChunksTable.tokenCount],
        metadata = metadata,
        createdAt = row[DocumentChunksTable.createdAt],
    )
}

fun DocumentChunkDto.Companion.from(entity: DocumentChunkEntity) = DocumentChunkDto(
    id = entity.id,
    documentId = entity.documentId,
    tenantId = entity.tenantId,
    content = entity.content,
    chunkIndex = entity.chunkIndex,
    totalChunks = entity.totalChunks,
    startOffset = entity.startOffset,
    endOffset = entity.endOffset,
    pageNumber = entity.pageNumber,
    embeddingModel = entity.embeddingModel,
    tokenCount = entity.tokenCount,
    metadata = entity.metadata,
    createdAt = entity.createdAt,
)
