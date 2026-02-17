package tech.dokus.database.repository.ai
import kotlin.uuid.Uuid

import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.tables.ai.ChatMessagesTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.ai.ChatCitation
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.ChatMessageId
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.ChatSessionSummary
import tech.dokus.domain.model.ai.MessageRole
import tech.dokus.domain.repository.ChatRepository
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Repository implementation for chat message persistence and conversation management.
 *
 * Provides CRUD operations for chat messages and session management for
 * document Q&A conversations. Supports both single-document and cross-document
 * chat scopes.
 *
 * CRITICAL SECURITY: All queries MUST filter by tenantId for multi-tenant isolation.
 */
class ChatRepositoryImpl : ChatRepository {

    private val logger = loggerFor()

    // =========================================================================
    // Message Operations
    // =========================================================================

    override suspend fun saveMessage(message: ChatMessageDto): ChatMessageDto =
        newSuspendedTransaction {
            val messageId = message.id.value
            val tenantUuid = message.tenantId.value
            val userUuid = message.userId.value
            val sessionUuid = message.sessionId.value

            logger.debug(
                "Saving chat message: id={}, session={}, role={}, tenant={}",
                messageId,
                sessionUuid,
                message.role,
                tenantUuid
            )

            // Serialize citations as JSON array
            val citationsJson = message.citations?.takeIf { it.isNotEmpty() }?.let { cites ->
                json.encodeToString(ListSerializer(ChatCitation.serializer()), cites)
            }

            ChatMessagesTable.insert {
                it[id] = messageId
                it[tenantId] = tenantUuid
                it[userId] = userUuid
                it[sessionId] = sessionUuid
                it[role] = message.role.dbValue
                it[content] = message.content
                it[scope] = message.scope.dbValue
                it[documentId] = message.documentId?.let { docId ->
                    docId.value
                }
                it[citations] = citationsJson
                it[chunksRetrieved] = message.chunksRetrieved
                it[aiModel] = message.aiModel
                it[aiProvider] = message.aiProvider
                it[generationTimeMs] = message.generationTimeMs
                it[promptTokens] = message.promptTokens
                it[completionTokens] = message.completionTokens
                it[sequenceNumber] = message.sequenceNumber
                it[createdAt] = message.createdAt
            }

            logger.debug("Successfully saved chat message: {}", messageId)
            message
        }

    override suspend fun getMessageById(
        tenantId: TenantId,
        messageId: ChatMessageId
    ): ChatMessageDto? = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val msgUuid = messageId.value

