@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.mappers.InvoiceMapper.toInvoice
import ai.dokus.foundation.database.mappers.InvoiceMapper.toInvoiceItem
import ai.dokus.foundation.database.tables.InvoiceItemsTable
import ai.dokus.foundation.database.tables.InvoicesTable
import ai.dokus.foundation.database.tables.PaymentsTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.BusinessUserId
import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.InvoiceId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.PaymentId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.TransactionId
import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.InvoiceItem
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class InvoiceRepository {
    private val logger = LoggerFactory.getLogger(InvoiceRepository::class.java)
    private val tenantRepository = TenantRepository()
    private val auditLogRepository = AuditLogRepository()

    suspend fun create(
        tenantId: TenantId,
        clientId: ClientId,
        items: List<InvoiceItem>,
        issueDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        dueDate: LocalDate = issueDate.plus(30, DateTimeUnit.DAY),
        notes: String? = null,
        userId: BusinessUserId? = null
    ): InvoiceId {
        // Get next invoice number (this is a suspend function, called outside dbQuery)
        val invoiceNumber = tenantRepository.getNextInvoiceNumber(tenantId)

        // Calculate totals from domain model (Money values)
        val subtotal = items.sumOf { BigDecimal(it.lineTotal.value) }
        val vatTotal = items.sumOf { BigDecimal(it.vatAmount.value) }
        val total = subtotal + vatTotal

        val tenantJavaUuid = tenantId.value.toJavaUuid()
        val clientJavaUuid = clientId.value.toJavaUuid()

        val invoiceId = dbQuery {
            // Insert invoice
            val id = InvoicesTable.insertAndGetId {
                it[InvoicesTable.tenantId] = tenantJavaUuid
                it[InvoicesTable.clientId] = clientJavaUuid
                it[InvoicesTable.invoiceNumber] = invoiceNumber.value
                it[InvoicesTable.issueDate] = issueDate
                it[InvoicesTable.dueDate] = dueDate
                it[subtotalAmount] = subtotal
                it[vatAmount] = vatTotal
                it[totalAmount] = total
                it[paidAmount] = BigDecimal.ZERO
                it[status] = InvoiceStatus.Draft
                it[InvoicesTable.notes] = notes
            }.value

            // Insert items
            items.forEachIndexed { index, item ->
                InvoiceItemsTable.insert {
                    it[InvoiceItemsTable.invoiceId] = id
                    it[description] = item.description
                    it[quantity] = BigDecimal(item.quantity.value)
                    it[unitPrice] = BigDecimal(item.unitPrice.value)
                    it[vatRate] = BigDecimal(item.vatRate.value)
                    it[lineTotal] = BigDecimal(item.lineTotal.value)
                    it[InvoiceItemsTable.vatAmount] = BigDecimal(item.vatAmount.value)
                    it[sortOrder] = index
                }
            }
            InvoiceId(id.toKotlinUuid())
        }

        // Audit log (called outside dbQuery as it's a suspend function)
        auditLogRepository.log(
            tenantId = tenantId,
            userId = userId,
            action = AuditAction.InvoiceCreated,
            entityType = EntityType.Invoice,
            entityId = invoiceId.value.toString(),
            newValues = mapOf(
                "invoice_number" to invoiceNumber.value,
                "total" to total.toString(),
                "client_id" to clientId.toString()
            )
        )

        logger.info("Created invoice ${invoiceNumber.value} for tenant $tenantId")
        return invoiceId
    }

    suspend fun findById(invoiceId: InvoiceId, tenantId: TenantId): Invoice? = dbQuery {
        val invoiceJavaUuid = invoiceId.value.toJavaUuid()
        val tenantJavaUuid = tenantId.value.toJavaUuid()
        val invoice = InvoicesTable
            .selectAll()
            .where { (InvoicesTable.id eq invoiceJavaUuid) and (InvoicesTable.tenantId eq tenantJavaUuid) }
            .singleOrNull()
            ?.toInvoice()

        invoice?.let {
            val items = InvoiceItemsTable
                .selectAll()
                .where { InvoiceItemsTable.invoiceId eq invoiceJavaUuid }
                .orderBy(InvoiceItemsTable.sortOrder)
                .map { it.toInvoiceItem() }

            invoice.copy(items = items)
        }
    }

    suspend fun listByTenant(
        tenantId: TenantId,
        status: InvoiceStatus? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Long = 0
    ): List<Invoice> = dbQuery {
        val tenantJavaUuid = tenantId.value.toJavaUuid()
        var query = InvoicesTable
            .selectAll()
            .where { InvoicesTable.tenantId eq tenantJavaUuid }

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
        invoiceId: InvoiceId,
        tenantId: TenantId,
        newStatus: InvoiceStatus,
        userId: BusinessUserId? = null
    ) {
        val invoiceJavaUuid = invoiceId.value.toJavaUuid()
        val tenantJavaUuid = tenantId.value.toJavaUuid()
        val oldStatus = dbQuery {
            val oldInvoice = InvoicesTable
                .selectAll()
                .where { (InvoicesTable.id eq invoiceJavaUuid) and (InvoicesTable.tenantId eq tenantJavaUuid) }
                .singleOrNull() ?: throw IllegalArgumentException("Invoice not found")

            val status = oldInvoice[InvoicesTable.status]

            InvoicesTable.update({
                (InvoicesTable.id eq invoiceJavaUuid) and (InvoicesTable.tenantId eq tenantJavaUuid)
            }) {
                it[InvoicesTable.status] = newStatus
                if (newStatus == InvoiceStatus.Paid) {
                    it[paidAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }

            status
        }

        // Audit log (called outside dbQuery as it's a suspend function)
        auditLogRepository.log(
            tenantId = tenantId,
            userId = userId,
            action = AuditAction.InvoiceStatusChanged,
            entityType = EntityType.Invoice,
            entityId = invoiceId.value.toString(),
            oldValues = mapOf("status" to oldStatus.dbValue),
            newValues = mapOf("status" to newStatus.dbValue)
        )
    }

    suspend fun recordPayment(
        invoiceId: InvoiceId,
        tenantId: TenantId,
        amount: Money,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        transactionId: TransactionId? = null,
        notes: String? = null,
        userId: BusinessUserId? = null
    ): PaymentId {
        val invoiceJavaUuid = invoiceId.value.toJavaUuid()
        val tenantJavaUuid = tenantId.value.toJavaUuid()
        val amountDecimal = BigDecimal(amount.value)
        val paymentId = dbQuery {
            // Verify invoice belongs to tenant
            val invoice = InvoicesTable
                .selectAll()
                .where { (InvoicesTable.id eq invoiceJavaUuid) and (InvoicesTable.tenantId eq tenantJavaUuid) }
                .singleOrNull() ?: throw IllegalArgumentException("Invoice not found")

            // Record payment
            val id = PaymentsTable.insertAndGetId {
                it[PaymentsTable.tenantId] = tenantJavaUuid
                it[PaymentsTable.invoiceId] = invoiceJavaUuid
                it[PaymentsTable.amount] = amountDecimal
                it[PaymentsTable.paymentDate] = paymentDate
                it[PaymentsTable.paymentMethod] = paymentMethod
                it[PaymentsTable.transactionId] = transactionId?.value
                it[PaymentsTable.notes] = notes
            }.value

            // Update invoice
            val newPaidAmount = invoice[InvoicesTable.paidAmount] + amountDecimal
            val totalAmount = invoice[InvoicesTable.totalAmount]
            val newStatus = when {
                newPaidAmount >= totalAmount -> InvoiceStatus.Paid
                newPaidAmount > BigDecimal.ZERO -> InvoiceStatus.PartiallyPaid
                else -> invoice[InvoicesTable.status]
            }

            InvoicesTable.update({ InvoicesTable.id eq invoiceJavaUuid }) {
                it[paidAmount] = newPaidAmount
                it[status] = newStatus
                if (newStatus == InvoiceStatus.Paid) {
                    it[paidAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }

            PaymentId(id.toKotlinUuid())
        }

        // Audit log (called outside dbQuery as it's a suspend function)
        auditLogRepository.log(
            tenantId = tenantId,
            userId = userId,
            action = AuditAction.PaymentRecorded,
            entityType = EntityType.Payment,
            entityId = paymentId.value.toString(),
            newValues = mapOf(
                "amount" to amount.value,
                "invoice_id" to invoiceId.toString(),
                "method" to paymentMethod.dbValue
            )
        )

        logger.info("Recorded payment of ${amount.value} for invoice $invoiceId")
        return paymentId
    }
}