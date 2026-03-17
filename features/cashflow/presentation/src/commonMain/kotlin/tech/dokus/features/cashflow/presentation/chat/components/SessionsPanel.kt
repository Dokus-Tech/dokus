package tech.dokus.features.cashflow.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_new_conversation
import tech.dokus.aura.resources.chat_scope_all
import tech.dokus.aura.resources.chat_scope_doc
import tech.dokus.aura.resources.chat_session_fallback
import tech.dokus.aura.resources.chat_sessions_label
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.ChatSessionSummary
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlinx.datetime.LocalDateTime

private val PanelWidth = 200.dp
private val ActiveBorderWidth = 2.dp

/**
 * Collapsible sessions panel for the Intelligence chat screen.
 * Shows list of previous chat sessions with title, date, and scope badge.
 */
@Composable
internal fun SessionsPanel(
    sessions: List<ChatSessionSummary>,
    activeSessionId: ChatSessionId?,
    onSessionClick: (ChatSessionId) -> Unit,
    onNewConversation: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(PanelWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
            .border(
                width = Constraints.Stroke.thin,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.chat_sessions_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.textMuted,
                letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
            )
            TextButton(onClick = onCollapse) {
                Text(
                    text = "\u25C2",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }

        // New conversation button
        PPrimaryButton(
            text = stringResource(Res.string.chat_new_conversation),
            onClick = onNewConversation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Constraints.Spacing.medium)
                .padding(bottom = Constraints.Spacing.small),
        )

        // Session list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            sessions.forEach { session ->
                val isActive = session.sessionId == activeSessionId
                SessionItem(
                    session = session,
                    isActive = isActive,
                    onClick = { onSessionClick(session.sessionId) },
                )
            }
        }
    }
}

@Composable
private fun SessionItem(
    session: ChatSessionSummary,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
            )
            .border(
                width = if (isActive) ActiveBorderWidth else 0.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                shape = RoundedCornerShape(0.dp),
            )
            .padding(
                start = if (isActive) Constraints.Spacing.medium - ActiveBorderWidth else Constraints.Spacing.medium,
                end = Constraints.Spacing.medium,
                top = Constraints.Spacing.small,
                bottom = Constraints.Spacing.small,
            ),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title ?: session.lastMessagePreview ?: stringResource(Res.string.chat_session_fallback),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Constraints.Spacing.xxSmall),
            ) {
                Text(
                    text = session.lastMessageAt.date.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
                ScopeBadge(scope = session.scope)
            }
        }
    }
}

@Composable
private fun ScopeBadge(scope: ChatScope) {
    val isAll = scope == ChatScope.AllDocs
    Text(
        text = if (isAll) stringResource(Res.string.chat_scope_all) else stringResource(Res.string.chat_scope_doc),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = if (isAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.textMuted,
        modifier = Modifier
            .background(
                color = if (isAll) MaterialTheme.colorScheme.amberSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(3.dp),
            )
            .padding(horizontal = Constraints.Spacing.xSmall, vertical = Constraints.Spacing.xxSmall),
    )
}

@Preview
@Composable
private fun SessionsPanelPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val fixedDate = LocalDateTime(2026, 3, 16, 10, 0)
    val sessions = listOf(
        ChatSessionSummary(
            sessionId = ChatSessionId.parse("00000000-0000-0000-0000-000000000001"),
            scope = ChatScope.AllDocs,
            title = "Q4 expense analysis",
            messageCount = 8,
            createdAt = fixedDate,
            lastMessageAt = fixedDate,
        ),
        ChatSessionSummary(
            sessionId = ChatSessionId.parse("00000000-0000-0000-0000-000000000002"),
            scope = ChatScope.SingleDoc,
            title = "Tesla invoices question",
            messageCount = 4,
            createdAt = fixedDate,
            lastMessageAt = fixedDate,
        ),
        ChatSessionSummary(
            sessionId = ChatSessionId.parse("00000000-0000-0000-0000-000000000003"),
            scope = ChatScope.AllDocs,
            title = "Cash position forecast",
            messageCount = 6,
            createdAt = fixedDate,
            lastMessageAt = fixedDate,
        ),
    )
    TestWrapper(parameters) {
        SessionsPanel(
            sessions = sessions,
            activeSessionId = sessions[0].sessionId,
            onSessionClick = {},
            onNewConversation = {},
            onCollapse = {},
        )
    }
}
