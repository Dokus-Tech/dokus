package tech.dokus.app.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.app.notifications.InvoiceLookupDataSource
import tech.dokus.app.notifications.NotificationRemoteDataSource
import tech.dokus.domain.Email
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.enums.NotificationReferenceType
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.InvoiceNumber
import tech.dokus.domain.ids.NotificationId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.NotificationDto
import tech.dokus.domain.model.NotificationPreferenceDto
import tech.dokus.domain.model.NotificationPreferencesResponse
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.UnreadCountResponse
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.cashflow.usecases.WatchPendingDocumentsUseCase
import tech.dokus.foundation.app.state.DokusState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class TodayContainerTest {

    @Test
    fun `open invoice notification marks read and navigates to document review`() = runTest {
        val testScope = this
        val tenantId = TenantId.generate()
        val userId = UserId.generate()
        val invoiceId = InvoiceId.generate()
        val documentId = DocumentId.generate()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val notification = NotificationDto(
            id = NotificationId.generate(),
            tenantId = tenantId,
            userId = userId,
            type = NotificationType.PeppolSendFailed,
            category = NotificationCategory.Peppol,
            title = "PEPPOL send failed - Inv #2026-001",
            referenceType = NotificationReferenceType.Invoice,
            referenceId = invoiceId.toString(),
            isRead = false,
            createdAt = now,
            emailSent = false
        )

        val notifications = mutableListOf(notification)
        val notificationDataSource = FakeNotificationRemoteDataSource(notifications)
        val invoiceLookup = FakeInvoiceLookupDataSource(
            invoicesById = mapOf(invoiceId to testInvoice(invoiceId, tenantId, documentId))
        )

        val container = TodayContainer(
            getCurrentTenantUseCase = FakeGetCurrentTenantUseCase(),
            watchPendingDocuments = FakeWatchPendingDocumentsUseCase(),
            notificationRemoteDataSource = notificationDataSource,
            invoiceLookupDataSource = invoiceLookup,
            unreadPollingInterval = Duration.ZERO
        )

        container.store.subscribeAndTest {
            testScope.advanceUntilIdle()

            TodayIntent.OpenNotification(notification) resultsIn TodayAction.NavigateToDocument(documentId.toString())
            testScope.advanceUntilIdle()

            val content = assertIs<TodayState.Content>(states.value)
            assertEquals(0, content.unreadNotificationCount)
            assertEquals(1, notificationDataSource.markReadCalls)
        }
    }

    @Test
    fun `mark all notifications read updates unread badge count`() = runTest {
        val testScope = this
        val tenantId = TenantId.generate()
        val userId = UserId.generate()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val notifications = mutableListOf(
            NotificationDto(
                id = NotificationId.generate(),
                tenantId = tenantId,
                userId = userId,
                type = NotificationType.PeppolReceived,
                category = NotificationCategory.Peppol,
                title = "New PEPPOL document received",
                referenceType = NotificationReferenceType.Document,
                referenceId = DocumentId.generate().toString(),
                isRead = false,
                createdAt = now,
                emailSent = false
            ),
            NotificationDto(
                id = NotificationId.generate(),
                tenantId = tenantId,
                userId = userId,
                type = NotificationType.VatWarning,
                category = NotificationCategory.Compliance,
                title = "VAT field incomplete",
                referenceType = NotificationReferenceType.Document,
                referenceId = DocumentId.generate().toString(),
                isRead = false,
                createdAt = now,
                emailSent = false
            )
        )
        val notificationDataSource = FakeNotificationRemoteDataSource(notifications)

        val container = TodayContainer(
            getCurrentTenantUseCase = FakeGetCurrentTenantUseCase(),
            watchPendingDocuments = FakeWatchPendingDocumentsUseCase(),
            notificationRemoteDataSource = notificationDataSource,
            invoiceLookupDataSource = FakeInvoiceLookupDataSource(),
            unreadPollingInterval = Duration.ZERO
        )

        container.store.subscribeAndTest {
            testScope.advanceUntilIdle()
            val initial = assertIs<TodayState.Content>(states.value)
            assertEquals(2, initial.unreadNotificationCount)

            emit(TodayIntent.MarkAllNotificationsRead)
            testScope.advanceUntilIdle()

            val updated = assertIs<TodayState.Content>(states.value)
            assertEquals(0, updated.unreadNotificationCount)
            assertEquals(1, notificationDataSource.markAllCalls)
        }
    }
}

