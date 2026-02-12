package tech.dokus.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.screens.settings.components.formatRelativeTime
import tech.dokus.app.viewmodel.NotificationFilterTab
import tech.dokus.app.viewmodel.TodayAction
import tech.dokus.app.viewmodel.TodayContainer
import tech.dokus.app.viewmodel.TodayIntent
import tech.dokus.app.viewmodel.TodayState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_search
import tech.dokus.aura.resources.search_placeholder
import tech.dokus.aura.resources.settings_select_workspace
import tech.dokus.aura.resources.settings_switch_workspace
import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.NotificationDto
import tech.dokus.features.cashflow.presentation.cashflow.components.PendingDocumentsCard
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.AvatarShape
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact
import tech.dokus.foundation.aura.components.common.PTopAppBarSearchAction
import tech.dokus.foundation.aura.components.filter.DokusFilterToggle
import tech.dokus.foundation.aura.components.filter.DokusFilterToggleRow
import tech.dokus.foundation.aura.components.navigation.UserPreferencesMenu
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.textMuted
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
    var searchQuery by remember { mutableStateOf("") }
    val isLargeScreen = LocalScreenSize.current.isLarge
    var isSearchExpanded by rememberSaveable { mutableStateOf(isLargeScreen) }
    val searchExpanded = isLargeScreen || isSearchExpanded

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
                navController.navigateTo(CashFlowDestination.DocumentReview(action.documentId))
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

    LaunchedEffect(isLargeScreen) {
        isSearchExpanded = isLargeScreen
    }

    val contentState = state as? TodayState.Content
    val currentTenant = contentState?.tenantState?.let { if (it.isSuccess()) it.data else null }
    val currentAvatar = contentState?.currentAvatar
    val pendingDocumentsState = contentState?.pendingDocumentsState
    val notificationsState: DokusState<List<NotificationDto>> =
        contentState?.notificationsState ?: DokusState.idle()
    val unreadNotificationCount = contentState?.unreadNotificationCount ?: 0
    val notificationFilter = contentState?.notificationFilter ?: NotificationFilterTab.All

    Scaffold(
        topBar = {
            PTopAppBarSearchAction(
                searchContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isLargeScreen && !searchExpanded) {
                            IconButton(
                                onClick = { isSearchExpanded = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.Search,
                                    contentDescription = stringResource(Res.string.action_search)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = searchExpanded,
                            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                        ) {
                            PSearchFieldCompact(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = stringResource(Res.string.search_placeholder),
                                modifier = if (isLargeScreen) Modifier else Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { navController.navigateTo(AuthDestination.WorkspaceSelect) },
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        CompanyAvatarImage(
                            avatarUrl = currentAvatar?.small,
                            initial = currentTenant?.displayName?.value?.take(1) ?: "D",
                            size = AvatarSize.ExtraSmall,
                            shape = AvatarShape.RoundedSquare,
                            onClick = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = currentTenant?.displayName?.value
                                ?: stringResource(Res.string.settings_select_workspace),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.SwitchAccount,
                            contentDescription = stringResource(Res.string.settings_switch_workspace),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    NotificationBellDropdown(
                        unreadCount = unreadNotificationCount,
                        filter = notificationFilter,
                        notificationsState = notificationsState,
                        onFilterSelected = { filter ->
                            container.store.intent(TodayIntent.LoadNotifications(filter))
                        },
                        onNotificationClick = { notification ->
                            container.store.intent(TodayIntent.OpenNotification(notification))
                        },
                        onMarkAllAsRead = {
                            container.store.intent(TodayIntent.MarkAllNotificationsRead)
                        }
                    )

                    Spacer(Modifier.width(8.dp))
                    UserPreferencesMenu()
                }
            )
        },
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
                // Today desktop screen keeps focus on quick header actions.
            }
        }
    }
}

@Composable
private fun NotificationBellDropdown(
    unreadCount: Int,
    filter: NotificationFilterTab,
    notificationsState: DokusState<List<NotificationDto>>,
    onFilterSelected: (NotificationFilterTab) -> Unit,
    onNotificationClick: (NotificationDto) -> Unit,
    onMarkAllAsRead: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = !expanded }) {
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge {
                            Text(text = unreadCount.toString())
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications"
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 360.dp, max = 420.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleMedium
                )

                DokusFilterToggleRow {
                    NotificationFilterTab.entries.forEach { tab ->
                        DokusFilterToggle(
                            selected = tab == filter,
                            onClick = { onFilterSelected(tab) },
                            label = tab.label
                        )
                    }
                }

                HorizontalDivider()

                when (notificationsState) {
                    is DokusState.Loading -> {
                        Text(
                            text = "Loading notifications...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    is DokusState.Success -> {
                        val items = notificationsState.data
                        if (items.isEmpty()) {
                            Text(
                                text = "Nothing needs your attention.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textMuted,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items.forEach { item ->
                                    NotificationListItem(
                                        notification = item,
                                        onClick = {
                                            expanded = false
                                            onNotificationClick(item)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is DokusState.Error -> {
                        Text(
                            text = notificationsState.exception.localized,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    is DokusState.Idle -> {
                        Text(
                            text = "Nothing needs your attention.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                HorizontalDivider()

                TextButton(
                    onClick = {
                        onMarkAllAsRead()
                        expanded = false
                    },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Mark all as read")
                }
            }
        }
    }
}

@Composable
private fun NotificationListItem(
    notification: NotificationDto,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(width = 4.dp, height = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }

        NotificationIcon(notification.type)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                text = "${categoryLabel(notification.category)} - ${formatRelativeTime(notification.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NotificationIcon(type: NotificationType) {
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

    val icon = if (critical) Icons.Default.Warning else Icons.Default.Notifications
    val tint = if (critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(18.dp)
    )
}

private fun categoryLabel(category: NotificationCategory): String = when (category) {
    NotificationCategory.Peppol -> "PEPPOL"
    NotificationCategory.Compliance -> "Compliance"
    NotificationCategory.Billing -> "Billing"
}
