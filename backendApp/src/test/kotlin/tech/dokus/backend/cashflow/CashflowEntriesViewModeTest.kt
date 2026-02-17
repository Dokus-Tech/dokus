package tech.dokus.backend.cashflow
import kotlin.uuid.Uuid

import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.CashflowViewMode
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.TenantId
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CashflowEntriesViewModeTest {

    private lateinit var database: Database
    private val repository = CashflowEntriesRepository()
    private val service = CashflowEntriesService(repository)

    private lateinit var tenantUuid: Uuid
    private lateinit var contactUuid: Uuid
    private val tenantId: TenantId get() = TenantId(tenantUuid)

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                DocumentsTable,
                ContactsTable,
                CashflowEntriesTable
            )
        }

        tenantUuid = Uuid.random()
        contactUuid = Uuid.random()

        transaction(database) {
            TenantTable.insert {
                it[id] = tenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Test Company"
                it[displayName] = "Test Company"
                it[plan] = SubscriptionTier.Core
                it[status] = TenantStatus.Active
                it[language] = Language.En
                it[vatNumber] = "BE0123456789"
            }

            ContactsTable.insert {
                it[id] = contactUuid
                it[tenantId] = tenantUuid
                it[name] = "Acme"
                it[contactSource] = ContactSource.Manual
            }
        }
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                CashflowEntriesTable,
                ContactsTable,
                DocumentsTable,
                TenantTable
            )
        }
    }

    @Test
    fun `Upcoming and Overdue modes split unpaid entries deterministically`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

        val upcomingEntry = insertCashflowEntry(
            eventDate = today.plus(DatePeriod(days = 5)),
            status = CashflowEntryStatus.Open,
            amountGrossMinor = 10_000L,
            remainingMinor = 10_000L
        )
        val overdueOpenEntry = insertCashflowEntry(
            eventDate = today.plus(DatePeriod(days = -2)),
            status = CashflowEntryStatus.Open,
            amountGrossMinor = 20_000L,
            remainingMinor = 20_000L
        )
        val overdueFlaggedEntry = insertCashflowEntry(
            eventDate = today.plus(DatePeriod(days = -10)),
            status = CashflowEntryStatus.Overdue,
            amountGrossMinor = 30_000L,
            remainingMinor = 30_000L
        )
        insertCashflowEntry(
            eventDate = today.plus(DatePeriod(days = -3)),
            status = CashflowEntryStatus.Paid,
            amountGrossMinor = 15_000L,
            remainingMinor = 0L,
            paidAt = LocalDateTime(today.year, today.monthNumber, today.dayOfMonth, 10, 0, 0)
        )

        val upcoming = repository.listEntries(
            tenantId = tenantId,
            viewMode = CashflowViewMode.Upcoming,
            fromDate = today,
            toDate = today.plus(DatePeriod(days = 30)),
            statuses = listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue)
        ).getOrThrow()

        assertTrue(upcoming.any { it.id == CashflowEntryId.parse(upcomingEntry.toString()) })
        assertTrue(upcoming.none { it.id == CashflowEntryId.parse(overdueOpenEntry.toString()) })
        assertTrue(upcoming.none { it.id == CashflowEntryId.parse(overdueFlaggedEntry.toString()) })

        val overdue = repository.listEntries(
            tenantId = tenantId,
            viewMode = CashflowViewMode.Overdue,
            statuses = listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue)
        ).getOrThrow()

        val overdueIds = overdue.map { it.id }.toSet()
        assertTrue(CashflowEntryId.parse(overdueOpenEntry.toString()) in overdueIds)
        assertTrue(CashflowEntryId.parse(overdueFlaggedEntry.toString()) in overdueIds)
        assertTrue(CashflowEntryId.parse(upcomingEntry.toString()) !in overdueIds)
    }

    @Test
    fun `View modes apply default status sets when statuses are omitted`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val yesterday = today.plus(DatePeriod(days = -1))
        val tomorrow = today.plus(DatePeriod(days = 1))
        val nowPaidAt = LocalDateTime(today.year, today.monthNumber, today.dayOfMonth, 10, 0, 0)
        val futurePaidDate = today.plus(DatePeriod(days = 3))
        val futurePaidAt = LocalDateTime(
            futurePaidDate.year,
            futurePaidDate.monthNumber,
            futurePaidDate.dayOfMonth,
            10,
            0,
            0
        )

        val upcomingOpen = insertCashflowEntry(
            eventDate = tomorrow,
            status = CashflowEntryStatus.Open,
            amountGrossMinor = 11_000L,
            remainingMinor = 11_000L
        )
        insertCashflowEntry(
            eventDate = tomorrow.plus(DatePeriod(days = 2)),
            status = CashflowEntryStatus.Paid,
            amountGrossMinor = 12_000L,
            remainingMinor = 0L,
            paidAt = futurePaidAt
        )
        val overdueOpen = insertCashflowEntry(
            eventDate = yesterday,
            status = CashflowEntryStatus.Overdue,
            amountGrossMinor = 13_000L,
            remainingMinor = 13_000L
        )
        val overduePaid = insertCashflowEntry(
            eventDate = yesterday,
            status = CashflowEntryStatus.Paid,
            amountGrossMinor = 14_000L,
            remainingMinor = 0L,
            paidAt = nowPaidAt
        )

        val upcoming = repository.listEntries(
            tenantId = tenantId,
            viewMode = CashflowViewMode.Upcoming,
            fromDate = today,
            toDate = today.plus(DatePeriod(days = 30))
        ).getOrThrow()
        assertEquals(
            setOf(CashflowEntryId.parse(upcomingOpen.toString())),
            upcoming.map { it.id }.toSet()
        )

        val overdue = repository.listEntries(
            tenantId = tenantId,
            viewMode = CashflowViewMode.Overdue
        ).getOrThrow()
        assertEquals(
            setOf(CashflowEntryId.parse(overdueOpen.toString())),
            overdue.map { it.id }.toSet()
        )

        val history = repository.listEntries(
            tenantId = tenantId,
            viewMode = CashflowViewMode.History,
            fromDate = today.plus(DatePeriod(days = -1)),
            toDate = today.plus(DatePeriod(days = 1))
        ).getOrThrow()
        assertEquals(
            setOf(CashflowEntryId.parse(overduePaid.toString())),
            history.map { it.id }.toSet()
        )
    }

    @Suppress("LongParameterList")
    private fun insertCashflowEntry(
        eventDate: LocalDate,
        status: CashflowEntryStatus,
        amountGrossMinor: Long,
        remainingMinor: Long,
        amountVatMinor: Long = 0L,
        paidAt: LocalDateTime? = null
    ): Uuid {
        val entryUuid = Uuid.random()
        transaction(database) {
            CashflowEntriesTable.insert {
                it[id] = entryUuid
                it[tenantId] = tenantUuid
                it[sourceType] = CashflowSourceType.Invoice
                it[sourceId] = Uuid.random()
                it[documentId] = null
                it[direction] = CashflowDirection.In
                it[CashflowEntriesTable.eventDate] = eventDate
                it[amountGross] = BigDecimal.valueOf(amountGrossMinor, 2)
                it[amountVat] = BigDecimal.valueOf(amountVatMinor, 2)
                it[remainingAmount] = BigDecimal.valueOf(remainingMinor, 2)
                it[CashflowEntriesTable.status] = status
                it[counterpartyId] = contactUuid
                it[CashflowEntriesTable.paidAt] = paidAt
            }
        }
        return entryUuid
    }
}
