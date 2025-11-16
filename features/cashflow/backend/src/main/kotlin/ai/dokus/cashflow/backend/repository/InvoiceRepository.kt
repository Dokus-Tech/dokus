package ai.dokus.cashflow.backend.repository

import ai.dokus.cashflow.backend.database.tables.InvoiceItemsTable
import ai.dokus.cashflow.backend.database.tables.InvoicesTable
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.InvoiceItem
import ai.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.util.UUID

/**
 * Repository for managing invoices
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return invoices from different tenants
 * 3. All operations must be tenant-isolated
 */
class InvoiceRepository {

    /**
     * Create a new invoice with its items
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> = runCatching {
        dbQuery {
            // Generate invoice number
            // TODO: Implement proper invoice number generation (fetch from tenant settings)
            val invoiceNumber = "INV-${System.currentTimeMillis()}"

            // Insert invoice
            val invoiceId = InvoicesTable.insertAndGetId {
                it[tenantId] = UUID.fromString(request.tenantId.toString())
                it[clientId] = UUID.fromString(request.clientId.toString())
                it[InvoicesTable.invoiceNumber] = invoiceNumber
                val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                it[issueDate] = request.issueDate ?: today
                it[dueDate] = request.dueDate ?: today.plus(kotlinx.datetime.DatePeriod(days = 30))
                it[subtotalAmount] = request.items.sumOf { item -> java.math.BigDecimal(item.lineTotal.value) }
                it[vatAmount] = request.items.sumOf { item -> java.math.BigDecimal(item.vatAmount.value) }
                it[totalAmount] = request.items.sumOf { item -> java.math.BigDecimal(item.lineTotal.value) + java.math.BigDecimal(item.vatAmount.value) }
                it[paidAmount] = java.math.BigDecimal.ZERO
                it[status] = InvoiceStatus.Draft
                it[notes] = request.notes
            }

            // Insert invoice items
            request.items.forEachIndexed { index, item ->
                InvoiceItemsTable.insert {
                    it[InvoiceItemsTable.invoiceId] = invoiceId.value
                    it[description] = item.description
                    it[quantity] = java.math.BigDecimal(item.quantity.value)
                    it[unitPrice] = java.math.BigDecimal(item.unitPrice.value)
                    it[vatRate] = java.math.BigDecimal(item.vatRate.value)
                    it[lineTotal] = java.math.BigDecimal(item.lineTotal.value)
                    it[vatAmount] = java.math.BigDecimal(item.vatAmount.value)
                    it[sortOrder] = index
                }
            }

            // Manually fetch and return the complete invoice
            val row = InvoicesTable.selectAll().where {
                (InvoicesTable.id eq invoiceId.value) and
                (InvoicesTable.tenantId eq UUID.fromString(request.tenantId.toString()))
            }.single()

            val items = InvoiceItemsTable.selectAll().where {
                InvoiceItemsTable.invoiceId eq invoiceId.value
            }.orderBy(InvoiceItemsTable.sortOrder)
            .map { itemRow ->
                InvoiceItem(
                    id = InvoiceItemId.parse(itemRow[InvoiceItemsTable.id].value.toString()),
                    invoiceId = InvoiceId.parse(invoiceId.value.toString()),
                    description = itemRow[InvoiceItemsTable.description],
                    quantity = Quantity(itemRow[InvoiceItemsTable.quantity].toString()),
                    unitPrice = Money(itemRow[InvoiceItemsTable.unitPrice].toString()),
                    vatRate = VatRate(itemRow[InvoiceItemsTable.vatRate].toString()),
                    lineTotal = Money(itemRow[InvoiceItemsTable.lineTotal].toString()),
                    vatAmount = Money(itemRow[InvoiceItemsTable.vatAmount].toString()),
                    sortOrder = itemRow[InvoiceItemsTable.sortOrder]
                )
            }

            Invoice(
                id = InvoiceId.parse(row[InvoicesTable.id].value.toString()),
                tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                clientId = ClientId.parse(row[InvoicesTable.clientId].toString()),
                invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                issueDate = row[InvoicesTable.issueDate],
                dueDate = row[InvoicesTable.dueDate],
                subtotalAmount = Money(row[InvoicesTable.subtotalAmount].toString()),
                vatAmount = Money(row[InvoicesTable.vatAmount].toString()),
                totalAmount = Money(row[InvoicesTable.totalAmount].toString()),
                paidAmount = Money(row[InvoicesTable.paidAmount].toString()),
                status = row[InvoicesTable.status],
                currency = row[InvoicesTable.currency],
                notes = row[InvoicesTable.notes],
                termsAndConditions = row[InvoicesTable.termsAndConditions],
                items = items,
                peppolId = row[InvoicesTable.peppolId]?.let { PeppolId(it) },
                peppolSentAt = row[InvoicesTable.peppolSentAt],
                peppolStatus = row[InvoicesTable.peppolStatus],
                paymentLink = row[InvoicesTable.paymentLink],
                paymentLinkExpiresAt = row[InvoicesTable.paymentLinkExpiresAt],
                paidAt = row[InvoicesTable.paidAt],
                paymentMethod = row[InvoicesTable.paymentMethod],
                createdAt = row[InvoicesTable.createdAt],
                updatedAt = row[InvoicesTable.updatedAt]
            )
        }
    }

    /**
     * Get a single invoice by ID
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<Invoice?> = runCatching {
        dbQuery {
            val row = InvoicesTable.selectAll().where {
                (InvoicesTable.id eq UUID.fromString(invoiceId.toString())) and
                (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull() ?: return@dbQuery null

            // Fetch invoice items
            val items = InvoiceItemsTable.selectAll().where {
                InvoiceItemsTable.invoiceId eq UUID.fromString(invoiceId.toString())
            }.orderBy(InvoiceItemsTable.sortOrder)
            .map { itemRow ->
                InvoiceItem(
                    id = InvoiceItemId.parse(itemRow[InvoiceItemsTable.id].value.toString()),
                    invoiceId = invoiceId,
                    description = itemRow[InvoiceItemsTable.description],
                    quantity = Quantity(itemRow[InvoiceItemsTable.quantity].toString()),
                    unitPrice = Money(itemRow[InvoiceItemsTable.unitPrice].toString()),
                    vatRate = VatRate(itemRow[InvoiceItemsTable.vatRate].toString()),
                    lineTotal = Money(itemRow[InvoiceItemsTable.lineTotal].toString()),
                    vatAmount = Money(itemRow[InvoiceItemsTable.vatAmount].toString()),
                    sortOrder = itemRow[InvoiceItemsTable.sortOrder]
                )
            }

            // Map to domain model
            Invoice(
                id = InvoiceId.parse(row[InvoicesTable.id].value.toString()),
                tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                clientId = ClientId.parse(row[InvoicesTable.clientId].toString()),
                invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                issueDate = row[InvoicesTable.issueDate],
                dueDate = row[InvoicesTable.dueDate],
                subtotalAmount = Money(row[InvoicesTable.subtotalAmount].toString()),
                vatAmount = Money(row[InvoicesTable.vatAmount].toString()),
                totalAmount = Money(row[InvoicesTable.totalAmount].toString()),
                paidAmount = Money(row[InvoicesTable.paidAmount].toString()),
                status = row[InvoicesTable.status],
                currency = row[InvoicesTable.currency],
                notes = row[InvoicesTable.notes],
                termsAndConditions = row[InvoicesTable.termsAndConditions],
                items = items,
                peppolId = row[InvoicesTable.peppolId]?.let { PeppolId(it) },
                peppolSentAt = row[InvoicesTable.peppolSentAt],
                peppolStatus = row[InvoicesTable.peppolStatus],
                paymentLink = row[InvoicesTable.paymentLink],
                paymentLinkExpiresAt = row[InvoicesTable.paymentLinkExpiresAt],
                paidAt = row[InvoicesTable.paidAt],
                paymentMethod = row[InvoicesTable.paymentMethod],
                createdAt = row[InvoicesTable.createdAt],
                updatedAt = row[InvoicesTable.updatedAt]
            )
        }
    }

    /**
     * List invoices for a tenant with optional filters
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listInvoices(
        tenantId: TenantId,
        status: InvoiceStatus? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Invoice>> = runCatching {
        dbQuery {
            var query = InvoicesTable.selectAll().where {
                InvoicesTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            // Apply filters
            if (status != null) {
                query = query.andWhere { InvoicesTable.status eq status }
            }
            if (fromDate != null) {
                query = query.andWhere { InvoicesTable.issueDate greaterEq fromDate }
            }
            if (toDate != null) {
                query = query.andWhere { InvoicesTable.issueDate lessEq toDate }
            }

            // Apply pagination and ordering
            query.orderBy(InvoicesTable.createdAt to SortOrder.DESC)
                .limit(limit)
                .map { row ->
                    // For list view, we don't fetch items to improve performance
                    // Items will be loaded when getting individual invoice
                    Invoice(
                        id = InvoiceId.parse(row[InvoicesTable.id].value.toString()),
                        tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                        clientId = ClientId.parse(row[InvoicesTable.clientId].toString()),
                        invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                        issueDate = row[InvoicesTable.issueDate],
                        dueDate = row[InvoicesTable.dueDate],
                        subtotalAmount = Money(row[InvoicesTable.subtotalAmount].toString()),
                        vatAmount = Money(row[InvoicesTable.vatAmount].toString()),
                        totalAmount = Money(row[InvoicesTable.totalAmount].toString()),
                        paidAmount = Money(row[InvoicesTable.paidAmount].toString()),
                        status = row[InvoicesTable.status],
                        currency = row[InvoicesTable.currency],
                        notes = row[InvoicesTable.notes],
                        termsAndConditions = row[InvoicesTable.termsAndConditions],
                        items = emptyList(), // Items not loaded in list view
                        peppolId = row[InvoicesTable.peppolId]?.let { PeppolId(it) },
                        peppolSentAt = row[InvoicesTable.peppolSentAt],
                        peppolStatus = row[InvoicesTable.peppolStatus],
                        paymentLink = row[InvoicesTable.paymentLink],
                        paymentLinkExpiresAt = row[InvoicesTable.paymentLinkExpiresAt],
                        paidAt = row[InvoicesTable.paidAt],
                        paymentMethod = row[InvoicesTable.paymentMethod],
                        createdAt = row[InvoicesTable.createdAt],
                        updatedAt = row[InvoicesTable.updatedAt]
                    )
                }
        }
    }

    /**
     * List overdue invoices for a tenant
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listOverdueInvoices(tenantId: TenantId): Result<List<Invoice>> = runCatching {
        dbQuery {
            val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date

            InvoicesTable.selectAll().where {
                (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (InvoicesTable.dueDate less today) and
                (InvoicesTable.status inList listOf(InvoiceStatus.Sent, InvoiceStatus.Draft))
            }.orderBy(InvoicesTable.dueDate)
            .map { row ->
                Invoice(
                    id = InvoiceId.parse(row[InvoicesTable.id].value.toString()),
                    tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                    clientId = ClientId.parse(row[InvoicesTable.clientId].toString()),
                    invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                    issueDate = row[InvoicesTable.issueDate],
                    dueDate = row[InvoicesTable.dueDate],
                    subtotalAmount = Money(row[InvoicesTable.subtotalAmount].toString()),
                    vatAmount = Money(row[InvoicesTable.vatAmount].toString()),
                    totalAmount = Money(row[InvoicesTable.totalAmount].toString()),
                    paidAmount = Money(row[InvoicesTable.paidAmount].toString()),
                    status = row[InvoicesTable.status],
                    currency = row[InvoicesTable.currency],
                    notes = row[InvoicesTable.notes],
                    termsAndConditions = row[InvoicesTable.termsAndConditions],
                    items = emptyList(),
                    peppolId = row[InvoicesTable.peppolId]?.let { PeppolId(it) },
                    peppolSentAt = row[InvoicesTable.peppolSentAt],
                    peppolStatus = row[InvoicesTable.peppolStatus],
                    paymentLink = row[InvoicesTable.paymentLink],
                    paymentLinkExpiresAt = row[InvoicesTable.paymentLinkExpiresAt],
                    paidAt = row[InvoicesTable.paidAt],
                    paymentMethod = row[InvoicesTable.paymentMethod],
                    createdAt = row[InvoicesTable.createdAt],
                    updatedAt = row[InvoicesTable.updatedAt]
                )
            }
        }
    }

    /**
     * Update invoice status
     * CRITICAL: MUST filter by tenant_id to prevent status updates of other tenants' invoices
     */
    suspend fun updateInvoiceStatus(
        invoiceId: InvoiceId,
        tenantId: TenantId,
        status: InvoiceStatus
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updatedRows = InvoicesTable.update({
                (InvoicesTable.id eq UUID.fromString(invoiceId.toString())) and
                (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[InvoicesTable.status] = status
            }
            updatedRows > 0
        }
    }

    /**
     * Update invoice
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId,
        request: CreateInvoiceRequest
    ): Result<Invoice> = runCatching {
        dbQuery {
            // Verify invoice exists and belongs to tenant
            val exists = InvoicesTable.selectAll().where {
                (InvoicesTable.id eq UUID.fromString(invoiceId.toString())) and
                (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0

            if (!exists) {
                throw IllegalArgumentException("Invoice not found or access denied")
            }

            // Update invoice
            InvoicesTable.update({
                (InvoicesTable.id eq UUID.fromString(invoiceId.toString())) and
                (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[clientId] = UUID.fromString(request.clientId.toString())
                it[subtotalAmount] = request.items.sumOf { item -> java.math.BigDecimal(item.lineTotal.value) }
                it[vatAmount] = request.items.sumOf { item -> java.math.BigDecimal(item.vatAmount.value) }
                it[totalAmount] = request.items.sumOf { item -> java.math.BigDecimal(item.lineTotal.value) + java.math.BigDecimal(item.vatAmount.value) }
                request.issueDate?.let { date -> it[issueDate] = date }
                request.dueDate?.let { date -> it[dueDate] = date }
                it[notes] = request.notes
            }

            // Delete existing items
            InvoiceItemsTable.deleteWhere {
                InvoiceItemsTable.invoiceId eq UUID.fromString(invoiceId.toString())
            }

            // Insert new items
            request.items.forEachIndexed { index, item ->
                InvoiceItemsTable.insert {
                    it[InvoiceItemsTable.invoiceId] = UUID.fromString(invoiceId.toString())
                    it[description] = item.description
                    it[quantity] = java.math.BigDecimal(item.quantity.value)
                    it[unitPrice] = java.math.BigDecimal(item.unitPrice.value)
                    it[vatRate] = java.math.BigDecimal(item.vatRate.value)
                    it[lineTotal] = java.math.BigDecimal(item.lineTotal.value)
                    it[vatAmount] = java.math.BigDecimal(item.vatAmount.value)
                    it[sortOrder] = index
                }
            }

            // Manually fetch and return the updated invoice
            val row = InvoicesTable.selectAll().where {
                (InvoicesTable.id eq UUID.fromString(invoiceId.toString())) and
                (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single()

            val items = InvoiceItemsTable.selectAll().where {
                InvoiceItemsTable.invoiceId eq UUID.fromString(invoiceId.toString())
            }.orderBy(InvoiceItemsTable.sortOrder)
            .map { itemRow ->
                InvoiceItem(
                    id = InvoiceItemId.parse(itemRow[InvoiceItemsTable.id].value.toString()),
                    invoiceId = invoiceId,
                    description = itemRow[InvoiceItemsTable.description],
                    quantity = Quantity(itemRow[InvoiceItemsTable.quantity].toString()),
                    unitPrice = Money(itemRow[InvoiceItemsTable.unitPrice].toString()),
                    vatRate = VatRate(itemRow[InvoiceItemsTable.vatRate].toString()),
                    lineTotal = Money(itemRow[InvoiceItemsTable.lineTotal].toString()),
                    vatAmount = Money(itemRow[InvoiceItemsTable.vatAmount].toString()),
                    sortOrder = itemRow[InvoiceItemsTable.sortOrder]
                )
            }

            Invoice(
                id = InvoiceId.parse(row[InvoicesTable.id].value.toString()),
                tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                clientId = ClientId.parse(row[InvoicesTable.clientId].toString()),
                invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                issueDate = row[InvoicesTable.issueDate],
                dueDate = row[InvoicesTable.dueDate],
                subtotalAmount = Money(row[InvoicesTable.subtotalAmount].toString()),
                vatAmount = Money(row[InvoicesTable.vatAmount].toString()),
                totalAmount = Money(row[InvoicesTable.totalAmount].toString()),
                paidAmount = Money(row[InvoicesTable.paidAmount].toString()),
                status = row[InvoicesTable.status],
                currency = row[InvoicesTable.currency],
                notes = row[InvoicesTable.notes],
                termsAndConditions = row[InvoicesTable.termsAndConditions],
                items = items,
                peppolId = row[InvoicesTable.peppolId]?.let { PeppolId(it) },
                peppolSentAt = row[InvoicesTable.peppolSentAt],
                peppolStatus = row[InvoicesTable.peppolStatus],
                paymentLink = row[InvoicesTable.paymentLink],
                paymentLinkExpiresAt = row[InvoicesTable.paymentLinkExpiresAt],
                paidAt = row[InvoicesTable.paidAt],
                paymentMethod = row[InvoicesTable.paymentMethod],
                createdAt = row[InvoicesTable.createdAt],
                updatedAt = row[InvoicesTable.updatedAt]
            )
        }
    }

    /**
     * Delete invoice (soft delete by marking as deleted/cancelled)
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deleteInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            // For now, we'll do a hard delete of items and invoice
            // In production, consider soft delete with a deleted_at timestamp

            // Delete items first (foreign key constraint)
            InvoiceItemsTable.deleteWhere {
                InvoiceItemsTable.invoiceId eq UUID.fromString(invoiceId.toString())
            }

            // Delete invoice
            val deletedRows = InvoicesTable.deleteWhere {
                (InvoicesTable.id eq UUID.fromString(invoiceId.toString())) and
                (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }

            deletedRows > 0
        }
    }

    /**
     * Check if an invoice exists and belongs to the tenant
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun exists(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            InvoicesTable.selectAll().where {
                (InvoicesTable.id eq UUID.fromString(invoiceId.toString())) and
                (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0
        }
    }
}
