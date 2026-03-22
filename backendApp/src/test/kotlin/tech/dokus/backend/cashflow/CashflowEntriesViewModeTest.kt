package tech.dokus.backend.cashflow

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class CashflowEntriesViewModeTest {

    private lateinit var database: Database
    private val repository = CashflowEntriesRepository()

    private lateinit var tenantUuid: UUID
    private lateinit var contactUuid: UUID
    private val tenantId: TenantId get() = TenantId(tenantUuid.toKotlinUuid())

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

        tenantUuid = UUID.randomUUID()
        contactUuid = UUID.randomUUID()

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
            exec("DROP ALL OBJECTS")
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

    @Test
    fun `Cashflow start date excludes old entries from all view modes`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val startDate = today.minus(DatePeriod(days = 90))

        // Entry within tracking window (30 days ago)
        val recentOverdue = insertCashflowEntry(
            eventDate = today.minus(DatePeriod(days = 30)),
            status = CashflowEntryStatus.Overdue,
            amountGrossMinor = 10_000L,
            remainingMinor = 10_000L,
        )
        // Entry before tracking window (200 days ago)
        val oldOverdue = insertCashflowEntry(
            eventDate = today.minus(DatePeriod(days = 200)),
            status = CashflowEntryStatus.Overdue,
            amountGrossMinor = 50_000L,
            remainingMinor = 50_000L,
        )
        // Upcoming entry within window
        val upcomingEntry = insertCashflowEntry(
            eventDate = today.plus(DatePeriod(days = 5)),
            status = CashflowEntryStatus.Open,
            amountGrossMinor = 20_000L,
            remainingMinor = 20_000L,
        )
        // Paid entry within window (event 10 days ago, paid today)
        val recentPaid = insertCashflowEntry(
            eventDate = today.minus(DatePeriod(days = 10)),
            status = CashflowEntryStatus.Paid,
            amountGrossMinor = 15_000L,
            remainingMinor = 0L,
            paidAt = LocalDateTime(today.year, today.monthNumber, today.dayOfMonth, 10, 0, 0),
        )
        // Paid entry BEFORE window (event 200 days ago, paid today)
        insertCashflowEntry(
            eventDate = today.minus(DatePeriod(days = 200)),
            status = CashflowEntryStatus.Paid,
            amountGrossMinor = 30_000L,
            remainingMinor = 0L,
            paidAt = LocalDateTime(today.year, today.monthNumber, today.dayOfMonth, 10, 0, 0),
        )

        // Overdue with start date: old entry excluded
        val overdue = repository.listEntries(
            tenantId = tenantId,
            viewMode = CashflowViewMode.Overdue,
            statuses = listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue),
            cashflowStartDate = startDate,
        ).getOrThrow()

        val overdueIds = overdue.map { it.id }.toSet()
        assertTrue(CashflowEntryId.parse(recentOverdue.toString()) in overdueIds, "recent overdue should be included")
        assertTrue(CashflowEntryId.parse(oldOverdue.toString()) !in overdueIds, "old overdue should be excluded")

        // Upcoming with start date: still works (all upcoming are after start date anyway)
        val upcoming = repository.listEntries(
            tenantId = tenantId,
            viewMode = CashflowViewMode.Upcoming,
            fromDate = today,
            toDate = today.plus(DatePeriod(days = 30)),
            cashflowStartDate = startDate,
        ).getOrThrow()

        assertEquals(
            setOf(CashflowEntryId.parse(upcomingEntry.toString())),
            upcoming.map { it.id }.toSet(),
        )

        // History with start date: excludes old paid entries
        val history = repository.listEntries(
            tenantId = tenantId,
            viewMode = CashflowViewMode.History,
            fromDate = today.minus(DatePeriod(days = 1)),
            toDate = today.plus(DatePeriod(days = 1)),
            cashflowStartDate = startDate,
        ).getOrThrow()

        val historyIds = history.map { it.id }.toSet()
        assertTrue(CashflowEntryId.parse(recentPaid.toString()) in historyIds, "recent paid should be in history")
        assertEquals(1, historyIds.size, "old paid entry should be excluded from history")
    }

    @Test
    fun `Contact query respects cashflow start date`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val startDate = today.minus(DatePeriod(days = 90))

        // Recent open entry
        val recentEntry = insertCashflowEntry(
            eventDate = today.minus(DatePeriod(days = 10)),
            status = CashflowEntryStatus.Open,
            amountGrossMinor = 10_000L,
            remainingMinor = 10_000L,
        )
        // Old entry before start date
        insertCashflowEntry(
            eventDate = today.minus(DatePeriod(days = 200)),
            status = CashflowEntryStatus.Open,
            amountGrossMinor = 50_000L,
            remainingMinor = 50_000L,
        )

        val entries = repository.listOpenInvoiceEntriesByContact(
            tenantId = tenantId,
            contactId = tech.dokus.domain.ids.ContactId(contactUuid.toKotlinUuid()),
            cashflowStartDate = startDate,
        ).getOrThrow()

        assertEquals(1, entries.size, "old entry should be excluded from contact query")
        assertEquals(CashflowEntryId.parse(recentEntry.toString()), entries.single().id)
    }

    @Test
    fun `Without cashflow start date all entries are returned`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

        insertCashflowEntry(
            eventDate = today.minus(DatePeriod(days = 200)),
            status = CashflowEntryStatus.Overdue,
            amountGrossMinor = 50_000L,
            remainingMinor = 50_000L,
        )
        insertCashflowEntry(
            eventDate = today.minus(DatePeriod(days = 10)),
            status = CashflowEntryStatus.Overdue,
            amountGrossMinor = 10_000L,
            remainingMinor = 10_000L,
        )

        // Without cashflowStartDate, both should be returned
        val overdue = repository.listEntries(
            tenantId = tenantId,
            viewMode = CashflowViewMode.Overdue,
            statuses = listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue),
        ).getOrThrow()

        assertEquals(2, overdue.size, "without start date, all entries should be returned")
    }

    @Suppress("LongParameterList")
    private fun insertCashflowEntry(
        eventDate: LocalDate,
        status: CashflowEntryStatus,
        amountGrossMinor: Long,
        remainingMinor: Long,
        amountVatMinor: Long = 0L,
        paidAt: LocalDateTime? = null
    ): UUID {
        val entryUuid = UUID.randomUUID()
        transaction(database) {
            CashflowEntriesTable.insert {
                it[id] = entryUuid
                it[tenantId] = tenantUuid
                it[sourceType] = CashflowSourceType.Invoice
                it[sourceId] = UUID.randomUUID()
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
