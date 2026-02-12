package tech.dokus.backend.notifications

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.notifications.NotificationPreferencesRepository
import tech.dokus.database.repository.notifications.NotificationRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.notifications.NotificationPreferencesTable
import tech.dokus.database.tables.notifications.NotificationsTable
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.enums.NotificationReferenceType
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.NotificationId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class NotificationRepositoriesTest {

    private lateinit var database: Database
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var preferencesRepository: NotificationPreferencesRepository

    private lateinit var tenantUuid: UUID
    private lateinit var userUuid: UUID
    private var tenantId: TenantId = TenantId.generate()
    private var userId: UserId = UserId.generate()

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_notifications_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                UsersTable,
                NotificationsTable,
                NotificationPreferencesTable
            )
        }

        tenantUuid = UUID.randomUUID()
        userUuid = UUID.randomUUID()
        tenantId = TenantId(tenantUuid.toKotlinUuid())
        userId = UserId(userUuid.toKotlinUuid())

        transaction(database) {
            TenantTable.insert {
                it[id] = tenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Notification Test Tenant"
                it[displayName] = "Notification Test Tenant"
                it[plan] = SubscriptionTier.Core
                it[status] = TenantStatus.Active
                it[language] = Language.En
                it[vatNumber] = "BE0123456789"
            }

            UsersTable.insert {
                it[id] = userUuid
                it[email] = "notifications@test.dokus"
                it[passwordHash] = "hash"
                it[firstName] = "Notif"
                it[lastName] = "Tester"
                it[isActive] = true
            }
        }

        notificationRepository = NotificationRepository()
        preferencesRepository = NotificationPreferencesRepository()
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                NotificationPreferencesTable,
                NotificationsTable,
                UsersTable,
                TenantTable
            )
        }
    }

    @Test
    fun `notification repository supports create list unread and mark read flows`() = runBlocking {
        val peppol = notificationRepository.create(
            tenantId = tenantId,
            userId = userId,
            type = NotificationType.PeppolSendFailed,
            title = "PEPPOL send failed - Inv #2026-001",
            referenceType = NotificationReferenceType.Invoice,
            referenceId = "inv-1",
            isRead = false,
            emailSent = false
        ).getOrThrow()

        notificationRepository.create(
            tenantId = tenantId,
            userId = userId,
            type = NotificationType.ComplianceBlocker,
            title = "Document requires attention",
            referenceType = NotificationReferenceType.Document,
            referenceId = "doc-1",
            isRead = true,
            emailSent = false
        ).getOrThrow()

        notificationRepository.create(
            tenantId = tenantId,
            userId = userId,
            type = NotificationType.PaymentFailed,
            title = "Payment failed",
            referenceType = NotificationReferenceType.BillingItem,
            referenceId = "billing-1",
            isRead = false,
            emailSent = false
        ).getOrThrow()

        val unreadCountBefore = notificationRepository.unreadCount(tenantId, userId).getOrThrow()
        assertEquals(2, unreadCountBefore)

        val peppolOnly = notificationRepository.list(
            tenantId = tenantId,
            userId = userId,
            category = NotificationCategory.Peppol
        ).getOrThrow()
        assertEquals(1, peppolOnly.total)
        assertEquals(NotificationType.PeppolSendFailed, peppolOnly.items.single().type)

        val marked = notificationRepository.markRead(
            tenantId = tenantId,
            userId = userId,
            notificationId = peppol.id
        ).getOrThrow()
        assertTrue(marked)

        val unreadCountAfterSingle = notificationRepository.unreadCount(tenantId, userId).getOrThrow()
        assertEquals(1, unreadCountAfterSingle)

        val markedAll = notificationRepository.markAllRead(tenantId, userId).getOrThrow()
        assertEquals(1, markedAll)
        val unreadCountAfterAll = notificationRepository.unreadCount(tenantId, userId).getOrThrow()
        assertEquals(0, unreadCountAfterAll)
    }

    @Test
    fun `notification repository tracks email sent and recent dedup window`() = runBlocking {
        val notification = notificationRepository.create(
            tenantId = tenantId,
            userId = userId,
            type = NotificationType.PeppolSendFailed,
            title = "PEPPOL send failed - Inv #2026-009",
            referenceType = NotificationReferenceType.Invoice,
            referenceId = "inv-9",
            isRead = false,
            emailSent = false
        ).getOrThrow()

        assertFalse(
            notificationRepository.hasRecentEmailFor(
                tenantId = tenantId,
                userId = userId,
                type = NotificationType.PeppolSendFailed,
                referenceId = "inv-9"
            ).getOrThrow()
        )

        notificationRepository.markEmailSent(
            tenantId = tenantId,
            userId = userId,
            notificationId = NotificationId.parse(notification.id.toString())
        ).getOrThrow()

        assertTrue(
            notificationRepository.hasRecentEmailFor(
                tenantId = tenantId,
                userId = userId,
                type = NotificationType.PeppolSendFailed,
                referenceId = "inv-9"
            ).getOrThrow()
        )
    }

    @Test
    fun `recent email dedup is tenant-scoped`() = runBlocking {
        val otherTenantUuid = UUID.randomUUID()
        val otherTenantId = TenantId(otherTenantUuid.toKotlinUuid())
        transaction(database) {
            TenantTable.insert {
                it[id] = otherTenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Other Tenant"
                it[displayName] = "Other Tenant"
                it[plan] = SubscriptionTier.Core
                it[status] = TenantStatus.Active
                it[language] = Language.En
                it[vatNumber] = "BE9876543210"
            }
        }

        val otherTenantNotification = notificationRepository.create(
            tenantId = otherTenantId,
            userId = userId,
            type = NotificationType.PeppolSendFailed,
            title = "PEPPOL send failed - Other tenant",
            referenceType = NotificationReferenceType.Invoice,
            referenceId = "inv-shared",
            isRead = false,
            emailSent = false
        ).getOrThrow()
        notificationRepository.markEmailSent(
            tenantId = otherTenantId,
            userId = userId,
            notificationId = otherTenantNotification.id
        ).getOrThrow()

        assertFalse(
            notificationRepository.hasRecentEmailFor(
                tenantId = tenantId,
                userId = userId,
                type = NotificationType.PeppolSendFailed,
                referenceId = "inv-shared"
            ).getOrThrow()
        )
        assertTrue(
            notificationRepository.hasRecentEmailFor(
                tenantId = otherTenantId,
                userId = userId,
                type = NotificationType.PeppolSendFailed,
                referenceId = "inv-shared"
            ).getOrThrow()
        )
    }

    @Test
    fun `notification preferences repository stores overrides only`() = runBlocking {
        val initially = preferencesRepository.listOverrides(userId).getOrThrow()
        assertTrue(initially.isEmpty())

        preferencesRepository.setOverride(
            userId = userId,
            type = NotificationType.VatWarning,
            emailEnabled = true
        ).getOrThrow()

        val afterSet = preferencesRepository.listOverrides(userId).getOrThrow()
        assertEquals(true, afterSet[NotificationType.VatWarning])

        val removed = preferencesRepository.removeOverride(
            userId = userId,
            type = NotificationType.VatWarning
        ).getOrThrow()
        assertTrue(removed)
        assertTrue(preferencesRepository.listOverrides(userId).getOrThrow().isEmpty())
    }
}
