package ai.dokus.foundation.database.services

import ai.dokus.foundation.database.mappers.InvoiceMapper.toInvoice
import ai.dokus.foundation.database.mappers.InvoiceMapper.toInvoiceItem
import ai.dokus.foundation.database.tables.InvoiceItemsTable
import ai.dokus.foundation.database.tables.InvoicesTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.InvoiceItem
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.domain.model.UpdateInvoiceStatusRequest
import ai.dokus.foundation.ktor.services.InvoiceService
import ai.dokus.foundation.ktor.services.TenantService
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
class InvoiceServiceImpl(
    private val tenantService: TenantService
) : InvoiceService {
    private val logger = LoggerFactory.getLogger(InvoiceServiceImpl::class.java)

    // Shared flow for real-time invoice updates
    private val invoiceUpdates = MutableSharedFlow<Invoice>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun create(request: CreateInvoiceRequest): Invoice = dbQuery {
        // Generate invoice number
        val invoiceNumber = tenantService.getNextInvoiceNumber(request.tenantId)

        // Calculate totals
        val (subtotal, vatTotal, total) = calculateTotals(request.items)

        // Insert invoice
        val invoiceId = InvoicesTable.insertAndGetId {
            it[tenantId] = request.tenantId.value.toJavaUuid()
            it[clientId] = request.clientId.value.toJavaUuid()
            it[InvoicesTable.invoiceNumber] = invoiceNumber.value
            it[issueDate] = kotlinx.datetime.toJavaLocalDate(request.issueDate)
            it[dueDate] = kotlinx.datetime.toJavaLocalDate(request.dueDate)
            it[subtotalAmount] = BigDecimal(subtotal.amount)
            it[vatAmount] = BigDecimal(vatTotal.amount)
            it[totalAmount] = BigDecimal(total.amount)
            it[paidAmount] = BigDecimal.ZERO
            it[status] = InvoiceStatus.Draft
            it[notes] = request.notes
            it[termsAndConditions] = request.termsAndConditions
        }.value

        // Insert invoice items
        request.items.forEachIndexed { index, item ->
            val lineTotal = Money((BigDecimal(item.quantity.value) * BigDecimal(item.unitPrice.amount)).toString())
            val vatAmount = Money((BigDecimal(lineTotal.amount) * BigDecimal(item.vatRate.value) / BigDecimal("100")).toString())

            InvoiceItemsTable.insert {
                it[InvoiceItemsTable.invoiceId] = invoiceId
                it[description] = item.description
                it[quantity] = BigDecimal(item.quantity.value)
                it[unitPrice] = BigDecimal(item.unitPrice.amount)
                it[vatRate] = BigDecimal(item.vatRate.value)
                it[InvoiceItemsTable.lineTotal] = BigDecimal(lineTotal.amount)
                it[InvoiceItemsTable.vatAmount] = BigDecimal(vatAmount.amount)
                it[sortOrder] = index
            }
        }

        logger.info("Created invoice $invoiceNumber for tenant ${request.tenantId}")

        // Fetch and return complete invoice
        val invoice = getInvoiceWithItems(invoiceId.toKotlinUuid())
        invoiceUpdates.emit(invoice)
        invoice
    }

    override suspend fun update(
        invoiceId: InvoiceId,
        issueDate: LocalDate?,
        dueDate: LocalDate?,
        notes: String?,
        termsAndConditions: String?
    ) = dbQuery {
        val javaUuid = invoiceId.value.toJavaUuid()

        // Check if invoice is draft
        val invoice = InvoicesTable.selectAll().where { InvoicesTable.id eq javaUuid }.singleOrNull()
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

        if (invoice[InvoicesTable.status] != InvoiceStatus.Draft) {
            throw IllegalArgumentException("Can only update draft invoices")
        }

        val updated = InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
            if (issueDate != null) it[InvoicesTable.issueDate] = kotlinx.datetime.toJavaLocalDate(issueDate)
            if (dueDate != null) it[InvoicesTable.dueDate] = kotlinx.datetime.toJavaLocalDate(dueDate)
            if (notes != null) it[InvoicesTable.notes] = notes
            if (termsAndConditions != null) it[InvoicesTable.termsAndConditions] = termsAndConditions
        }

        if (updated > 0) {
            logger.info("Updated invoice $invoiceId")
            val updatedInvoice = getInvoiceWithItems(invoiceId.value)
            invoiceUpdates.emit(updatedInvoice)
        }
    }

    override suspend fun updateItems(invoiceId: InvoiceId, items: List<InvoiceItem>) = dbQuery {
        val javaUuid = invoiceId.value.toJavaUuid()

        // Check if invoice is draft
        val invoice = InvoicesTable.selectAll().where { InvoicesTable.id eq javaUuid }.singleOrNull()
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

        if (invoice[InvoicesTable.status] != InvoiceStatus.Draft) {
            throw IllegalArgumentException("Can only update items for draft invoices")
        }

        // Delete existing items
        InvoiceItemsTable.deleteWhere { InvoiceItemsTable.invoiceId eq javaUuid }

        // Insert new items
        items.forEachIndexed { index, item ->
            val lineTotal = Money((BigDecimal(item.quantity.value) * BigDecimal(item.unitPrice.amount)).toString())
            val vatAmount = Money((BigDecimal(lineTotal.amount) * BigDecimal(item.vatRate.value) / BigDecimal("100")).toString())

            InvoiceItemsTable.insert {
                it[InvoiceItemsTable.invoiceId] = javaUuid
                it[description] = item.description
                it[quantity] = BigDecimal(item.quantity.value)
                it[unitPrice] = BigDecimal(item.unitPrice.amount)
                it[vatRate] = BigDecimal(item.vatRate.value)
                it[InvoiceItemsTable.lineTotal] = BigDecimal(lineTotal.amount)
                it[InvoiceItemsTable.vatAmount] = BigDecimal(vatAmount.amount)
                it[sortOrder] = index
            }
        }

        // Recalculate totals
        val (subtotal, vatTotal, total) = calculateTotals(items)
        InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
            it[subtotalAmount] = BigDecimal(subtotal.amount)
            it[vatAmount] = BigDecimal(vatTotal.amount)
            it[totalAmount] = BigDecimal(total.amount)
        }

        logger.info("Updated items for invoice $invoiceId")
        val updatedInvoice = getInvoiceWithItems(invoiceId.value)
        invoiceUpdates.emit(updatedInvoice)
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
        if (fromDate != null) query = query.andWhere { InvoicesTable.issueDate greaterEq kotlinx.datetime.toJavaLocalDate(fromDate) }
        if (toDate != null) query = query.andWhere { InvoicesTable.issueDate lessEq kotlinx.datetime.toJavaLocalDate(toDate) }
        if (limit != null) query = query.limit(limit)
        if (offset != null) query = query.limit(limit ?: 100, offset.toLong())

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
            .andWhere { InvoicesTable.dueDate less kotlinx.datetime.toJavaLocalDate(kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.UTC)) }
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
        val updatedInvoice = getInvoiceWithItems(request.invoiceId.value)
        invoiceUpdates.emit(updatedInvoice)
    }

    override suspend fun recordPayment(request: RecordPaymentRequest) = dbQuery {
        val javaUuid = request.invoiceId.value.toJavaUuid()
        val invoice = InvoicesTable.selectAll().where { InvoicesTable.id eq javaUuid }.singleOrNull()
            ?: throw IllegalArgumentException("Invoice not found: ${request.invoiceId}")

        val currentPaid = invoice[InvoicesTable.paidAmount]
        val newPaid = currentPaid + BigDecimal(request.amount.amount)
        val total = invoice[InvoicesTable.totalAmount]

        InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
            it[paidAmount] = newPaid
            if (newPaid >= total) {
                it[status] = InvoiceStatus.Paid
                it[paidAt] = kotlinx.datetime.toJavaInstant(kotlinx.datetime.Clock.System.now())
            }
        }

        logger.info("Recorded payment of ${request.amount} for invoice ${request.invoiceId}")
        val updatedInvoice = getInvoiceWithItems(request.invoiceId.value)
        invoiceUpdates.emit(updatedInvoice)
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
        return invoiceUpdates
    }

    override suspend fun calculateTotals(items: List<InvoiceItem>): Triple<Money, Money, Money> {
        var subtotal = BigDecimal.ZERO
        var vatTotal = BigDecimal.ZERO

        items.forEach { item ->
            val lineTotal = BigDecimal(item.quantity.value) * BigDecimal(item.unitPrice.amount)
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

    private suspend fun getInvoiceWithItems(invoiceId: kotlin.uuid.Uuid): Invoice {
        val invoice = InvoicesTable.selectAll()
            .where { InvoicesTable.id eq invoiceId.toJavaUuid() }
            .singleOrNull()
            ?.toInvoice()
            ?: return Invoice(
                id = InvoiceId(invoiceId),
                tenantId = TenantId(kotlin.uuid.Uuid.random()),
                clientId = ClientId(kotlin.uuid.Uuid.random()),
                invoiceNumber = InvoiceNumber(""),
                issueDate = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.UTC),
                dueDate = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.UTC),
                subtotalAmount = Money("0"),
                vatAmount = Money("0"),
                totalAmount = Money("0"),
                paidAmount = Money("0"),
                status = InvoiceStatus.Draft,
                paidAt = null,
                notes = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now(),
                items = emptyList()
            )

        val items = getInvoiceItems(invoiceId)
        return invoice.copy(items = items)
    }

    private suspend fun getInvoiceItems(invoiceId: kotlin.uuid.Uuid) = dbQuery {
        InvoiceItemsTable.selectAll()
            .where { InvoiceItemsTable.invoiceId eq invoiceId.toJavaUuid() }
            .orderBy(InvoiceItemsTable.sortOrder)
            .map { it.toInvoiceItem() }
    }
}
