package ai.dokus.foundation.database.services

import ai.dokus.foundation.database.mappers.InvoiceMapper.toInvoice
import ai.dokus.foundation.database.mappers.InvoiceMapper.toInvoiceItem
import ai.dokus.foundation.database.tables.InvoiceItemsTable
import ai.dokus.foundation.database.tables.InvoicesTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.InvoiceId
import ai.dokus.foundation.domain.InvoiceNumber
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.InvoiceItem
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.domain.model.UpdateInvoiceStatusRequest
import ai.dokus.foundation.ktor.services.InvoiceService
import ai.dokus.foundation.ktor.services.TenantService
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
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
class InvoiceServiceImpl(
    private val tenantService: TenantService
) : InvoiceService {
    private val logger = LoggerFactory.getLogger(InvoiceServiceImpl::class.java)

    override suspend fun create(request: CreateInvoiceRequest): Invoice {
        val invoiceNumber = tenantService.getNextInvoiceNumber(request.tenantId)
        val (subtotal, vatTotal, total) = calculateTotals(request.items)
        val today = Clock.System.todayIn(TimeZone.UTC)

        val invoiceId = dbQuery {
            val id = InvoicesTable.insertAndGetId {
                it[tenantId] = request.tenantId.value.toJavaUuid()
                it[clientId] = request.clientId.value.toJavaUuid()
                it[InvoicesTable.invoiceNumber] = invoiceNumber.value
                it[issueDate] = request.issueDate ?: today
                it[dueDate] = request.dueDate ?: today.plus(DatePeriod(days = 30))
                it[subtotalAmount] = BigDecimal(subtotal.value)
                it[InvoicesTable.vatAmount] = BigDecimal(vatTotal.value)
                it[totalAmount] = BigDecimal(total.value)
                it[paidAmount] = BigDecimal.ZERO
                it[status] = InvoiceStatus.Draft
                it[notes] = request.notes
            }.value

            request.items.forEachIndexed { index, item ->
                val lineTotal = Money((BigDecimal(item.quantity.value) * BigDecimal(item.unitPrice.value)).toString())
                val itemVatAmount = Money((BigDecimal(lineTotal.value) * BigDecimal(item.vatRate.value) / BigDecimal("100")).toString())

                InvoiceItemsTable.insert {
                    it[InvoiceItemsTable.invoiceId] = id
                    it[description] = item.description
                    it[quantity] = BigDecimal(item.quantity.value)
                    it[unitPrice] = BigDecimal(item.unitPrice.value)
                    it[vatRate] = BigDecimal(item.vatRate.value)
                    it[InvoiceItemsTable.lineTotal] = BigDecimal(lineTotal.value)
                    it[InvoiceItemsTable.vatAmount] = BigDecimal(itemVatAmount.value)
                    it[sortOrder] = index
                }
            }

            id
        }

        logger.info("Created invoice $invoiceNumber for tenant ${request.tenantId}")
        return getInvoiceWithItems(invoiceId.toKotlinUuid())
    }

    override suspend fun update(
        invoiceId: InvoiceId,
        issueDate: LocalDate?,
        dueDate: LocalDate?,
        notes: String?,
        termsAndConditions: String?
    ) = dbQuery {
        val javaUuid = invoiceId.value.toJavaUuid()

        val invoice = InvoicesTable.selectAll().where { InvoicesTable.id eq javaUuid }.singleOrNull()
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

        if (invoice[InvoicesTable.status] != InvoiceStatus.Draft) {
            throw IllegalArgumentException("Can only update draft invoices")
        }

        InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
            if (issueDate != null) it[InvoicesTable.issueDate] = issueDate
            if (dueDate != null) it[InvoicesTable.dueDate] = dueDate
            if (notes != null) it[InvoicesTable.notes] = notes
            if (termsAndConditions != null) it[InvoicesTable.termsAndConditions] = termsAndConditions
        }

        logger.info("Updated invoice $invoiceId")
    }

    override suspend fun updateItems(invoiceId: InvoiceId, items: List<InvoiceItem>) {
        val (subtotal, vatTotal, total) = calculateTotals(items)

        dbQuery {
            val javaUuid = invoiceId.value.toJavaUuid()

            val invoice = InvoicesTable.selectAll().where { InvoicesTable.id eq javaUuid }.singleOrNull()
                ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

            if (invoice[InvoicesTable.status] != InvoiceStatus.Draft) {
                throw IllegalArgumentException("Can only update items for draft invoices")
            }

            InvoiceItemsTable.deleteWhere { InvoiceItemsTable.invoiceId eq javaUuid }

            items.forEachIndexed { index, item ->
                val lineTotal = Money((BigDecimal(item.quantity.value) * BigDecimal(item.unitPrice.value)).toString())
                val itemVatAmount = Money((BigDecimal(lineTotal.value) * BigDecimal(item.vatRate.value) / BigDecimal("100")).toString())

                InvoiceItemsTable.insert {
                    it[InvoiceItemsTable.invoiceId] = javaUuid
                    it[description] = item.description
                    it[quantity] = BigDecimal(item.quantity.value)
                    it[unitPrice] = BigDecimal(item.unitPrice.value)
                    it[vatRate] = BigDecimal(item.vatRate.value)
                    it[InvoiceItemsTable.lineTotal] = BigDecimal(lineTotal.value)
                    it[InvoiceItemsTable.vatAmount] = BigDecimal(itemVatAmount.value)
                    it[sortOrder] = index
                }
            }

            InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
                it[subtotalAmount] = BigDecimal(subtotal.value)
                it[InvoicesTable.vatAmount] = BigDecimal(vatTotal.value)
                it[totalAmount] = BigDecimal(total.value)
            }
        }

        logger.info("Updated items for invoice $invoiceId")
    }

    override suspend fun delete(invoiceId: InvoiceId) = dbQuery {
        val javaUuid = invoiceId.value.toJavaUuid()

        val invoice = InvoicesTable.selectAll().where { InvoicesTable.id eq javaUuid }.singleOrNull()
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

        if (invoice[InvoicesTable.status] != InvoiceStatus.Draft) {
            throw IllegalArgumentException("Can only delete draft invoices")
        }

        InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
            it[status] = InvoiceStatus.Cancelled
        }

        logger.info("Cancelled invoice $invoiceId")
    }

    override suspend fun findById(id: InvoiceId): Invoice? = dbQuery {
        getInvoiceWithItems(id.value)
    }

    override suspend fun listByTenant(
        tenantId: TenantId,
        status: InvoiceStatus?,
        clientId: ClientId?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int?,
        offset: Int?
    ): List<Invoice> = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        var query = InvoicesTable.selectAll().where { InvoicesTable.tenantId eq javaUuid }

        if (status != null) query = query.andWhere { InvoicesTable.status eq status }
        if (clientId != null) query = query.andWhere { InvoicesTable.clientId eq clientId.value.toJavaUuid() }
        if (fromDate != null) query = query.andWhere { InvoicesTable.issueDate greaterEq fromDate }
        if (toDate != null) query = query.andWhere { InvoicesTable.issueDate lessEq toDate }
        if (limit != null) query = query.limit(limit)
        if (offset != null) query = query.offset(offset.toLong())

        query.orderBy(InvoicesTable.issueDate to SortOrder.DESC)
            .map { row ->
                val invoice = row.toInvoice()
                val items = getInvoiceItems(invoice.id.value)
                invoice.copy(items = items)
            }
    }

    override suspend fun listByClient(clientId: ClientId, status: InvoiceStatus?): List<Invoice> = dbQuery {
        var query = InvoicesTable.selectAll().where { InvoicesTable.clientId eq clientId.value.toJavaUuid() }
        if (status != null) query = query.andWhere { InvoicesTable.status eq status }

        query.map { row ->
            val invoice = row.toInvoice()
            val items = getInvoiceItems(invoice.id.value)
            invoice.copy(items = items)
        }
    }

    override suspend fun listOverdue(tenantId: TenantId): List<Invoice> = dbQuery {
        InvoicesTable.selectAll()
            .where { InvoicesTable.tenantId eq tenantId.value.toJavaUuid() }
            .andWhere { InvoicesTable.status eq InvoiceStatus.Sent }
            .andWhere { InvoicesTable.dueDate less kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.UTC) }
            .map { row ->
                val invoice = row.toInvoice()
                val items = getInvoiceItems(invoice.id.value)
                invoice.copy(items = items)
            }
    }

    override suspend fun updateStatus(request: UpdateInvoiceStatusRequest) = dbQuery {
        val javaUuid = request.invoiceId.value.toJavaUuid()
        InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
            it[status] = request.status
        }

        logger.info("Updated status for invoice ${request.invoiceId} to ${request.status}")
    }

    override suspend fun recordPayment(request: RecordPaymentRequest) = dbQuery {
        val javaUuid = request.invoiceId.value.toJavaUuid()
        val invoice = InvoicesTable.selectAll().where { InvoicesTable.id eq javaUuid }.singleOrNull()
            ?: throw IllegalArgumentException("Invoice not found: ${request.invoiceId}")

        val currentPaid = invoice[InvoicesTable.paidAmount]
        val newPaid = currentPaid + BigDecimal(request.amount.value)
        val total = invoice[InvoicesTable.totalAmount]

        InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
            it[paidAmount] = newPaid
            if (newPaid >= total) {
                it[status] = InvoiceStatus.Paid
                it[paidAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }
        }

        logger.info("Recorded payment of ${request.amount} for invoice ${request.invoiceId}")
    }

    override suspend fun sendViaEmail(
        invoiceId: InvoiceId,
        recipientEmail: String?,
        ccEmails: List<String>?,
        message: String?
    ) {
        logger.info("Sending invoice $invoiceId via email to $recipientEmail")
        throw NotImplementedError("Email sending integration not yet implemented")
    }

    override suspend fun sendViaPeppol(invoiceId: InvoiceId) {
        logger.info("Sending invoice $invoiceId via Peppol")
        throw NotImplementedError("Peppol integration not yet implemented")
    }

    override suspend fun generatePDF(invoiceId: InvoiceId): ByteArray {
        logger.info("Generating PDF for invoice $invoiceId")
        throw NotImplementedError("PDF generation not yet implemented")
    }

    override suspend fun generatePaymentLink(invoiceId: InvoiceId, expiresAt: Instant?): String {
        logger.info("Generating payment link for invoice $invoiceId")
        throw NotImplementedError("Payment link generation not yet implemented")
    }

    override suspend fun markAsSent(invoiceId: InvoiceId) = dbQuery {
        val javaUuid = invoiceId.value.toJavaUuid()
        InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
            it[status] = InvoiceStatus.Sent
        }

        logger.info("Marked invoice $invoiceId as sent")
    }

    override fun watchInvoices(tenantId: TenantId): Flow<Invoice> {
        throw NotImplementedError("Flow-based invoice streaming not yet implemented")
    }

    override suspend fun calculateTotals(items: List<InvoiceItem>): Triple<Money, Money, Money> {
        var subtotal = BigDecimal.ZERO
        var vatTotal = BigDecimal.ZERO

        items.forEach { item ->
            val lineTotal = BigDecimal(item.quantity.value) * BigDecimal(item.unitPrice.value)
            val vatAmount = lineTotal * BigDecimal(item.vatRate.value) / BigDecimal("100")
            subtotal += lineTotal
            vatTotal += vatAmount
        }

        val total = subtotal + vatTotal

        return Triple(
            Money(subtotal.toString()),
            Money(vatTotal.toString()),
            Money(total.toString())
        )
    }

    override suspend fun getStatistics(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Map<String, Money> {
        throw NotImplementedError("Statistics calculation not yet implemented")
    }

    private fun getInvoiceWithItems(invoiceId: kotlin.uuid.Uuid): Invoice {
        val invoice = InvoicesTable.selectAll()
            .where { InvoicesTable.id eq invoiceId.toJavaUuid() }
            .singleOrNull()
            ?.toInvoice()
            ?: return Invoice(
                id = InvoiceId(invoiceId),
                tenantId = TenantId(kotlin.uuid.Uuid.random()),
                clientId = ClientId(kotlin.uuid.Uuid.random()),
                invoiceNumber = InvoiceNumber(""),
                issueDate = Clock.System.todayIn(TimeZone.UTC),
                dueDate = Clock.System.todayIn(TimeZone.UTC),
                subtotalAmount = Money("0"),
                vatAmount = Money("0"),
                totalAmount = Money("0"),
                paidAmount = Money("0"),
                status = InvoiceStatus.Draft,
                paidAt = null,
                notes = null,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                items = emptyList()
            )

        val items = getInvoiceItems(invoiceId)
        return invoice.copy(items = items)
    }

    private fun getInvoiceItems(invoiceId: kotlin.uuid.Uuid): List<InvoiceItem> {
        return InvoiceItemsTable.selectAll()
            .where { InvoiceItemsTable.invoiceId eq invoiceId.toJavaUuid() }
            .orderBy(InvoiceItemsTable.sortOrder)
            .map { it.toInvoiceItem() }
    }
}
