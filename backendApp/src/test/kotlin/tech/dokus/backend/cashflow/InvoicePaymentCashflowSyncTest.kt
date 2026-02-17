package tech.dokus.backend.cashflow

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.cashflow.InvoiceService
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.InvoiceNumberRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.services.InvoiceNumberGenerator
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.RecordPaymentRequest
import tech.dokus.domain.toDbDecimal
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InvoicePaymentCashflowSyncTest {

    private lateinit var database: Database
    private lateinit var tenantUuid: UUID
    private lateinit var contactUuid: UUID
    private lateinit var invoiceUuid: UUID
    private val tenantId: TenantId get() = TenantId(tenantUuid)

    private val cashflowEntriesRepository = CashflowEntriesRepository()
    private val invoiceRepository = InvoiceRepository(InvoiceNumberGenerator(InvoiceNumberRepository()))
    private val invoiceService = InvoiceService(invoiceRepository, cashflowEntriesRepository)

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
                InvoicesTable,
                CashflowEntriesTable
            )
        }

        tenantUuid = Uuid.random()
        contactUuid = Uuid.random()
        invoiceUuid = Uuid.random()

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
                it[name] = "Test Contact"
                it[contactSource] = ContactSource.Manual
            }

            InvoicesTable.insert {
                it[id] = invoiceUuid
                it[tenantId] = tenantUuid
                it[contactId] = contactUuid
                it[InvoicesTable.invoiceNumber] = "INV-PAY-100"
                it[issueDate] = LocalDate(2024, 1, 1)
                it[dueDate] = LocalDate(2024, 1, 31)
                it[subtotalAmount] = BigDecimal("100.00")
                it[vatAmount] = BigDecimal("21.00")
                it[InvoicesTable.totalAmount] = BigDecimal("121.00")
                it[InvoicesTable.paidAmount] = BigDecimal.ZERO
                it[status] = InvoiceStatus.Draft
                it[direction] = DocumentDirection.Outbound
            }
        }
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                CashflowEntriesTable,
                InvoicesTable,
                ContactsTable,
                DocumentsTable,
                TenantTable
            )
        }
    }

    @Test
    fun `recordPayment sets cashflow paidAt when entry becomes paid`() = runBlocking {
        val invoiceId = InvoiceId.parse(invoiceUuid.toString())
        val entry = cashflowEntriesRepository.createEntry(
            tenantId = tenantId,
            sourceType = CashflowSourceType.Invoice,
            sourceId = Uuid.parse(invoiceId.toString()),
            documentId = null,
            direction = CashflowDirection.In,
            eventDate = LocalDate(2024, 1, 31),
            amountGross = Money.from("121.00")!!,
            amountVat = Money.from("21.00")!!,
            contactId = null
        ).getOrThrow()

        val request = RecordPaymentRequest(
            invoiceId = invoiceId,
            amount = Money.from("121.00")!!,
            paymentDate = LocalDate(2024, 2, 10),
            paymentMethod = PaymentMethod.BankTransfer
        )

        invoiceService.recordPayment(
            invoiceId = invoiceId,
            tenantId = tenantId,
            request = request
        ).getOrThrow()

        val updated = cashflowEntriesRepository.getEntry(entry.id, tenantId).getOrThrow()
        assertNotNull(updated)
        assertEquals(Money.ZERO, updated.remainingAmount)
        assertEquals(CashflowEntryStatus.Paid, updated.status)
        assertEquals(LocalDateTime(2024, 2, 10, 12, 0, 0), updated.paidAt)
    }
}
