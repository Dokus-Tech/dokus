package tech.dokus.app.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.screens.settings.components.formatRelativeTime
import tech.dokus.app.viewmodel.NotificationFilterTab
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.today_notifications_empty
import tech.dokus.aura.resources.today_notifications_filter_all
import tech.dokus.aura.resources.today_notifications_filter_compliance
import tech.dokus.aura.resources.today_notifications_filter_peppol
import tech.dokus.aura.resources.today_notifications_filter_unread
import tech.dokus.aura.resources.today_notifications_loading
import tech.dokus.aura.resources.today_notifications_title
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.model.NotificationDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.layout.DokusPanelListItem
import tech.dokus.foundation.aura.components.layout.DokusTabbedPanel
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.icon
import tech.dokus.foundation.aura.extensions.iconTint
import tech.dokus.foundation.aura.extensions.localized

@Composable
internal fun TodayNotificationsPanel(
    filter: NotificationFilterTab,
    notificationsState: DokusState<List<NotificationDto>>,
    onFilterSelected: (NotificationFilterTab) -> Unit,
    onNotificationClick: (NotificationDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusTabbedPanel(
        title = stringResource(Res.string.today_notifications_title),
        tabs = NotificationFilterTab.entries,
        selectedTab = filter,
        onTabSelected = onFilterSelected,
        tabLabel = { tab -> tab.localized },
        modifier = modifier
    ) {
        when (notificationsState) {
            is DokusState.Loading -> {
                Text(
                    text = stringResource(Res.string.today_notifications_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is DokusState.Success -> {
                val items = notificationsState.data
                if (items.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.today_notifications_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
                    ) {
                        items.forEach { notification ->
                            DokusPanelListItem(
                                title = notification.title,
                                supportingText = "${notification.category.localized} - ${
                                    formatRelativeTime(notification.createdAt)
                                }",
                                leading = { NotificationLeadingIcon(notification.type) },
                                onClick = { onNotificationClick(notification) }
                            )
                        }
                    }
                }
            }

            is DokusState.Error -> {
                Text(
                    text = notificationsState.exception.localized,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is DokusState.Idle -> {
                Text(
                    text = stringResource(Res.string.today_notifications_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NotificationLeadingIcon(type: NotificationType) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            tint = type.iconTint,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Localized label for [NotificationFilterTab].
 * Lives here because NotificationFilterTab is in composeApp, not in aura.
 */
private val NotificationFilterTab.localized: String
    @Composable get() = when (this) {
        NotificationFilterTab.All -> stringResource(Res.string.today_notifications_filter_all)
        NotificationFilterTab.Unread -> stringResource(Res.string.today_notifications_filter_unread)
        NotificationFilterTab.Peppol -> stringResource(Res.string.today_notifications_filter_peppol)
        NotificationFilterTab.Compliance -> stringResource(Res.string.today_notifications_filter_compliance)
    }
