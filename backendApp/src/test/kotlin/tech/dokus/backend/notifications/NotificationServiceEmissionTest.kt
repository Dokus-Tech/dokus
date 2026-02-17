package tech.dokus.backend.notifications

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.auth.EmailService
import tech.dokus.backend.services.auth.EmailTemplate
import tech.dokus.backend.services.auth.EmailTemplateRenderer
import tech.dokus.backend.services.notifications.NotificationEmission
import tech.dokus.backend.services.notifications.NotificationPreferencesService
import tech.dokus.backend.services.notifications.NotificationService
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.notifications.NotificationRepository
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.enums.NotificationReferenceType
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.NotificationId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.NotificationDto
import tech.dokus.domain.model.User
import tech.dokus.domain.model.UserInTenant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import java.util.concurrent.atomic.AtomicBoolean

class NotificationServiceEmissionTest {

    private val notificationRepository = mockk<NotificationRepository>()
    private val userRepository = mockk<UserRepository>()
    private val preferencesService = mockk<NotificationPreferencesService>()
    private val emailService = mockk<EmailService>()
    private val templateRenderer = mockk<EmailTemplateRenderer>()
    private val testEmailScope = CoroutineScope(Dispatchers.Unconfined)

    private val service = NotificationService(
        notificationRepository = notificationRepository,
        userRepository = userRepository,
        preferencesService = preferencesService,
        emailService = emailService,
        emailTemplateRenderer = templateRenderer,
        emailScope = testEmailScope
    )

    private val tenantId = TenantId.generate()
    private val userId = UserId.generate()

