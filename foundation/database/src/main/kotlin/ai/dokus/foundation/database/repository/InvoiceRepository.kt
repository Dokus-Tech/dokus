package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.enums.AuditAction
import ai.dokus.foundation.database.enums.Currency
import ai.dokus.foundation.database.enums.EntityType
import ai.dokus.foundation.database.enums.InvoiceStatus
import ai.dokus.foundation.database.enums.PaymentMethod
import ai.dokus.foundation.database.tables.InvoiceItemsTable
import ai.dokus.foundation.database.tables.InvoicesTable
import ai.dokus.foundation.database.tables.PaymentsTable
import ai.dokus.foundation.database.utils.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.UUID

data class Invoice(
    val id: UUID,
    val tenantId: UUID,
    val clientId: UUID,
    val invoiceNumber: String,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val subtotalAmount: BigDecimal,
    val vatAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val status: InvoiceStatus,
    val currency: Currency = Currency.EUR,
    val notes: String? = null,
    val items: List<InvoiceItem> = emptyList()
)

data class InvoiceItem(
    val id: UUID? = null,
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val vatRate: BigDecimal,
    val lineTotal: BigDecimal,
    val vatAmount: BigDecimal
)

class InvoiceRepository {
    private val logger = LoggerFactory.getLogger(InvoiceRepository::class.java)
    private val tenantRepository = TenantRepository()
    private val auditLogRepository = AuditLogRepository()

    suspend fun create(
        tenantId: UUID,
        clientId: UUID,
        items: List<InvoiceItem>,
        issueDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        dueDate: LocalDate = issueDate.plus(30, DateTimeUnit.DAY),
        notes: String? = null,
        userId: UUID? = null
    ): UUID {
        // Get next invoice number (this is a suspend function, called outside dbQuery)
        val invoiceNumber = tenantRepository.getNextInvoiceNumber(tenantId)

        // Calculate totals
        val subtotal = items.sumOf { it.lineTotal }
        val vatTotal = items.sumOf { it.vatAmount }
        val total = subtotal + vatTotal

        val invoiceId = dbQuery {
            // Insert invoice
            val id = InvoicesTable.insertAndGetId {
                it[InvoicesTable.tenantId] = tenantId
                it[InvoicesTable.clientId] = clientId
                it[InvoicesTable.invoiceNumber] = invoiceNumber
                it[InvoicesTable.issueDate] = issueDate
                it[InvoicesTable.dueDate] = dueDate
                it[subtotalAmount] = subtotal
                it[vatAmount] = vatTotal
                it[totalAmount] = total
                it[paidAmount] = BigDecimal.ZERO
                it[status] = InvoiceStatus.DRAFT
                it[InvoicesTable.notes] = notes
            }.value

            // Insert items
            items.forEachIndexed { index, item ->
                InvoiceItemsTable.insert {
                    it[InvoiceItemsTable.invoiceId] = id
                    it[description] = item.description
                    it[quantity] = item.quantity
                    it[unitPrice] = item.unitPrice
                    it[vatRate] = item.vatRate
                    it[lineTotal] = item.lineTotal
                    it[InvoiceItemsTable.vatAmount] = item.vatAmount
                    it[sortOrder] = index
                }
            }
            id
        }

        // Audit log (called outside dbQuery as it's a suspend function)
        auditLogRepository.log(
            tenantId = tenantId,
            userId = userId,
            action = AuditAction.INVOICE_CREATED,
            entityType = EntityType.INVOICE,
            entityId = invoiceId,
            newValues = mapOf(
                "invoice_number" to invoiceNumber,
                "total" to total.toString(),
                "client_id" to clientId.toString()
            )
        )

        logger.info("Created invoice $invoiceNumber for tenant $tenantId")
        return invoiceId
    }

    suspend fun findById(invoiceId: UUID, tenantId: UUID): Invoice? = dbQuery {
        val invoice = InvoicesTable
            .selectAll()
            .where { (InvoicesTable.id eq invoiceId) and (InvoicesTable.tenantId eq tenantId) }
            .singleOrNull()
            ?.toInvoice()

        invoice?.let {
            val items = InvoiceItemsTable
                .selectAll()
                .where { InvoiceItemsTable.invoiceId eq invoiceId }
                .orderBy(InvoiceItemsTable.sortOrder)
                .map { it.toInvoiceItem() }

            invoice.copy(items = items)
        }
    }

    suspend fun listByTenant(
        tenantId: UUID,
        status: InvoiceStatus? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Long = 0
    ): List<Invoice> = dbQuery {
        var query = InvoicesTable
            .selectAll()
            .where { InvoicesTable.tenantId eq tenantId }

        status?.let {
            query = query.andWhere { InvoicesTable.status eq it }
        }

        fromDate?.let {
            query = query.andWhere { InvoicesTable.issueDate greaterEq it }
        }

        toDate?.let {
            query = query.andWhere { InvoicesTable.issueDate lessEq it }
        }

        query
            .orderBy(InvoicesTable.issueDate to SortOrder.DESC)
            .limit(limit)
            .offset(offset)
            .map { it.toInvoice() }
    }

