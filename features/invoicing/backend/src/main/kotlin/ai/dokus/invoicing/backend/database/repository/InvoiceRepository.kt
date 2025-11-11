package ai.dokus.invoicing.backend.database.repository

import ai.dokus.invoicing.backend.database.mappers.InvoiceMapper.toInvoice
import ai.dokus.invoicing.backend.database.mappers.InvoiceMapper.toInvoiceItem
import ai.dokus.invoicing.backend.database.tables.InvoiceItemsTable
import ai.dokus.invoicing.backend.database.tables.InvoicesTable
import ai.dokus.foundation.ktor.database.dbQuery
import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.InvoiceId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.InvoiceStatus
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class InvoiceRepository {
    private val logger = LoggerFactory.getLogger(InvoiceRepository::class.java)

    suspend fun create(
        tenantId: TenantId,
        clientId: ClientId,
        invoiceNumber: String,
        items: List<InvoiceItem>,
        issueDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        dueDate: LocalDate = issueDate.plus(30, DateTimeUnit.DAY),
        notes: String? = null
    ): InvoiceId {

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
                it[InvoicesTable.invoiceNumber] = invoiceNumber
                it[InvoicesTable.issueDate] = issueDate
                it[InvoicesTable.dueDate] = dueDate
                it[subtotalAmount] = subtotal
                it[vatAmount] = vatTotal
                it[totalAmount] = total
                it[paidAmount] = BigDecimal.ZERO
                it[status] = InvoiceStatus.Draft
                it[InvoicesTable.notes] = notes
            }.value.toKotlinUuid()

            // Insert items
            items.forEachIndexed { index, item ->
                InvoiceItemsTable.insert {
                    it[InvoiceItemsTable.invoiceId] = id.toJavaUuid()
                    it[description] = item.description
                    it[quantity] = BigDecimal(item.quantity.value)
                    it[unitPrice] = BigDecimal(item.unitPrice.value)
                    it[vatRate] = BigDecimal(item.vatRate.value)
                    it[lineTotal] = BigDecimal(item.lineTotal.value)
                    it[InvoiceItemsTable.vatAmount] = BigDecimal(item.vatAmount.value)
                    it[sortOrder] = index
                }
            }
            id
        }

        logger.info("Created invoice $invoiceNumber for tenant $tenantId")
        return InvoiceId(invoiceId)
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
        newStatus: InvoiceStatus
    ): InvoiceStatus {
        val invoiceJavaUuid = invoiceId.value.toJavaUuid()
        val tenantJavaUuid = tenantId.value.toJavaUuid()
        return dbQuery {
            val oldInvoice = InvoicesTable
                .selectAll()
                .where { (InvoicesTable.id eq invoiceJavaUuid) and (InvoicesTable.tenantId eq tenantJavaUuid) }
                .singleOrNull() ?: throw IllegalArgumentException("Invoice not found")

            val oldStatus = oldInvoice[InvoicesTable.status]

            InvoicesTable.update({
                (InvoicesTable.id eq invoiceJavaUuid) and (InvoicesTable.tenantId eq tenantJavaUuid)
            }) {
                it[InvoicesTable.status] = newStatus
                if (newStatus == InvoiceStatus.Paid) {
                    it[paidAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }

            oldStatus
        }
    }

    suspend fun updatePaidAmount(
        invoiceId: InvoiceId,
        tenantId: TenantId,
        paidAmount: Money
    ) {
        val invoiceJavaUuid = invoiceId.value.toJavaUuid()
        val tenantJavaUuid = tenantId.value.toJavaUuid()
        val amountDecimal = BigDecimal(paidAmount.value)

        dbQuery {
            // Verify invoice belongs to tenant
            val invoice = InvoicesTable
                .selectAll()
                .where { (InvoicesTable.id eq invoiceJavaUuid) and (InvoicesTable.tenantId eq tenantJavaUuid) }
                .singleOrNull() ?: throw IllegalArgumentException("Invoice not found")

            val totalAmount = invoice[InvoicesTable.totalAmount]
            val newStatus = when {
                amountDecimal >= totalAmount -> InvoiceStatus.Paid
                amountDecimal > BigDecimal.ZERO -> InvoiceStatus.PartiallyPaid
                else -> InvoiceStatus.Draft
            }

            InvoicesTable.update({
                (InvoicesTable.id eq invoiceJavaUuid) and (InvoicesTable.tenantId eq tenantJavaUuid)
            }) {
                it[InvoicesTable.paidAmount] = amountDecimal
                it[status] = newStatus
                if (newStatus == InvoiceStatus.Paid) {
                    it[paidAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }
        }

        logger.info("Updated paid amount for invoice $invoiceId to ${paidAmount.value}")
    }
}