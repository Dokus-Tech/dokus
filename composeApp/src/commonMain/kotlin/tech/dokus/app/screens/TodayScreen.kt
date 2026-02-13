package tech.dokus.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Briefcase
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.MessageCircle
import compose.icons.feathericons.User
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.viewmodel.TodayAction
import tech.dokus.app.viewmodel.TodayContainer
import tech.dokus.app.viewmodel.TodayIntent
import tech.dokus.app.viewmodel.TodayState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.cashflow.presentation.cashflow.components.PendingDocumentsCard
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.components.layout.DokusPanelListItem
import tech.dokus.foundation.aura.components.layout.DokusTabbedPanel
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize

/**
 * Today screen using FlowMVI Container pattern.
 * Displays pending documents and notifications.
 */
@Composable
internal fun TodayScreen(
    container: TodayContainer = container()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    val isLargeScreen = LocalScreenSize.current.isLarge

    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is TodayAction.NavigateToDocument -> {
                // TODO: Navigate to document edit/confirmation screen
            }
            is TodayAction.ShowError -> {
                pendingError = action.error
            }
        }
    }

    // Extract state data
    val contentState = state as? TodayState.Content
    val pendingDocumentsState = contentState?.pendingDocumentsState

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        // Mobile today content
        if (!isLargeScreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pending documents card - always show (displays empty/error state when needed)
                pendingDocumentsState?.let { docsState ->
                    PendingDocumentsCard(
                        state = docsState,
                        onDocumentClick = { /* TODO: Navigate to document edit/confirmation screen */ },
                        onLoadMore = { container.store.intent(TodayIntent.LoadMorePendingDocuments) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Other today widgets can be added here
                TodayNotificationsPanel(modifier = Modifier.fillMaxWidth())
            }
        } else {
            // Desktop today content (pending documents shown in Cashflow screen)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(Constrains.Spacing.xLarge),
                verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xLarge)
            ) {
                TodayNotificationsPanel(
                    modifier = Modifier.widthIn(max = 420.dp)
                )
            }
        }
    }
}

private data class TodayNotification(
    val title: String,
    val category: String,
    val timeLabel: String,
    val icon: ImageVector,
)

@Composable
private fun TodayNotificationsPanel(
    modifier: Modifier = Modifier
) {
    val tabs = listOf("All", "New", "Mailroom", "Agent", "Accounting")
    var selectedTab by rememberSaveable { mutableStateOf(tabs.first()) }
    val notifications = remember {
        listOf(
            TodayNotification(
                title = "Inland revenue service (IRS)",
                category = "Mailroom",
                timeLabel = "Just now",
                icon = FeatherIcons.Briefcase
            ),
            TodayNotification(
                title = "Year end bookkeeping report ready for review",
                category = "Accounting",
                timeLabel = "2 days ago",
                icon = FeatherIcons.CheckCircle
            ),
            TodayNotification(
                title = "Foreign qualification in California was successfully filed",
                category = "Agent",
                timeLabel = "2 days ago",
                icon = FeatherIcons.User
            ),
            TodayNotification(
                title = "Foreign qualification in New York was successfully filed",
                category = "Agent",
                timeLabel = "2 days ago",
                icon = FeatherIcons.MessageCircle
            )
        )
    }
    val visibleNotifications = if (selectedTab == tabs.first()) {
        notifications
    } else {
        notifications.filter { it.category == selectedTab }
    }

    DokusTabbedPanel(
        title = "Your notifications",
        tabs = tabs,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        tabLabel = { it },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
        ) {
            if (visibleNotifications.isEmpty()) {
                Text(
                    text = "No notifications yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                visibleNotifications.forEach { item ->
                    DokusPanelListItem(
                        title = item.title,
                        supportingText = "${item.category} - ${item.timeLabel}",
                        leading = { NotificationLeadingIcon(icon = item.icon) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationLeadingIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}
