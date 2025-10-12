package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.tables.*
import ai.dokus.foundation.database.utils.dbQuery
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
    val status: String,
    val currency: String = "EUR",
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
    ): UUID = dbQuery {
        // Get next invoice number
        val invoiceNumber = tenantRepository.getNextInvoiceNumber(tenantId)

        // Calculate totals
        val subtotal = items.sumOf { it.lineTotal }
        val vatTotal = items.sumOf { it.vatAmount }
        val total = subtotal + vatTotal

        // Insert invoice
        val invoiceId = InvoicesTable.insertAndGetId {
            it[InvoicesTable.tenantId] = tenantId
            it[InvoicesTable.clientId] = clientId
            it[InvoicesTable.invoiceNumber] = invoiceNumber
            it[InvoicesTable.issueDate] = issueDate
            it[InvoicesTable.dueDate] = dueDate
            it[subtotalAmount] = subtotal
            it[vatAmount] = vatTotal
            it[totalAmount] = total
            it[paidAmount] = BigDecimal.ZERO
            it[status] = "draft"
            it[InvoicesTable.notes] = notes
        }.value

        // Insert items
        items.forEachIndexed { index, item ->
            InvoiceItemsTable.insert {
                it[InvoiceItemsTable.invoiceId] = invoiceId
                it[description] = item.description
                it[quantity] = item.quantity
                it[unitPrice] = item.unitPrice
                it[vatRate] = item.vatRate
                it[lineTotal] = item.lineTotal
                it[InvoiceItemsTable.vatAmount] = item.vatAmount
                it[sortOrder] = index
            }
        }

        // Audit log
        auditLogRepository.log(
            tenantId = tenantId,
            userId = userId,
            action = "invoice.created",
            entityType = "invoice",
            entityId = invoiceId,
            newValues = mapOf(
                "invoice_number" to invoiceNumber,
                "total" to total.toString(),
                "client_id" to clientId.toString()
            )
        )

        logger.info("Created invoice $invoiceNumber for tenant $tenantId")
        invoiceId
    }

    suspend fun findById(invoiceId: UUID, tenantId: UUID): Invoice? = dbQuery {
        val invoice = InvoicesTable
            .select { (InvoicesTable.id eq invoiceId) and (InvoicesTable.tenantId eq tenantId) }
            .singleOrNull()
            ?.toInvoice()

        invoice?.let {
            val items = InvoiceItemsTable
                .select { InvoiceItemsTable.invoiceId eq invoiceId }
                .orderBy(InvoiceItemsTable.sortOrder)
                .map { it.toInvoiceItem() }

            invoice.copy(items = items)
        }
    }

    suspend fun listByTenant(
        tenantId: UUID,
        status: String? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Long = 0
    ): List<Invoice> = dbQuery {
        var query = InvoicesTable
            .select { InvoicesTable.tenantId eq tenantId }

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
            .limit(limit, offset)
            .map { it.toInvoice() }
    }

    suspend fun updateStatus(
        invoiceId: UUID,
        tenantId: UUID,
        newStatus: String,
        userId: UUID? = null
    ): Unit = dbQuery {
        val oldInvoice = InvoicesTable
            .select { (InvoicesTable.id eq invoiceId) and (InvoicesTable.tenantId eq tenantId) }
            .singleOrNull() ?: throw IllegalArgumentException("Invoice not found")

        val oldStatus = oldInvoice[InvoicesTable.status]

        InvoicesTable.update({
            (InvoicesTable.id eq invoiceId) and (InvoicesTable.tenantId eq tenantId)
        }) {
            it[status] = newStatus
            if (newStatus == "paid") {
                it[paidAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }

        // Audit log
        auditLogRepository.log(
            tenantId = tenantId,
            userId = userId,
            action = "invoice.status_changed",
            entityType = "invoice",
            entityId = invoiceId,
            oldValues = mapOf("status" to oldStatus),
            newValues = mapOf("status" to newStatus)
        )
    }

    suspend fun recordPayment(
        invoiceId: UUID,
        tenantId: UUID,
        amount: BigDecimal,
        paymentDate: LocalDate,
        paymentMethod: String,
        transactionId: String? = null,
        notes: String? = null,
        userId: UUID? = null
    ): UUID = dbQuery {
        // Verify invoice belongs to tenant
        val invoice = InvoicesTable
            .select { (InvoicesTable.id eq invoiceId) and (InvoicesTable.tenantId eq tenantId) }
            .singleOrNull() ?: throw IllegalArgumentException("Invoice not found")

        // Record payment
        val paymentId = PaymentsTable.insertAndGetId {
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
            newPaidAmount >= totalAmount -> "paid"
            newPaidAmount > BigDecimal.ZERO -> "partially_paid"
            else -> invoice[InvoicesTable.status]
        }

        InvoicesTable.update({ InvoicesTable.id eq invoiceId }) {
            it[paidAmount] = newPaidAmount
            it[status] = newStatus
            if (newStatus == "paid") {
                it[paidAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }

        // Audit log
        auditLogRepository.log(
            tenantId = tenantId,
            userId = userId,
            action = "payment.recorded",
            entityType = "payment",
            entityId = paymentId,
            newValues = mapOf(
                "amount" to amount.toString(),
                "invoice_id" to invoiceId.toString(),
                "method" to paymentMethod
            )
        )

        logger.info("Recorded payment of $amount for invoice $invoiceId")
        paymentId
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