    @Test
    fun `peppol send failed event emits notification and sends email`() = runBlocking {
        val userInTenant = testUserInTenant(userId, tenantId, "peppol.failed@test.dokus")
        coEvery { userRepository.listByTenant(tenantId, true) } returns listOf(userInTenant)
        stubCreateNotification()
        coEvery { preferencesService.isEmailEnabled(userId, NotificationType.PeppolSendFailed) } returns Result.success(true)
        coEvery {
            notificationRepository.hasRecentEmailFor(
                tenantId,
                userId,
                NotificationType.PeppolSendFailed,
                "inv-42"
            )
        } returns Result.success(false)
        coEvery {
            templateRenderer.renderNotification(
                type = NotificationType.PeppolSendFailed,
                title = any(),
                details = any(),
                openPath = any()
            )
        } returns EmailTemplate(
            subject = "PEPPOL send failed",
            htmlBody = "<html/>",
            textBody = "text"
        )
        coEvery { emailService.send(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { notificationRepository.markEmailSent(any(), any(), any()) } returns Result.success(true)

        val emitted = service.emit(
            NotificationEmission(
                tenantId = tenantId,
                type = NotificationType.PeppolSendFailed,
                title = "PEPPOL send failed - Inv #42",
                referenceType = NotificationReferenceType.Invoice,
                referenceId = "inv-42",
                openPath = "/cashflow/document_review/doc-42",
                emailDetails = listOf("Reason: Recipient not found")
            )
        ).getOrThrow()

        assertEquals(1, emitted.size)
        assertEquals(NotificationType.PeppolSendFailed, emitted.single().type)

        coVerify(timeout = 1_000, exactly = 1) { emailService.send("peppol.failed@test.dokus", any(), any(), any()) }
        coVerify(timeout = 1_000, exactly = 1) { notificationRepository.markEmailSent(tenantId, userId, any()) }
    }

    @Test
    fun `peppol send failed dedup window skips duplicate email`() = runBlocking {
        val userInTenant = testUserInTenant(userId, tenantId, "peppol.dup@test.dokus")
        coEvery { userRepository.listByTenant(tenantId, true) } returns listOf(userInTenant)
        stubCreateNotification()
        coEvery { preferencesService.isEmailEnabled(userId, NotificationType.PeppolSendFailed) } returns Result.success(true)
        coEvery {
            notificationRepository.hasRecentEmailFor(
                tenantId,
                userId,
                NotificationType.PeppolSendFailed,
                "inv-100"
            )
        } returns Result.success(true)

        val emitted = service.emit(
            NotificationEmission(
                tenantId = tenantId,
                type = NotificationType.PeppolSendFailed,
                title = "PEPPOL send failed - Inv #100",
                referenceType = NotificationReferenceType.Invoice,
                referenceId = "inv-100",
                openPath = "/cashflow/document_review/doc-100"
            )
        ).getOrThrow()

        assertEquals(1, emitted.size)
        coVerify(exactly = 0) { emailService.send(any(), any(), any(), any()) }
        coVerify(exactly = 0) { notificationRepository.markEmailSent(any(), any(), any()) }
    }

    @Test
    fun `peppol received with email disabled emits in app only`() = runBlocking {
        val userInTenant = testUserInTenant(userId, tenantId, "peppol.received@test.dokus")
        coEvery { userRepository.listByTenant(tenantId, true) } returns listOf(userInTenant)
        stubCreateNotification()
        coEvery { preferencesService.isEmailEnabled(userId, NotificationType.PeppolReceived) } returns Result.success(false)

        val emitted = service.emit(
            NotificationEmission(
                tenantId = tenantId,
                type = NotificationType.PeppolReceived,
                title = "New PEPPOL document received",
                referenceType = NotificationReferenceType.Document,
                referenceId = "doc-88",
                openPath = "/cashflow/document_review/doc-88"
            )
        ).getOrThrow()

        assertEquals(1, emitted.size)
        assertTrue(emitted.single().type == NotificationType.PeppolReceived)
        coVerify(exactly = 0) { emailService.send(any(), any(), any(), any()) }
    }

    @Test
    fun `concurrent emits for same notification key send only one email`() = runBlocking {
        val concurrentService = NotificationService(
            notificationRepository = notificationRepository,
            userRepository = userRepository,
            preferencesService = preferencesService,
            emailService = emailService,
            emailTemplateRenderer = templateRenderer,
            emailScope = CoroutineScope(Dispatchers.Default)
        )

        val userInTenant = testUserInTenant(userId, tenantId, "peppol.concurrent@test.dokus")
        coEvery { userRepository.listByTenant(tenantId, true) } returns listOf(userInTenant)
        stubCreateNotification()
        coEvery { preferencesService.isEmailEnabled(userId, NotificationType.PeppolSendFailed) } returns Result.success(true)

        val emailSentFlag = AtomicBoolean(false)
        coEvery {
            notificationRepository.hasRecentEmailFor(
                tenantId,
                userId,
                NotificationType.PeppolSendFailed,
                "inv-race"
            )
        } answers {
            Result.success(emailSentFlag.get())
        }
        coEvery {
            templateRenderer.renderNotification(
                type = NotificationType.PeppolSendFailed,
                title = any(),
                details = any(),
                openPath = any()
            )
        } returns EmailTemplate(
            subject = "PEPPOL send failed",
            htmlBody = "<html/>",
            textBody = "text"
        )
        coEvery { emailService.send(any(), any(), any(), any()) } coAnswers {
            delay(120)
            Result.success(Unit)
        }
        coEvery { notificationRepository.markEmailSent(any(), any(), any()) } answers {
            emailSentFlag.set(true)
            Result.success(true)
        }

        coroutineScope {
            launch {
                concurrentService.emit(
                    NotificationEmission(
                        tenantId = tenantId,
                        type = NotificationType.PeppolSendFailed,
                        title = "PEPPOL send failed - Inv #race",
                        referenceType = NotificationReferenceType.Invoice,
                        referenceId = "inv-race",
                        openPath = "/cashflow/document_review/doc-race"
                    )
                ).getOrThrow()
            }
            launch {
                concurrentService.emit(
                    NotificationEmission(
                        tenantId = tenantId,
                        type = NotificationType.PeppolSendFailed,
                        title = "PEPPOL send failed - Inv #race",
                        referenceType = NotificationReferenceType.Invoice,
                        referenceId = "inv-race",
                        openPath = "/cashflow/document_review/doc-race"
                    )
                ).getOrThrow()
            }
        }

        coVerify(timeout = 2_000, exactly = 1) { emailService.send("peppol.concurrent@test.dokus", any(), any(), any()) }
    }

    private fun stubCreateNotification() {
        coEvery {
            notificationRepository.create(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            val tenant = TenantId(arg<Uuid>(0))
            val user = UserId(arg<Uuid>(1))
            val type = arg<NotificationType>(2)
            val title = arg<String>(3)
            val refType = arg<NotificationReferenceType>(4)
            val refId = arg<String>(5)

            Result.success(
                NotificationDto(
                    id = NotificationId.generate(),
                    tenantId = tenant,
                    userId = user,
                    type = type,
                    title = title,
                    referenceType = refType,
                    referenceId = refId,
                    isRead = false,
                    createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                    emailSent = false
                )
            )
        }
    }

    private fun testUserInTenant(
        userId: UserId,
        tenantId: TenantId,
        email: String
    ): UserInTenant {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return UserInTenant(
            user = User(
                id = userId,
                email = Email(email),
                firstName = Name("Test"),
                lastName = Name("User"),
                emailVerified = true,
                isActive = true,
                createdAt = now,
                updatedAt = now
            ),
            tenantId = tenantId,
            role = UserRole.Viewer,
            membershipActive = true
        )
    }
}
