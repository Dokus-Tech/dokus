package tech.dokus.app.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.viewmodel.NotificationFilterTab
import tech.dokus.app.viewmodel.TodayAction
import tech.dokus.app.viewmodel.TodayContainer
import tech.dokus.app.viewmodel.TodayIntent
import tech.dokus.app.viewmodel.TodayState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.NotificationDto
import tech.dokus.features.cashflow.presentation.cashflow.components.PendingDocumentsCard
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.state.DokusState
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

    val spacing = if (isLargeScreen) Constrains.Spacing.xLarge else Constrains.Spacing.large
    val contentModifier = if (isLargeScreen) {
        Modifier.widthIn(max = 480.dp)
    } else {
        Modifier.fillMaxWidth()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            if (!isLargeScreen && pendingDocumentsState != null) {
                PendingDocumentsCard(
                    state = pendingDocumentsState,
                    onDocumentClick = { document ->
                        navController.navigateTo(CashFlowDestination.DocumentReview(document.document.id.toString()))
                    },
                    onLoadMore = { container.store.intent(TodayIntent.LoadMorePendingDocuments) },
                    modifier = contentModifier
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
                modifier = contentModifier
            )
        }
    }
}
