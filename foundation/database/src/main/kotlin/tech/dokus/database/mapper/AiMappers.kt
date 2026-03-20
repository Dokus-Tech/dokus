package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
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

internal fun ResultRow.toChatMessageDto(): ChatMessageDto {
    val citationsJson = this[ChatMessagesTable.citations]
    val citations = citationsJson?.let {
        try {
            json.decodeFromString<List<ChatCitation>>(it)
        } catch (e: Exception) {
            logger.warn("Failed to parse citations JSON: ${e.message}")
            null
        }
    }

    val contentBlocksJson = this[ChatMessagesTable.contentBlocks]
    val contentBlocks = contentBlocksJson?.let {
        try {
            json.decodeFromString<List<ChatContentBlock>>(it)
        } catch (e: Exception) {
            logger.warn("Failed to parse content blocks JSON: ${e.message}")
            null
        }
    }

    return ChatMessageDto(
        id = ChatMessageId.parse(this[ChatMessagesTable.id].value.toString()),
        tenantId = TenantId.parse(this[ChatMessagesTable.tenantId].toString()),
        userId = UserId(this[ChatMessagesTable.userId].toString()),
        sessionId = ChatSessionId.parse(this[ChatMessagesTable.sessionId].toString()),
        role = MessageRole.fromDbValue(this[ChatMessagesTable.role]),
        content = this[ChatMessagesTable.content],
        scope = ChatScope.fromDbValue(this[ChatMessagesTable.scope]),
        documentId = this[ChatMessagesTable.documentId]?.let {
            DocumentId.parse(it.toString())
        },
        citations = citations,
        contentBlocks = contentBlocks,
        chunksRetrieved = this[ChatMessagesTable.chunksRetrieved],
        aiModel = this[ChatMessagesTable.aiModel],
        aiProvider = this[ChatMessagesTable.aiProvider],
        generationTimeMs = this[ChatMessagesTable.generationTimeMs],
        promptTokens = this[ChatMessagesTable.promptTokens],
        completionTokens = this[ChatMessagesTable.completionTokens],
        sequenceNumber = this[ChatMessagesTable.sequenceNumber],
        createdAt = this[ChatMessagesTable.createdAt]
    )
}

internal fun ResultRow.toDocumentChunkDto(): DocumentChunkDto {
    val metadataJson = this[DocumentChunksTable.metadata]
    val metadata = metadataJson?.let {
        try {
            json.decodeFromString<ChunkMetadata>(it)
        } catch (e: Exception) {
            logger.warn("Failed to parse chunk metadata: ${e.message}")
            null
        }
    }

    return DocumentChunkDto(
        id = DocumentChunkId.parse(this[DocumentChunksTable.id].value.toString()),
        documentId = DocumentId.parse(
            this[DocumentChunksTable.documentId].toString()
        ),
        tenantId = TenantId.parse(this[DocumentChunksTable.tenantId].toString()),
        content = this[DocumentChunksTable.content],
        chunkIndex = this[DocumentChunksTable.chunkIndex],
        totalChunks = this[DocumentChunksTable.totalChunks],
        startOffset = this[DocumentChunksTable.startOffset],
        endOffset = this[DocumentChunksTable.endOffset],
        pageNumber = this[DocumentChunksTable.pageNumber],
        embeddingModel = this[DocumentChunksTable.embeddingModel],
        tokenCount = this[DocumentChunksTable.tokenCount],
        metadata = metadata,
        createdAt = this[DocumentChunksTable.createdAt]
    )
}