        ChatMessagesTable
            .selectAll()
            .where {
                (ChatMessagesTable.id eq msgUuid) and
                    (ChatMessagesTable.tenantId eq tenantUuid)
            }
            .singleOrNull()
            ?.toMessageDto()
    }

    override suspend fun getSessionMessages(
        tenantId: TenantId,
        sessionId: ChatSessionId,
        limit: Int,
        offset: Int,
        descending: Boolean
    ): Pair<List<ChatMessageDto>, Long> = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val sessionUuid = sessionId.value

        val baseQuery = ChatMessagesTable
            .selectAll()
            .where {
                (ChatMessagesTable.tenantId eq tenantUuid) and
                    (ChatMessagesTable.sessionId eq sessionUuid)
            }

        val total = baseQuery.count()

        val sortOrder = if (descending) SortOrder.DESC else SortOrder.ASC
        val messages = baseQuery
            .orderBy(ChatMessagesTable.sequenceNumber to sortOrder)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toMessageDto() }

        messages to total
    }

    override suspend fun getMessagesForDocument(
        tenantId: TenantId,
        documentId: DocumentId,
        limit: Int,
        offset: Int
    ): Pair<List<ChatMessageDto>, Long> = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val documentUuid = documentId.value

        val baseQuery = ChatMessagesTable
            .selectAll()
            .where {
                (ChatMessagesTable.tenantId eq tenantUuid) and
                    (ChatMessagesTable.documentId eq documentUuid)
            }

        val total = baseQuery.count()

        val messages = baseQuery
            .orderBy(ChatMessagesTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toMessageDto() }

        messages to total
    }

    override suspend fun getNextSequenceNumber(
        tenantId: TenantId,
        sessionId: ChatSessionId
    ): Int = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val sessionUuid = sessionId.value

        val maxSeq: Int? = ChatMessagesTable
            .select(ChatMessagesTable.sequenceNumber.max())
            .where {
                (ChatMessagesTable.tenantId eq tenantUuid) and
                    (ChatMessagesTable.sessionId eq sessionUuid)
            }
            .singleOrNull()
            ?.get(ChatMessagesTable.sequenceNumber.max())

        (maxSeq ?: 0).plus(1)
    }

    // =========================================================================
    // Session Operations
    // =========================================================================

    override suspend fun listSessions(
        tenantId: TenantId,
        scope: ChatScope?,
        documentId: DocumentId?,
        limit: Int,
        offset: Int
    ): Pair<List<ChatSessionSummary>, Long> = newSuspendedTransaction {
        val tenantUuid = tenantId.value

        // Build query with optional filters
        var query = ChatMessagesTable
            .selectAll()
            .where { ChatMessagesTable.tenantId eq tenantUuid }

        if (scope != null) {
            query = query.andWhere { ChatMessagesTable.scope eq scope.dbValue }
        }

        if (documentId != null) {
            val documentUuid = documentId.value
            query = query.andWhere { ChatMessagesTable.documentId eq documentUuid }
        }

        // Get distinct sessions
        val allMessages = query
            .orderBy(ChatMessagesTable.createdAt to SortOrder.DESC)
            .toList()

        // Group by session and build summaries
        val sessionGroups = allMessages.groupBy { it[ChatMessagesTable.sessionId] }
        val total = sessionGroups.size.toLong()

        val sessions = sessionGroups.entries
            .sortedByDescending { entry ->
                entry.value.maxOfOrNull { it[ChatMessagesTable.createdAt] }
            }
            .drop(offset)
            .take(limit)
            .map { (sessionUuid, messages) ->
                val firstMessage = messages.minByOrNull { it[ChatMessagesTable.sequenceNumber] }!!
                val lastMessage = messages.maxByOrNull { it[ChatMessagesTable.sequenceNumber] }!!

                ChatSessionSummary(
                    sessionId = ChatSessionId.parse(sessionUuid.toString()),
                    scope = ChatScope.fromDbValue(firstMessage[ChatMessagesTable.scope]),
                    documentId = firstMessage[ChatMessagesTable.documentId]?.let {
                        DocumentId.parse(it.toString())
                    },
                    documentName = null, // Would need join for this
                    messageCount = messages.size,
                    lastMessagePreview = lastMessage[ChatMessagesTable.content].take(100),
                    createdAt = firstMessage[ChatMessagesTable.createdAt],
                    lastMessageAt = lastMessage[ChatMessagesTable.createdAt]
                )
            }

        sessions to total
    }

    override suspend fun getSessionSummary(
        tenantId: TenantId,
        sessionId: ChatSessionId
    ): ChatSessionSummary? = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val sessionUuid = sessionId.value

        val messages = ChatMessagesTable
            .selectAll()
            .where {
                (ChatMessagesTable.tenantId eq tenantUuid) and
                    (ChatMessagesTable.sessionId eq sessionUuid)
            }
            .orderBy(ChatMessagesTable.sequenceNumber to SortOrder.ASC)
            .toList()

        if (messages.isEmpty()) return@newSuspendedTransaction null

        val firstMessage = messages.first()
        val lastMessage = messages.last()

        // Try to get document name if single-doc scope
        val documentName = firstMessage[ChatMessagesTable.documentId]?.let { docId ->
            try {
                (DocumentsTable innerJoin DocumentsTable)
                    .selectAll()
                    .where {
                        (DocumentsTable.id eq docId) and
                            (DocumentsTable.tenantId eq tenantUuid)
                    }
                    .singleOrNull()
                    ?.get(DocumentsTable.filename)
            } catch (e: Exception) {
                logger.warn("Failed to get document name for session: ${e.message}")
                null
            }
        }

        ChatSessionSummary(
            sessionId = ChatSessionId.parse(sessionUuid.toString()),
            scope = ChatScope.fromDbValue(firstMessage[ChatMessagesTable.scope]),
            documentId = firstMessage[ChatMessagesTable.documentId]?.let {
                DocumentId.parse(it.toString())
            },
            documentName = documentName,
            messageCount = messages.size,
            lastMessagePreview = lastMessage[ChatMessagesTable.content].take(100),
            createdAt = firstMessage[ChatMessagesTable.createdAt],
            lastMessageAt = lastMessage[ChatMessagesTable.createdAt]
        )
    }

    override suspend fun sessionExists(
        tenantId: TenantId,
        sessionId: ChatSessionId
    ): Boolean = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val sessionUuid = sessionId.value

        ChatMessagesTable
            .selectAll()
            .where {
                (ChatMessagesTable.tenantId eq tenantUuid) and
                    (ChatMessagesTable.sessionId eq sessionUuid)
            }
            .limit(1)
            .count() > 0
    }

    override suspend fun countMessagesForTenant(
        tenantId: TenantId
    ): Long = newSuspendedTransaction {
        val tenantUuid = tenantId.value

        ChatMessagesTable
            .selectAll()
            .where { ChatMessagesTable.tenantId eq tenantUuid }
            .count()
    }

    override suspend fun countSessionsForTenant(
        tenantId: TenantId
    ): Long = newSuspendedTransaction {
        val tenantUuid = tenantId.value

        ChatMessagesTable
            .select(ChatMessagesTable.sessionId)
            .where { ChatMessagesTable.tenantId eq tenantUuid }
            .withDistinct()
            .count()
    }

    override suspend fun getRecentSessionsForUser(
        tenantId: TenantId,
        userId: UserId,
        limit: Int
    ): List<ChatSessionSummary> = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val userUuid = userId.value

        val messages = ChatMessagesTable
            .selectAll()
            .where {
                (ChatMessagesTable.tenantId eq tenantUuid) and
                    (ChatMessagesTable.userId eq userUuid)
            }
            .orderBy(ChatMessagesTable.createdAt to SortOrder.DESC)
            .toList()

        messages
            .groupBy { it[ChatMessagesTable.sessionId] }
            .entries
            .take(limit)
            .map { (sessionUuid, sessionMessages) ->
                val firstMessage =
                    sessionMessages.minByOrNull { it[ChatMessagesTable.sequenceNumber] }!!
                val lastMessage =
                    sessionMessages.maxByOrNull { it[ChatMessagesTable.sequenceNumber] }!!

                ChatSessionSummary(
                    sessionId = ChatSessionId.parse(sessionUuid.toString()),
                    scope = ChatScope.fromDbValue(firstMessage[ChatMessagesTable.scope]),
                    documentId = firstMessage[ChatMessagesTable.documentId]?.let {
                        DocumentId.parse(it.toString())
                    },
                    documentName = null,
                    messageCount = sessionMessages.size,
                    lastMessagePreview = lastMessage[ChatMessagesTable.content].take(100),
                    createdAt = firstMessage[ChatMessagesTable.createdAt],
                    lastMessageAt = lastMessage[ChatMessagesTable.createdAt]
                )
            }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private fun ResultRow.toMessageDto(): ChatMessageDto {
        val citationsJson = this[ChatMessagesTable.citations]
        val citations = citationsJson?.let {
            try {
                json.decodeFromString<List<ChatCitation>>(it)
            } catch (e: Exception) {
                logger.warn("Failed to parse citations JSON: ${e.message}")
                null
            }
        }

        return ChatMessageDto(
            id = ChatMessageId(this[ChatMessagesTable.id].value),
            tenantId = TenantId(this[ChatMessagesTable.tenantId]),
            userId = UserId(this[ChatMessagesTable.userId]),
            sessionId = ChatSessionId(this[ChatMessagesTable.sessionId]),
            role = MessageRole.fromDbValue(this[ChatMessagesTable.role]),
            content = this[ChatMessagesTable.content],
            scope = ChatScope.fromDbValue(this[ChatMessagesTable.scope]),
            documentId = this[ChatMessagesTable.documentId]?.let {
                DocumentId.parse(it.toString())
            },
            citations = citations,
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
}
