@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.features.cashflow.presentation.chat.components

import kotlin.uuid.ExperimentalUuidApi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.model.ai.ChatCitation
import tech.dokus.domain.model.ai.ChatContentBlock
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.DocumentReference
import tech.dokus.domain.model.ai.MessageRole
import tech.dokus.foundation.aura.components.chat.ChatAssistantMessage
import tech.dokus.foundation.aura.components.chat.ChatCitationChips
import tech.dokus.foundation.aura.components.chat.ChatDocumentCardList
import tech.dokus.foundation.aura.components.chat.ChatInvoiceDetailCard
import tech.dokus.foundation.aura.components.chat.ChatSummaryCard
import tech.dokus.foundation.aura.components.chat.ChatTransactionChip
import tech.dokus.foundation.aura.components.chat.ChatUserBubble
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.ai.ChatMessageId
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId

/**
 * Message list for the Intelligence chat screen.
 * Renders user bubbles and assistant messages with structured content blocks.
 */
@Composable
internal fun IntelligenceMessages(
    messages: List<ChatMessageDto>,
    listState: LazyListState,
    onDocumentDownload: (DocumentReference) -> Unit,
    onDocumentClick: (DocumentReference) -> Unit,
    onDownloadAllZip: (List<DocumentReference>) -> Unit,
    onCitationClick: (ChatCitation) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = Constraints.Spacing.xxLarge,
            vertical = Constraints.Spacing.xLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
    ) {
        items(
            items = messages,
            key = { it.id.value },
        ) { message ->
            when (message.role) {
                MessageRole.User -> {
                    ChatUserBubble(text = message.content)
                }
                MessageRole.Assistant -> {
                    AssistantMessageWithBlocks(
                        message = message,
                        onDocumentDownload = onDocumentDownload,
                        onDocumentClick = onDocumentClick,
                        onDownloadAllZip = onDownloadAllZip,
                        onCitationClick = onCitationClick,
                    )
                }
                MessageRole.System -> {
                    // System messages not rendered in v29 design
                }
            }
        }
    }
}

@Composable
private fun AssistantMessageWithBlocks(
    message: ChatMessageDto,
    onDocumentDownload: (DocumentReference) -> Unit,
    onDocumentClick: (DocumentReference) -> Unit,
    onDownloadAllZip: (List<DocumentReference>) -> Unit,
    onCitationClick: (ChatCitation) -> Unit,
) {
    ChatAssistantMessage {
        val blocks = message.contentBlocks
        if (blocks != null) {
            // Render structured content blocks
            Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
                blocks.forEach { block ->
                    when (block) {
                        is ChatContentBlock.Text -> {
                            Text(
                                text = block.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                            )
                        }
                        is ChatContentBlock.Summary -> {
                            ChatSummaryCard(rows = block.rows)
                        }
                        is ChatContentBlock.Documents -> {
                            ChatDocumentCardList(
                                block = block,
                                onDownloadSingle = onDocumentDownload,
                                onDownloadAll = { onDownloadAllZip(block.items) },
                                onDocumentClick = onDocumentClick,
                            )
                        }
                        is ChatContentBlock.InvoiceDetail -> {
                            ChatInvoiceDetailCard(
                                block = block,
                                onDownload = {
                                    block.documentId?.let { id ->
                                        onDocumentDownload(
                                            DocumentReference(
                                                documentId = id,
                                                name = block.name,
                                                ref = block.ref,
                                                type = "Invoice",
                                            )
                                        )
                                    }
                                },
                            )
                        }
                        is ChatContentBlock.Transactions -> {
                            block.items.forEach { tx ->
                                ChatTransactionChip(tx = tx)
                            }
                        }
                    }
                }
            }
        } else {
            // Fallback: plain text
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
            )
        }

        // Follow-up text (if content has blocks, the trailing text from AI)
        // This is handled by Text blocks in the contentBlocks list

        // Citations
        val citations = message.citations
        if (!citations.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(Constraints.Spacing.small))
            ChatCitationChips(
                citations = citations,
                onCitationClick = onCitationClick,
            )
        }
    }
}

@Preview
@Composable
private fun IntelligenceMessagesPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val fixedDate = LocalDateTime(2026, 3, 16, 10, 0)
    val tenantId = TenantId.parse("00000000-0000-0000-0000-000000000001")
    val userId = UserId("00000000-0000-0000-0000-000000000002")
    val sessionId = ChatSessionId.parse("00000000-0000-0000-0000-000000000003")
    val messages = listOf(
        ChatMessageDto(
            id = ChatMessageId.parse("00000000-0000-0000-0000-000000000010"),
            tenantId = tenantId,
            userId = userId,
            sessionId = sessionId,
            role = MessageRole.User,
            content = "What were my biggest expenses in Q4?",
            scope = ChatScope.AllDocs,
            sequenceNumber = 0,
            createdAt = fixedDate,
        ),
        ChatMessageDto(
            id = ChatMessageId.parse("00000000-0000-0000-0000-000000000011"),
            tenantId = tenantId,
            userId = userId,
            sessionId = sessionId,
            role = MessageRole.Assistant,
            content = "Based on your confirmed documents, the largest expense was \u20ac798.60 from SRL Accounting & Tax.",
            scope = ChatScope.AllDocs,
            sequenceNumber = 1,
            createdAt = fixedDate,
        ),
    )
    TestWrapper(parameters) {
        IntelligenceMessages(
            messages = messages,
            listState = rememberLazyListState(),
            onDocumentDownload = {},
            onDocumentClick = {},
            onDownloadAllZip = {},
            onCitationClick = {},
        )
    }
}
