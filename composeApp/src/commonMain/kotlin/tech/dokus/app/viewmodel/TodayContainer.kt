package tech.dokus.app.viewmodel

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.app.notifications.InvoiceLookupDataSource
import tech.dokus.app.notifications.NotificationRemoteDataSource
import tech.dokus.domain.enums.NotificationReferenceType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.cashflow.usecases.WatchPendingDocumentsUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal typealias TodayCtx = PipelineContext<TodayState, TodayIntent, TodayAction>

/**
 * Container for Today screen using FlowMVI.
 *
 * Manages:
 * - Current tenant information
 * - Pending documents list
 * - Notification badge, filters, and dropdown content
 */
internal class TodayContainer(
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase,
    private val watchPendingDocuments: WatchPendingDocumentsUseCase,
    private val notificationRemoteDataSource: NotificationRemoteDataSource,
    private val invoiceLookupDataSource: InvoiceLookupDataSource,
    private val unreadPollingInterval: Duration = 30.seconds,
) : Container<TodayState, TodayIntent, TodayAction> {

    private val logger = Logger.forClass<TodayContainer>()

    // Internal state for pending documents pagination
    private val allPendingDocuments = MutableStateFlow<List<DocumentRecordDto>>(emptyList())
    private val pendingVisibleCount = MutableStateFlow(TodayState.PENDING_PAGE_SIZE)

    override val store: Store<TodayState, TodayIntent, TodayAction> =
        store(TodayState.Content()) {
            init {
                launchWatchPendingDocuments()
                if (unreadPollingInterval > Duration.ZERO) {
                    launchUnreadCountPolling()
                }
                launch {
                    intent(TodayIntent.LoadNotifications())
                    intent(TodayIntent.RefreshUnreadNotifications)
                }
            }

            reduce { intent ->
                when (intent) {
                    is TodayIntent.RefreshTenant -> handleRefreshTenant()
                    is TodayIntent.RefreshPendingDocuments -> handleRefreshPendingDocuments()
                    is TodayIntent.LoadMorePendingDocuments -> handleLoadMorePendingDocuments()
                    is TodayIntent.LoadNotifications -> handleLoadNotifications(intent.filter)
                    is TodayIntent.RefreshUnreadNotifications -> handleRefreshUnreadNotifications()
                    is TodayIntent.OpenNotification -> handleOpenNotification(intent.notification)
                    is TodayIntent.MarkAllNotificationsRead -> handleMarkAllNotificationsRead()
                }
            }
        }

    private suspend fun TodayCtx.launchWatchPendingDocuments() {
        launch {
            watchPendingDocuments().collectLatest { state ->
                when (state) {
                    is DokusState.Loading -> {
                        withState<TodayState.Content, _> {
                            updateState { copy(pendingDocumentsState = DokusState.loading()) }
                        }
                    }

                    is DokusState.Success -> {
                        allPendingDocuments.value = state.data
                        pendingVisibleCount.value = TodayState.PENDING_PAGE_SIZE
                        updatePendingPaginationState()
                    }

                    is DokusState.Error -> {
                        allPendingDocuments.value = emptyList()
                        withState<TodayState.Content, _> {
                            updateState {
                                copy(
                                    pendingDocumentsState = DokusState.error(
                                        state.exception,
                                        state.retryHandler
                                    )
                                )
                            }
                        }
                    }

                    is DokusState.Idle -> {
                        withState<TodayState.Content, _> {
                            updateState { copy(pendingDocumentsState = DokusState.idle()) }
                        }
                    }
                }
            }
        }
    }

    private suspend fun TodayCtx.launchUnreadCountPolling() {
        launch {
            while (true) {
                intent(TodayIntent.RefreshUnreadNotifications)
                delay(unreadPollingInterval)
            }
        }
    }

    private suspend fun TodayCtx.handleRefreshTenant() {
        withState<TodayState.Content, _> {
            logger.d { "Refreshing tenant" }

            updateState { copy(tenantState = DokusState.loading()) }

            getCurrentTenantUseCase().fold(
                onSuccess = { tenant ->
                    logger.d { "Tenant loaded: ${tenant?.displayName}" }
                    updateState {
                        copy(
                            tenantState = DokusState.success(tenant),
                            currentAvatar = tenant?.avatar
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load tenant" }
                    updateState {
                        copy(
                            tenantState = DokusState.error(error) {
                                intent(TodayIntent.RefreshTenant)
                            }
                        )
                    }
                }
            )
        }
    }

    private fun handleRefreshPendingDocuments() {
        logger.d { "Refreshing pending documents" }
        watchPendingDocuments.refresh()
    }

    private suspend fun TodayCtx.handleLoadMorePendingDocuments() {
        val allDocs = allPendingDocuments.value
        val currentVisible = pendingVisibleCount.value

        if (currentVisible >= allDocs.size) return

        logger.d { "Loading more pending documents" }

        pendingVisibleCount.value = (currentVisible + TodayState.PENDING_PAGE_SIZE)
            .coerceAtMost(allDocs.size)

        updatePendingPaginationState()
    }

    private suspend fun TodayCtx.updatePendingPaginationState() {
        val allDocs = allPendingDocuments.value
        val visibleCount = pendingVisibleCount.value
        val visibleDocs = allDocs.take(visibleCount)
        val hasMore = visibleCount < allDocs.size

        val paginationState = tech.dokus.domain.model.common.PaginationState(
            data = visibleDocs,
            currentPage = visibleCount / TodayState.PENDING_PAGE_SIZE,
            pageSize = TodayState.PENDING_PAGE_SIZE,
            hasMorePages = hasMore,
            isLoadingMore = false
        )

        withState<TodayState.Content, _> {
            updateState {
                copy(pendingDocumentsState = DokusState.success(paginationState))
            }
        }
    }

    private suspend fun TodayCtx.handleLoadNotifications(filter: NotificationFilterTab) {
        withState<TodayState.Content, _> {
            updateState {
                copy(
                    notificationFilter = filter,
                    notificationsState = DokusState.loading()
                )
            }
        }

        notificationRemoteDataSource.listNotifications(
            category = filter.category,
            isRead = if (filter.unreadOnly) false else null,
            limit = 20,
            offset = 0
        ).fold(
            onSuccess = { page ->
                withState<TodayState.Content, _> {
                    updateState {
                        copy(notificationsState = DokusState.success(page.items))
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load notifications" }
                withState<TodayState.Content, _> {
                    updateState {
                        copy(
                            notificationsState = DokusState.error(error) {
                                intent(TodayIntent.LoadNotifications(filter))
                            }
                        )
                    }
                }
            }
        )
    }

    private suspend fun TodayCtx.handleRefreshUnreadNotifications() {
        notificationRemoteDataSource.unreadCount().fold(
            onSuccess = { count ->
                withState<TodayState.Content, _> {
                    updateState { copy(unreadNotificationCount = count) }
                }
            },
            onFailure = { error ->
                logger.w(error) { "Failed to refresh unread notifications count" }
            }
        )
    }

    private suspend fun TodayCtx.handleOpenNotification(notification: tech.dokus.domain.model.NotificationDto) {
        var currentFilter = NotificationFilterTab.All
        withState<TodayState.Content, _> {
            currentFilter = notificationFilter
        }

        notificationRemoteDataSource.markRead(notification.id)
            .onFailure { error ->
                logger.w(error) { "Failed to mark notification as read" }
            }

        when (val target = resolveNavigationTarget(notification)) {
            is NotificationNavigationTarget.Document -> action(TodayAction.NavigateToDocument(target.documentId))
            NotificationNavigationTarget.Cashflow -> action(TodayAction.NavigateToCashflow)
            null -> Unit
        }

        intent(TodayIntent.RefreshUnreadNotifications)
        intent(TodayIntent.LoadNotifications(currentFilter))
    }

    private suspend fun TodayCtx.handleMarkAllNotificationsRead() {
        var currentFilter = NotificationFilterTab.All
        withState<TodayState.Content, _> {
            currentFilter = notificationFilter
        }

        notificationRemoteDataSource.markAllRead()
            .onFailure { error ->
                logger.e(error) { "Failed to mark all notifications as read" }
                action(
                    TodayAction.ShowError(
                        error as? DokusException
                            ?: DokusException.InternalError("Failed to mark all as read")
                    )
                )
            }

        intent(TodayIntent.RefreshUnreadNotifications)
        intent(TodayIntent.LoadNotifications(currentFilter))
    }

    private suspend fun resolveNavigationTarget(
        notification: tech.dokus.domain.model.NotificationDto
    ): NotificationNavigationTarget? {
        return when (notification.referenceType) {
            NotificationReferenceType.Document -> NotificationNavigationTarget.Document(notification.referenceId)
            NotificationReferenceType.ComplianceItem -> NotificationNavigationTarget.Document(notification.referenceId)
            NotificationReferenceType.Invoice -> {
                val invoiceId = runCatching { InvoiceId.parse(notification.referenceId) }.getOrNull()
                    ?: return NotificationNavigationTarget.Cashflow
                val documentId = invoiceLookupDataSource.getInvoice(invoiceId)
                    .getOrNull()
                    ?.documentId
                    ?.toString()
                if (documentId != null) {
                    NotificationNavigationTarget.Document(documentId)
                } else {
                    NotificationNavigationTarget.Cashflow
                }
            }

            NotificationReferenceType.Transmission,
            NotificationReferenceType.BillingItem -> null
        }
    }

    private sealed interface NotificationNavigationTarget {
        data class Document(val documentId: String) : NotificationNavigationTarget
        data object Cashflow : NotificationNavigationTarget
    }
}
