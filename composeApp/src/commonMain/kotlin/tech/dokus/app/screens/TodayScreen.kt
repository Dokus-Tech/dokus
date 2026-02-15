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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Inbox
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.screens.settings.components.formatRelativeTime
import tech.dokus.app.viewmodel.NotificationFilterTab
import tech.dokus.app.viewmodel.TodayAction
import tech.dokus.app.viewmodel.TodayContainer
import tech.dokus.app.viewmodel.TodayIntent
import tech.dokus.app.viewmodel.TodayState
import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.NotificationDto
import tech.dokus.features.cashflow.presentation.cashflow.components.PendingDocumentsCard
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.layout.DokusPanelListItem
import tech.dokus.foundation.aura.components.layout.DokusTabbedPanel
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

/**
 * Today screen using FlowMVI Container pattern.
 */
@Composable
internal fun TodayScreen(
    container: TodayContainer = container()
) {
    val navController = LocalNavController.current
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

    val state by container.store.subscribe { action ->
        when (action) {
            is TodayAction.NavigateToDocument -> {
                navController.navigateTo(CashFlowDestination.DocumentReview(action.documentId))
            }

            TodayAction.NavigateToCashflow -> {
                navController.navigateTo(CashFlowDestination.CashflowLedger())
            }

            TodayAction.NavigateToWorkspaceSelect -> {
                navController.navigateTo(AuthDestination.WorkspaceSelect)
            }

            is TodayAction.ShowError -> {
                pendingError = action.error
            }
        }
    }

    LaunchedEffect(Unit) {
        container.store.intent(TodayIntent.RefreshTenant)
    }

    val contentState = state as? TodayState.Content
    val pendingDocumentsState = contentState?.pendingDocumentsState
    val notificationsState: DokusState<List<NotificationDto>> = contentState?.notificationsState ?: DokusState.idle()
    val notificationFilter = contentState?.notificationFilter ?: NotificationFilterTab.All

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        if (!isLargeScreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                pendingDocumentsState?.let { docsState ->
                    PendingDocumentsCard(
                        state = docsState,
                        onDocumentClick = { document ->
                            navController.navigateTo(CashFlowDestination.DocumentReview(document.document.id.toString()))
                        },
                        onLoadMore = { container.store.intent(TodayIntent.LoadMorePendingDocuments) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TodayNotificationsPanel(
                    filter = notificationFilter,
                    notificationsState = notificationsState,
                    onFilterSelected = { filter ->
                        container.store.intent(TodayIntent.LoadNotifications(filter))
                    },
                    onNotificationClick = { notification ->
                        container.store.intent(TodayIntent.OpenNotification(notification))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(Constrains.Spacing.xLarge),
                verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xLarge)
            ) {
                TodayNotificationsPanel(
                    filter = notificationFilter,
                    notificationsState = notificationsState,
                    onFilterSelected = { filter ->
                        container.store.intent(TodayIntent.LoadNotifications(filter))
                    },
                    onNotificationClick = { notification ->
                        container.store.intent(TodayIntent.OpenNotification(notification))
                    },
                    modifier = Modifier.widthIn(max = 480.dp)
                )
            }
        }
    }
}

@Composable
private fun TodayNotificationsPanel(
    filter: NotificationFilterTab,
    notificationsState: DokusState<List<NotificationDto>>,
    onFilterSelected: (NotificationFilterTab) -> Unit,
    onNotificationClick: (NotificationDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusTabbedPanel(
        title = "Your notifications",
        tabs = NotificationFilterTab.entries,
        selectedTab = filter,
        onTabSelected = onFilterSelected,
        tabLabel = { it.label },
        modifier = modifier
    ) {
        when (notificationsState) {
            is DokusState.Loading -> {
                Text(
                    text = "Loading notifications...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is DokusState.Success -> {
                val items = notificationsState.data
                if (items.isEmpty()) {
                    Text(
                        text = "Nothing needs your attention.",
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
                                supportingText = "${categoryLabel(notification.category)} - ${
                                    formatRelativeTime(
                                        notification.createdAt
                                    )
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
                    text = "Nothing needs your attention.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NotificationLeadingIcon(type: NotificationType) {
    val critical = when (type) {
        NotificationType.PeppolSendFailed,
        NotificationType.ComplianceBlocker,
        NotificationType.PaymentFailed -> true

        NotificationType.PeppolReceived,
        NotificationType.PeppolSendConfirmed,
        NotificationType.VatWarning,
        NotificationType.PaymentConfirmed,
        NotificationType.SubscriptionChanged -> false
    }
    val icon = if (critical) Icons.Default.Warning else FeatherIcons.Inbox
    val tint = if (critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun categoryLabel(category: NotificationCategory): String = when (category) {
    NotificationCategory.Peppol -> "PEPPOL"
    NotificationCategory.Compliance -> "Compliance"
    NotificationCategory.Billing -> "Billing"
}
