@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.features.cashflow.presentation.chat.components

import kotlin.uuid.ExperimentalUuidApi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.MessageRole
import tech.dokus.foundation.aura.components.chat.ChatAssistantMessage
import tech.dokus.foundation.aura.components.chat.ChatUserBubble
import tech.dokus.foundation.aura.constrains.Constraints

/**
 * Message list for the Intelligence chat screen.
 * Renders user bubbles and assistant messages with v29 styling.
 *
 * For M1: plain text only. Structured blocks (summary, documents, etc.) will be added in M2.
 */
@Composable
internal fun IntelligenceMessages(
    messages: List<ChatMessageDto>,
    listState: LazyListState,
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
                    ChatAssistantMessage {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                        )
                    }
                }
                MessageRole.System -> {
                    // System messages not rendered in v29 design
                }
            }
        }
    }
}
