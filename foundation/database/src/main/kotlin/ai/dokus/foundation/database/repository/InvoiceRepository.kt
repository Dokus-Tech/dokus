package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.enums.AuditAction
import ai.dokus.foundation.database.enums.EntityType
import ai.dokus.foundation.database.enums.InvoiceStatus
import ai.dokus.foundation.database.enums.PaymentMethod
import ai.dokus.foundation.database.mappers.InvoiceMapper.toInvoice
import ai.dokus.foundation.database.mappers.InvoiceMapper.toInvoiceItem
import ai.dokus.foundation.database.mappers.toJavaLocalDate
import ai.dokus.foundation.database.tables.InvoiceItemsTable
import ai.dokus.foundation.database.tables.InvoicesTable
import ai.dokus.foundation.database.tables.PaymentsTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.InvoiceItem
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
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

class InvoiceRepository {
    private val logger = LoggerFactory.getLogger(InvoiceRepository::class.java)
    private val tenantRepository = TenantRepository()
    private val auditLogRepository = AuditLogRepository()

    suspend fun create(
        tenantId: String,
        clientId: String,
        items: List<InvoiceItem>,
        issueDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        dueDate: LocalDate = issueDate.plus(30, DateTimeUnit.DAY),
        notes: String? = null,
        userId: String? = null
    ): String {
        // Get next invoice number (this is a suspend function, called outside dbQuery)
        val invoiceNumber = tenantRepository.getNextInvoiceNumber(tenantId)

        // Calculate totals from domain model (String values)
        val subtotal = items.sumOf { BigDecimal(it.lineTotal) }
        val vatTotal = items.sumOf { BigDecimal(it.vatAmount) }
        val total = subtotal + vatTotal

        val tenantUuid = UUID.fromString(tenantId)
        val clientUuid = UUID.fromString(clientId)

        val invoiceId = dbQuery {
            // Insert invoice
            val id = InvoicesTable.insertAndGetId {
                it[InvoicesTable.tenantId] = tenantUuid
                it[InvoicesTable.clientId] = clientUuid
                it[InvoicesTable.invoiceNumber] = invoiceNumber
                it[InvoicesTable.issueDate] = issueDate.toJavaLocalDate()
                it[InvoicesTable.dueDate] = dueDate.toJavaLocalDate()
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
                    it[quantity] = BigDecimal(item.quantity)
                    it[unitPrice] = BigDecimal(item.unitPrice)
                    it[vatRate] = BigDecimal(item.vatRate)
                    it[lineTotal] = BigDecimal(item.lineTotal)
                    it[InvoiceItemsTable.vatAmount] = BigDecimal(item.vatAmount)
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
            entityId = invoiceId.toString(),
            newValues = mapOf(
                "invoice_number" to invoiceNumber,
                "total" to total.toString(),
                "client_id" to clientId
            )
        )

        logger.info("Created invoice $invoiceNumber for tenant $tenantId")
        return invoiceId.toString()
    }

    suspend fun findById(invoiceId: String, tenantId: String): Invoice? = dbQuery {
        val invoiceUuid = UUID.fromString(invoiceId)
        val tenantUuid = UUID.fromString(tenantId)
        val invoice = InvoicesTable
            .selectAll()
            .where { (InvoicesTable.id eq invoiceUuid) and (InvoicesTable.tenantId eq tenantUuid) }
            .singleOrNull()
            ?.toInvoice()

        invoice?.let {
            val items = InvoiceItemsTable
                .selectAll()
                .where { InvoiceItemsTable.invoiceId eq invoiceUuid }
                .orderBy(InvoiceItemsTable.sortOrder)
                .map { it.toInvoiceItem() }

            invoice.copy(items = items)
        }
    }

    suspend fun listByTenant(
        tenantId: String,
        status: InvoiceStatus? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Long = 0
    ): List<Invoice> = dbQuery {
        val tenantUuid = UUID.fromString(tenantId)
        var query = InvoicesTable
            .selectAll()
            .where { InvoicesTable.tenantId eq tenantUuid }

        status?.let {
            query = query.andWhere { InvoicesTable.status eq it }
        }

        fromDate?.let {
            query = query.andWhere { InvoicesTable.issueDate greaterEq it.toJavaLocalDate() }
        }

        toDate?.let {
            query = query.andWhere { InvoicesTable.issueDate lessEq it.toJavaLocalDate() }
        }

        query
            .orderBy(InvoicesTable.issueDate to SortOrder.DESC)
            .limit(limit)
            .offset(offset)
            .map { it.toInvoice() }
    }

    suspend fun updateStatus(
        invoiceId: String,
        tenantId: String,
        newStatus: InvoiceStatus,
        userId: String? = null
    ) {
        val invoiceUuid = UUID.fromString(invoiceId)
        val tenantUuid = UUID.fromString(tenantId)
        val oldStatus = dbQuery {
            val oldInvoice = InvoicesTable
                .selectAll()
                .where { (InvoicesTable.id eq invoiceUuid) and (InvoicesTable.tenantId eq tenantUuid) }
                .singleOrNull() ?: throw IllegalArgumentException("Invoice not found")

            val status = oldInvoice[InvoicesTable.status]

            InvoicesTable.update({
                (InvoicesTable.id eq invoiceUuid) and (InvoicesTable.tenantId eq tenantUuid)
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
        invoiceId: String,
        tenantId: String,
        amount: String,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        transactionId: String? = null,
        notes: String? = null,
        userId: String? = null
    ): String {
        val invoiceUuid = UUID.fromString(invoiceId)
        val tenantUuid = UUID.fromString(tenantId)
        val amountDecimal = BigDecimal(amount)
        val paymentId = dbQuery {
            // Verify invoice belongs to tenant
            val invoice = InvoicesTable
                .selectAll()
                .where { (InvoicesTable.id eq invoiceUuid) and (InvoicesTable.tenantId eq tenantUuid) }
                .singleOrNull() ?: throw IllegalArgumentException("Invoice not found")

            // Record payment
            val id = PaymentsTable.insertAndGetId {
                it[PaymentsTable.tenantId] = tenantUuid
                it[PaymentsTable.invoiceId] = invoiceUuid
                it[PaymentsTable.amount] = amountDecimal
                it[PaymentsTable.paymentDate] = paymentDate.toJavaLocalDate()
                it[PaymentsTable.paymentMethod] = paymentMethod
                it[PaymentsTable.transactionId] = transactionId
                it[PaymentsTable.notes] = notes
            }.value

            // Update invoice
            val newPaidAmount = invoice[InvoicesTable.paidAmount] + amountDecimal
            val totalAmount = invoice[InvoicesTable.totalAmount]
            val newStatus = when {
                newPaidAmount >= totalAmount -> InvoiceStatus.PAID
                newPaidAmount > BigDecimal.ZERO -> InvoiceStatus.PARTIALLY_PAID
                else -> invoice[InvoicesTable.status]
            }

            InvoicesTable.update({ InvoicesTable.id eq invoiceUuid }) {
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
            entityId = paymentId.toString(),
            newValues = mapOf(
                "amount" to amount,
                "invoice_id" to invoiceId,
                "method" to paymentMethod.dbValue
            )
        )

        logger.info("Recorded payment of $amount for invoice $invoiceId")
        return paymentId.toString()
    }
}