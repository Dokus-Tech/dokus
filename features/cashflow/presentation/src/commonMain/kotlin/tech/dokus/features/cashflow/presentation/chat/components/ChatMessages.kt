package tech.dokus.features.cashflow.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.MessageRole
import tech.dokus.foundation.aura.components.chat.ChatMessageBubble
import tech.dokus.foundation.aura.components.chat.ChatMessageRole
import tech.dokus.foundation.aura.components.chat.ChatSourceCitationList
import tech.dokus.foundation.aura.components.chat.CitationDisplayData
import tech.dokus.foundation.aura.constrains.Constrains
import kotlinx.datetime.LocalDateTime

@Composable
internal fun MessagesList(
    messages: List<ChatMessageDto>,
    expandedCitationIds: Set<String>,
    listState: LazyListState,
    isLargeScreen: Boolean,
    onToggleCitation: (String) -> Unit,
    onDocumentClick: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = if (isLargeScreen) Constrains.Spacing.xLarge else Constrains.Spacing.medium,
            vertical = Constrains.Spacing.medium
        ),
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        items(
            items = messages,
            key = { it.id.toString() }
        ) { message ->
            MessageItem(
                message = message,
                expandedCitationIds = expandedCitationIds,
                onToggleCitation = onToggleCitation,
                onDocumentClick = onDocumentClick
            )
        }
    }
}

@Composable
private fun MessageItem(
    message: ChatMessageDto,
    expandedCitationIds: Set<String>,
    onToggleCitation: (String) -> Unit,
    onDocumentClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        ChatMessageBubble(
            message = message.content,
            role = when (message.role) {
                MessageRole.User -> ChatMessageRole.User
                MessageRole.Assistant -> ChatMessageRole.Assistant
                MessageRole.System -> ChatMessageRole.Assistant
            },
            timestamp = formatTimestamp(message.createdAt)
        )

        val citations = message.citations
        if (message.role == MessageRole.Assistant && !citations.isNullOrEmpty()) {
            val citationDisplayData = citations.map { citation ->
                CitationDisplayData(
                    chunkId = citation.chunkId,
                    documentId = citation.documentId,
                    documentName = citation.documentName,
                    pageNumber = citation.pageNumber,
                    excerpt = citation.excerpt,
                    relevanceScore = citation.relevanceScore
                )
            }

            ChatSourceCitationList(
                citations = citationDisplayData,
                onDocumentClick = onDocumentClick,
                modifier = Modifier.padding(start = Constrains.Spacing.large)
            )
        }
    }
}

private fun formatTimestamp(dateTime: LocalDateTime): String {
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}