    suspend fun updateStatus(
        invoiceId: UUID,
        tenantId: UUID,
        newStatus: InvoiceStatus,
        userId: UUID? = null
    ) {
        val oldStatus = dbQuery {
            val oldInvoice = InvoicesTable
                .selectAll()
                .where { (InvoicesTable.id eq invoiceId) and (InvoicesTable.tenantId eq tenantId) }
                .singleOrNull() ?: throw IllegalArgumentException("Invoice not found")

            val status = oldInvoice[InvoicesTable.status]

            InvoicesTable.update({
                (InvoicesTable.id eq invoiceId) and (InvoicesTable.tenantId eq tenantId)
            }) {
                it[InvoicesTable.status] = newStatus
                if (newStatus == InvoiceStatus.PAID) {
                    it[paidAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }

            status
        }

        // Audit log (called outside dbQuery as it's a suspend function)
        auditLogRepository.log(
            tenantId = tenantId,
            userId = userId,
            action = AuditAction.INVOICE_STATUS_CHANGED,
            entityType = EntityType.INVOICE,
            entityId = invoiceId,
            oldValues = mapOf("status" to oldStatus.dbValue),
            newValues = mapOf("status" to newStatus.dbValue)
        )
    }

    suspend fun recordPayment(
        invoiceId: UUID,
        tenantId: UUID,
        amount: BigDecimal,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        transactionId: String? = null,
        notes: String? = null,
        userId: UUID? = null
    ): UUID {
        val paymentId = dbQuery {
            // Verify invoice belongs to tenant
            val invoice = InvoicesTable
                .selectAll()
                .where { (InvoicesTable.id eq invoiceId) and (InvoicesTable.tenantId eq tenantId) }
                .singleOrNull() ?: throw IllegalArgumentException("Invoice not found")

            // Record payment
            val id = PaymentsTable.insertAndGetId {
                it[PaymentsTable.tenantId] = tenantId
                it[PaymentsTable.invoiceId] = invoiceId
                it[PaymentsTable.amount] = amount
                it[PaymentsTable.paymentDate] = paymentDate
                it[PaymentsTable.paymentMethod] = paymentMethod
                it[PaymentsTable.transactionId] = transactionId
                it[PaymentsTable.notes] = notes
            }.value

            // Update invoice
            val newPaidAmount = invoice[InvoicesTable.paidAmount] + amount
            val totalAmount = invoice[InvoicesTable.totalAmount]
            val newStatus = when {
                newPaidAmount >= totalAmount -> InvoiceStatus.PAID
                newPaidAmount > BigDecimal.ZERO -> InvoiceStatus.PARTIALLY_PAID
                else -> invoice[InvoicesTable.status]
            }

            InvoicesTable.update({ InvoicesTable.id eq invoiceId }) {
                it[paidAmount] = newPaidAmount
                it[status] = newStatus
                if (newStatus == InvoiceStatus.PAID) {
                    it[paidAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }

            id
        }

        // Audit log (called outside dbQuery as it's a suspend function)
        auditLogRepository.log(
            tenantId = tenantId,
            userId = userId,
            action = AuditAction.PAYMENT_RECORDED,
            entityType = EntityType.PAYMENT,
            entityId = paymentId,
            newValues = mapOf(
                "amount" to amount.toString(),
                "invoice_id" to invoiceId.toString(),
                "method" to paymentMethod.dbValue
            )
        )

        logger.info("Recorded payment of $amount for invoice $invoiceId")
        return paymentId
    }

    private fun ResultRow.toInvoice() = Invoice(
        id = this[InvoicesTable.id].value,
        tenantId = this[InvoicesTable.tenantId].value,
        clientId = this[InvoicesTable.clientId].value,
        invoiceNumber = this[InvoicesTable.invoiceNumber],
        issueDate = this[InvoicesTable.issueDate],
        dueDate = this[InvoicesTable.dueDate],
        subtotalAmount = this[InvoicesTable.subtotalAmount],
        vatAmount = this[InvoicesTable.vatAmount],
        totalAmount = this[InvoicesTable.totalAmount],
        paidAmount = this[InvoicesTable.paidAmount],
        status = this[InvoicesTable.status],
        currency = this[InvoicesTable.currency],
        notes = this[InvoicesTable.notes]
    )

    private fun ResultRow.toInvoiceItem() = InvoiceItem(
        id = this[InvoiceItemsTable.id].value,
        description = this[InvoiceItemsTable.description],
        quantity = this[InvoiceItemsTable.quantity],
        unitPrice = this[InvoiceItemsTable.unitPrice],
        vatRate = this[InvoiceItemsTable.vatRate],
        lineTotal = this[InvoiceItemsTable.lineTotal],
        vatAmount = this[InvoiceItemsTable.vatAmount]
    )
}