private class FakeGetCurrentTenantUseCase : GetCurrentTenantUseCase {
    override suspend fun invoke(): Result<Tenant?> = Result.success(null)
}

private class FakeWatchPendingDocumentsUseCase : WatchPendingDocumentsUseCase {
    override fun invoke(limit: Int): Flow<DokusState<List<tech.dokus.domain.model.DocumentRecordDto>>> {
        return flowOf(DokusState.success(emptyList()))
    }

    override fun refresh() = Unit
}

private class FakeInvoiceLookupDataSource(
    private val invoicesById: Map<InvoiceId, FinancialDocumentDto.InvoiceDto> = emptyMap()
) : InvoiceLookupDataSource {
    override suspend fun getInvoice(id: InvoiceId): Result<FinancialDocumentDto.InvoiceDto> {
        return invoicesById[id]?.let { Result.success(it) } ?: Result.failure(
            IllegalStateException("Invoice not found in fake datasource")
        )
    }
}

private class FakeNotificationRemoteDataSource(
    private val notifications: MutableList<NotificationDto>
) : NotificationRemoteDataSource {
    var markReadCalls: Int = 0
    var markAllCalls: Int = 0

    override suspend fun listNotifications(
        type: NotificationType?,
        category: NotificationCategory?,
        isRead: Boolean?,
        limit: Int,
        offset: Int
    ): Result<PaginatedResponse<NotificationDto>> {
        val filtered = notifications.filter { item ->
            (type == null || item.type == type) &&
                (category == null || item.category == category) &&
                (isRead == null || item.isRead == isRead)
        }
        return Result.success(
            PaginatedResponse(
                items = filtered.drop(offset).take(limit),
                total = filtered.size.toLong(),
                limit = limit,
                offset = offset
            )
        )
    }

    override suspend fun unreadCount(): Result<Int> {
        return Result.success(notifications.count { !it.isRead })
    }

    override suspend fun markRead(notificationId: NotificationId): Result<Unit> {
        markReadCalls += 1
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index >= 0) {
            notifications[index] = notifications[index].copy(isRead = true)
        }
        return Result.success(Unit)
    }

    override suspend fun markAllRead(): Result<Int> {
        markAllCalls += 1
        val unread = notifications.count { !it.isRead }
        notifications.replaceAll { it.copy(isRead = true) }
        return Result.success(unread)
    }

    override suspend fun getPreferences(): Result<NotificationPreferencesResponse> {
        return Result.success(
            NotificationPreferencesResponse(
                preferences = NotificationType.entries.map {
                    NotificationPreferenceDto(
                        type = it,
                        emailEnabled = it.defaultEmailEnabled,
                        emailLocked = it.emailLocked
                    )
                }
            )
        )
    }

    override suspend fun updatePreference(
        type: NotificationType,
        emailEnabled: Boolean
    ): Result<NotificationPreferenceDto> {
        return Result.success(
            NotificationPreferenceDto(
                type = type,
                emailEnabled = if (type.emailLocked) true else emailEnabled,
                emailLocked = type.emailLocked
            )
        )
    }
}

private fun testInvoice(
    invoiceId: InvoiceId,
    tenantId: TenantId,
    documentId: DocumentId
): FinancialDocumentDto.InvoiceDto {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    return FinancialDocumentDto.InvoiceDto(
        id = invoiceId,
        tenantId = tenantId,
        direction = DocumentDirection.Outbound,
        contactId = ContactId.generate(),
        invoiceNumber = InvoiceNumber("INV-2026-001"),
        issueDate = LocalDate(2026, 1, 1),
        dueDate = LocalDate(2026, 1, 31),
        subtotalAmount = Money(10_000),
        vatAmount = Money(2_100),
        totalAmount = Money(12_100),
        status = InvoiceStatus.Sent,
        documentId = documentId,
        createdAt = now,
        updatedAt = now
    )
}
