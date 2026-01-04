package tech.dokus.features.cashflow.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.chat_general_chat
import tech.dokus.aura.resources.chat_history_empty
import tech.dokus.aura.resources.chat_history_title
import tech.dokus.aura.resources.chat_message_count_plural
import tech.dokus.aura.resources.chat_message_count_single
import tech.dokus.aura.resources.chat_new_chat
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.ChatSessionSummary
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.constrains.Constrains

@Composable
internal fun SessionPickerDialog(
    sessions: List<ChatSessionSummary>,
    onSessionSelect: (ChatSessionId) -> Unit,
    onNewSession: () -> Unit,
    onDismiss: () -> Unit,
) {
    DokusDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.chat_history_title),
        scrollableContent = false, // LazyColumn manages its own scrolling
        content = {
            if (sessions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(Constrains.Spacing.medium))
                    Text(
                        text = stringResource(Res.string.chat_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
                ) {
                    items(sessions) { session ->
                        SessionListItem(
                            session = session,
                            onClick = { onSessionSelect(session.sessionId) }
                        )
                    }
                }
            }
        },
        primaryAction = DokusDialogAction(
            text = stringResource(Res.string.chat_new_chat),
            onClick = onNewSession
        ),
        secondaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_cancel),
            onClick = onDismiss
        )
    )
}

@Composable
private fun SessionListItem(
    session: ChatSessionSummary,
    onClick: () -> Unit,
) {
    val messageCountText = if (session.messageCount == 1) {
        stringResource(Res.string.chat_message_count_single, session.messageCount)
    } else {
        stringResource(Res.string.chat_message_count_plural, session.messageCount)
    }

    DokusCardSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(Constrains.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.documentName ?: stringResource(Res.string.chat_general_chat),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = messageCountText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val preview = session.lastMessagePreview
            if (preview != null) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
