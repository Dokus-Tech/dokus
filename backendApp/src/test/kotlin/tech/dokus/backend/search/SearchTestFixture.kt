package tech.dokus.backend.search

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import tech.dokus.backend.services.search.SearchService
import tech.dokus.database.repository.search.SearchRepository
import tech.dokus.database.repository.search.SearchSignalRepository
import tech.dokus.database.repository.search.SearchSuggestionRepository
import tech.dokus.database.repository.search.SearchPersonalizationQueries
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.CreditNotesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.database.tables.search.SearchSignalStatsTable
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import java.math.BigDecimal
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
abstract class SearchTestFixture {

    protected lateinit var database: Database

    protected val searchSignalRepository = SearchSignalRepository()
    protected val searchSuggestionRepository = SearchSuggestionRepository(
        searchSignalRepository = searchSignalRepository,
        personalizationQueries = SearchPersonalizationQueries(),
    )
    protected val searchRepository = SearchRepository(searchSuggestionRepository)
    protected val searchService = SearchService(searchRepository, searchSignalRepository)

    protected lateinit var tenantUuid: UUID
    protected lateinit var userUuid: UUID
    protected val tenantId: TenantId
        get() = TenantId(tenantUuid.toKotlinUuid())
    protected val userId: UserId
        get() = UserId(userUuid.toKotlinUuid())

    @BeforeEach
    fun setupSearchFixture() {
        database = Database.connect(
            url = "jdbc:h2:mem:search_test_${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                UsersTable,
                DocumentsTable,
                DocumentSourcesTable,
                DocumentIngestionRunsTable,
                ContactsTable,
                DocumentDraftsTable,
                InvoicesTable,
                ExpensesTable,
                CreditNotesTable,
                CashflowEntriesTable,
                SearchSignalStatsTable,
            )
        }

        tenantUuid = UUID.randomUUID()
        userUuid = UUID.randomUUID()

        insertTenant(tenantUuid, "Tenant A")
        insertUser(userUuid, "owner@tenant-a.test")
    }

    @AfterEach
    fun teardownSearchFixture() {
        // No-op: each test uses an isolated in-memory database name.
    }

    protected fun insertTenant(tenant: UUID, name: String) {
        transaction(database) {
            TenantTable.insert {
                it[id] = tenant
                it[type] = TenantType.Company
                it[legalName] = name
                it[displayName] = name
                it[plan] = SubscriptionTier.Core
                it[status] = TenantStatus.Active
                it[language] = Language.En
                it[vatNumber] = "BE0123456789"
            }
        }
    }

    protected fun insertUser(user: UUID, email: String) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        transaction(database) {
            UsersTable.insert {
                it[id] = user
                it[UsersTable.email] = email
                it[passwordHash] = "hash"
                it[firstName] = "Test"
                it[lastName] = "User"
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    protected fun insertContact(
        tenant: UUID,
        name: String,
        isActive: Boolean = true,
    ): UUID {
        val id = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        transaction(database) {
            ContactsTable.insert {
                it[ContactsTable.id] = id
                it[tenantId] = tenant
                it[ContactsTable.name] = name
                it[ContactsTable.isActive] = isActive
                it[contactSource] = ContactSource.Manual
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
        return id
    }

    protected fun insertDocument(
        tenant: UUID,
        filename: String,
    ): UUID {
        val id = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        transaction(database) {
            DocumentsTable.insert {
                it[DocumentsTable.id] = id
                it[tenantId] = tenant
                it[DocumentsTable.filename] = filename
                it[contentType] = "application/pdf"
                it[sizeBytes] = 512L
                it[storageKey] = "docs/$id.pdf"
                it[documentSource] = DocumentSource.Upload
                it[uploadedAt] = now
            }
        }
        return id
    }

    protected fun insertCashflowEntry(
        tenant: UUID,
        status: CashflowEntryStatus,
        eventDate: LocalDate,
        counterpartyId: UUID?,
    ): UUID {
        val id = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val amount = BigDecimal("100.00")
        transaction(database) {
            CashflowEntriesTable.insert {
                it[CashflowEntriesTable.id] = id
                it[tenantId] = tenant
                it[sourceType] = CashflowSourceType.Invoice
                it[sourceId] = UUID.randomUUID()
                it[documentId] = null
                it[direction] = CashflowDirection.Out
                it[CashflowEntriesTable.eventDate] = eventDate
                it[amountGross] = amount
                it[amountVat] = BigDecimal.ZERO
                it[remainingAmount] = if (status == CashflowEntryStatus.Paid) BigDecimal.ZERO else amount
                it[CashflowEntriesTable.status] = status
                it[CashflowEntriesTable.counterpartyId] = counterpartyId
                it[CashflowEntriesTable.paidAt] = if (status == CashflowEntryStatus.Paid) now else null
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
        return id
    }
}
