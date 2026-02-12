package tech.dokus.backend.notifications

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.notifications.NotificationPreferencesService
import tech.dokus.database.repository.notifications.NotificationPreferencesRepository
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.UserId
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NotificationPreferencesServiceTest {

    private val preferencesRepository = mockk<NotificationPreferencesRepository>()
    private val service = NotificationPreferencesService(preferencesRepository)
    private val userId = UserId.generate()

    @Test
    fun `list merges defaults with stored overrides`() = runBlocking {
        coEvery { preferencesRepository.listOverrides(userId) } returns Result.success(
            mapOf(
                NotificationType.VatWarning to true,
                NotificationType.PaymentConfirmed to false
            )
        )

        val response = service.list(userId).getOrThrow()
        val vatWarning = response.preferences.first { it.type == NotificationType.VatWarning }
        val paymentConfirmed = response.preferences.first { it.type == NotificationType.PaymentConfirmed }
        val peppolFailed = response.preferences.first { it.type == NotificationType.PeppolSendFailed }

        assertTrue(vatWarning.emailEnabled)
        assertEquals(false, paymentConfirmed.emailEnabled)
        assertEquals(true, peppolFailed.emailEnabled) // locked and required
        assertTrue(peppolFailed.emailLocked)
    }

    @Test
    fun `cannot disable locked notification email`() = runBlocking {
        val result = service.update(
            userId = userId,
            type = NotificationType.PaymentFailed,
            emailEnabled = false
        )

        val error = result.exceptionOrNull()
        assertIs<DokusException.BadRequest>(error)
        coVerify(exactly = 0) { preferencesRepository.setOverride(any(), any(), any()) }
        coVerify(exactly = 0) { preferencesRepository.removeOverride(any(), any()) }
    }

    @Test
    fun `updating to default removes override`() = runBlocking {
        coEvery {
            preferencesRepository.removeOverride(userId, NotificationType.VatWarning)
        } returns Result.success(true)

        val updated = service.update(
            userId = userId,
            type = NotificationType.VatWarning,
            emailEnabled = NotificationType.VatWarning.defaultEmailEnabled
        ).getOrThrow()

        assertEquals(NotificationType.VatWarning.defaultEmailEnabled, updated.emailEnabled)
        coVerify(exactly = 1) { preferencesRepository.removeOverride(userId, NotificationType.VatWarning) }
        coVerify(exactly = 0) { preferencesRepository.setOverride(any(), any(), any()) }
    }

    @Test
    fun `updating away from default stores override`() = runBlocking {
        coEvery {
            preferencesRepository.setOverride(userId, NotificationType.VatWarning, true)
        } returns Result.success(Unit)

        val updated = service.update(
            userId = userId,
            type = NotificationType.VatWarning,
            emailEnabled = true
        ).getOrThrow()

        assertTrue(updated.emailEnabled)
        coVerify(exactly = 1) { preferencesRepository.setOverride(userId, NotificationType.VatWarning, true) }
        coVerify(exactly = 0) { preferencesRepository.removeOverride(any(), any()) }
    }
}

