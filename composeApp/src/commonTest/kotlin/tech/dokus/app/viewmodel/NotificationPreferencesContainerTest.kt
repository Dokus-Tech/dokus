package tech.dokus.app.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.app.notifications.NotificationRemoteDataSource
import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.ids.NotificationId
import tech.dokus.domain.model.NotificationDto
import tech.dokus.domain.model.NotificationPreferenceDto
import tech.dokus.domain.model.NotificationPreferencesResponse
import tech.dokus.domain.model.common.PaginatedResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPreferencesContainerTest {

    @Test
    fun `load preferences maps defaults and overrides`() = runTest {
        val testScope = this
        val remoteDataSource = FakePreferencesRemoteDataSource(
            initialPreferences = listOf(
                NotificationPreferenceDto(
                    type = NotificationType.PeppolReceived,
                    emailEnabled = false
                ),
                NotificationPreferenceDto(
                    type = NotificationType.PaymentFailed,
                    emailEnabled = true,
                    emailLocked = true
                )
            )
        )
        val container = NotificationPreferencesContainer(remoteDataSource)

        container.store.subscribeAndTest {
            testScope.advanceUntilIdle()

            val content = assertIs<NotificationPreferencesState.Content>(states.value)
            assertEquals(false, content.preferenceFor(NotificationType.PeppolReceived).emailEnabled)
            assertEquals(true, content.preferenceFor(NotificationType.PaymentFailed).emailEnabled)
            assertTrue(content.preferenceFor(NotificationType.PaymentFailed).emailLocked)
        }
    }

    @Test
    fun `toggle preference updates state immediately and persists`() = runTest {
        val testScope = this
        val remoteDataSource = FakePreferencesRemoteDataSource(
            initialPreferences = NotificationType.entries.map { type ->
                NotificationPreferenceDto(
                    type = type,
                    category = type.category,
                    emailEnabled = type.defaultEmailEnabled,
                    emailLocked = type.emailLocked
                )
            }
        )
        val container = NotificationPreferencesContainer(remoteDataSource)

        container.store.subscribeAndTest {
            testScope.advanceUntilIdle()

            emit(
                NotificationPreferencesIntent.ToggleEmail(
                    type = NotificationType.VatWarning,
                    enabled = true
                )
            )
            testScope.advanceUntilIdle()

            val content = assertIs<NotificationPreferencesState.Content>(states.value)
            assertEquals(true, content.preferenceFor(NotificationType.VatWarning).emailEnabled)
            assertEquals(NotificationType.VatWarning, remoteDataSource.lastUpdatedType)
            assertEquals(true, remoteDataSource.lastUpdatedEnabled)
        }
    }
}

private class FakePreferencesRemoteDataSource(
    initialPreferences: List<NotificationPreferenceDto>
) : NotificationRemoteDataSource {
    private val preferencesByType = initialPreferences.associateBy { it.type }.toMutableMap()

    var lastUpdatedType: NotificationType? = null
    var lastUpdatedEnabled: Boolean? = null

    override suspend fun listNotifications(
        type: NotificationType?,
        category: NotificationCategory?,
        isRead: Boolean?,
        limit: Int,
        offset: Int
    ): Result<PaginatedResponse<NotificationDto>> {
        return Result.success(
            PaginatedResponse(
                items = emptyList(),
                total = 0L,
                limit = limit,
                offset = offset
            )
        )
    }

    override suspend fun unreadCount(): Result<Int> = Result.success(0)

    override suspend fun markRead(notificationId: NotificationId): Result<Unit> = Result.success(Unit)

    override suspend fun markAllRead(): Result<Int> = Result.success(0)

    override suspend fun getPreferences(): Result<NotificationPreferencesResponse> {
        return Result.success(
            NotificationPreferencesResponse(preferencesByType.values.toList())
        )
    }

    override suspend fun updatePreference(
        type: NotificationType,
        emailEnabled: Boolean
    ): Result<NotificationPreferenceDto> {
        lastUpdatedType = type
        lastUpdatedEnabled = emailEnabled

        val updated = NotificationPreferenceDto(
            type = type,
            category = type.category,
            emailEnabled = if (type.emailLocked) true else emailEnabled,
            emailLocked = type.emailLocked
        )
        preferencesByType[type] = updated
        return Result.success(updated)
    }
}

