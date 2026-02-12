package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.services.InvoiceNumberGenerator
import tech.dokus.database.tables.cashflow.InvoiceItemsTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.InvoiceNumber
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery
import java.math.BigDecimal
import java.util.UUID

/**
 * Repository for managing invoices
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return invoices from different tenants
 * 3. All operations must be tenant-isolated
 */
class InvoiceRepository(
    private val invoiceNumberGenerator: InvoiceNumberGenerator
) {

    /**
     * Create a new invoice with its items
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun createInvoice(
        tenantId: TenantId,
        request: CreateInvoiceRequest
    ): Result<FinancialDocumentDto.InvoiceDto> = runCatching {
        // Generate invoice number atomically BEFORE creating the invoice.
        // This ensures gap-less numbering as required by Belgian tax law.
        // The number is consumed even if invoice creation fails.
        val invoiceNumber = invoiceNumberGenerator.generateInvoiceNumber(tenantId).getOrThrow()

        dbQuery {
            // Insert invoice
            val invoiceId = InvoicesTable.insertAndGetId {
                it[InvoicesTable.tenantId] = UUID.fromString(tenantId.toString())
                it[contactId] = UUID.fromString(request.contactId.toString())
                it[InvoicesTable.invoiceNumber] = invoiceNumber
                val today = Clock.System.now()
                    .toLocalDateTime(TimeZone.UTC).date
                it[issueDate] = request.issueDate ?: today
                it[dueDate] = request.dueDate ?: today.plus(DatePeriod(days = 30))
                it[subtotalAmount] = request.subtotalAmount?.toDbDecimal()
                    ?: request.items.sumOf { item -> item.lineTotal.toDbDecimal() }
                it[vatAmount] = request.vatAmount?.toDbDecimal()
                    ?: request.items.sumOf { item -> item.vatAmount.toDbDecimal() }
                it[totalAmount] = request.totalAmount?.toDbDecimal()
                    ?: request.items.sumOf { item ->
                        item.lineTotal.toDbDecimal() + item.vatAmount.toDbDecimal()
                    }
                it[paidAmount] = BigDecimal.ZERO
                it[status] = InvoiceStatus.Draft
                it[InvoicesTable.direction] = request.direction
                it[notes] = request.notes
                it[documentId] = request.documentId?.let { docId -> UUID.fromString(docId.toString()) }
            }

            // Insert invoice items
            request.items.forEachIndexed { index, item ->
                InvoiceItemsTable.insert {
                    it[InvoiceItemsTable.invoiceId] = invoiceId.value
                    it[description] = item.description
                    it[quantity] = BigDecimal.valueOf(item.quantity)
                    it[unitPrice] = item.unitPrice.toDbDecimal()
                    it[vatRate] = item.vatRate.toDbDecimal()
                    it[lineTotal] = item.lineTotal.toDbDecimal()
                    it[vatAmount] = item.vatAmount.toDbDecimal()
                    it[sortOrder] = index
                }
            }

            // Manually fetch and return the complete invoice
            val row = InvoicesTable.selectAll().where {
                (InvoicesTable.id eq invoiceId.value) and
                    (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single()

            val items = InvoiceItemsTable.selectAll().where {
                InvoiceItemsTable.invoiceId eq invoiceId.value
            }.orderBy(InvoiceItemsTable.sortOrder)
                .map { itemRow ->
                    InvoiceItemDto(
                        id = itemRow[InvoiceItemsTable.id].value.toString(),
                        invoiceId = InvoiceId.parse(invoiceId.value.toString()),
                        description = itemRow[InvoiceItemsTable.description],
                        quantity = itemRow[InvoiceItemsTable.quantity].toDouble(),
                        unitPrice = Money.fromDbDecimal(itemRow[InvoiceItemsTable.unitPrice]),
                        vatRate = VatRate.fromDbDecimal(itemRow[InvoiceItemsTable.vatRate]),
                        lineTotal = Money.fromDbDecimal(itemRow[InvoiceItemsTable.lineTotal]),
                        vatAmount = Money.fromDbDecimal(itemRow[InvoiceItemsTable.vatAmount]),
                        sortOrder = itemRow[InvoiceItemsTable.sortOrder]
                    )
                }

            FinancialDocumentDto.InvoiceDto(
                id = InvoiceId.parse(row[InvoicesTable.id].value.toString()),
                tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                direction = row[InvoicesTable.direction],
                contactId = ContactId.parse(row[InvoicesTable.contactId].toString()),
                invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                issueDate = row[InvoicesTable.issueDate],
                dueDate = row[InvoicesTable.dueDate],
                subtotalAmount = Money.fromDbDecimal(row[InvoicesTable.subtotalAmount]),
                vatAmount = Money.fromDbDecimal(row[InvoicesTable.vatAmount]),
                totalAmount = Money.fromDbDecimal(row[InvoicesTable.totalAmount]),
                paidAmount = Money.fromDbDecimal(row[InvoicesTable.paidAmount]),
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
                documentId = row[InvoicesTable.documentId]?.let { DocumentId.parse(it.toString()) },
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
    ): Result<FinancialDocumentDto.InvoiceDto?> = runCatching {
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
                    InvoiceItemDto(
                        id = itemRow[InvoiceItemsTable.id].value.toString(),
                        invoiceId = invoiceId,
                        description = itemRow[InvoiceItemsTable.description],
                        quantity = itemRow[InvoiceItemsTable.quantity].toDouble(),
                        unitPrice = Money.fromDbDecimal(itemRow[InvoiceItemsTable.unitPrice]),
                        vatRate = VatRate.fromDbDecimal(itemRow[InvoiceItemsTable.vatRate]),
                        lineTotal = Money.fromDbDecimal(itemRow[InvoiceItemsTable.lineTotal]),
                        vatAmount = Money.fromDbDecimal(itemRow[InvoiceItemsTable.vatAmount]),
                        sortOrder = itemRow[InvoiceItemsTable.sortOrder]
                    )
                }

            // Map to domain model
            FinancialDocumentDto.InvoiceDto(
                id = InvoiceId.parse(row[InvoicesTable.id].value.toString()),
                tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                direction = row[InvoicesTable.direction],
                contactId = ContactId.parse(row[InvoicesTable.contactId].toString()),
                invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                issueDate = row[InvoicesTable.issueDate],
                dueDate = row[InvoicesTable.dueDate],
                subtotalAmount = Money.fromDbDecimal(row[InvoicesTable.subtotalAmount]),
                vatAmount = Money.fromDbDecimal(row[InvoicesTable.vatAmount]),
                totalAmount = Money.fromDbDecimal(row[InvoicesTable.totalAmount]),
                paidAmount = Money.fromDbDecimal(row[InvoicesTable.paidAmount]),
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
                documentId = row[InvoicesTable.documentId]?.let { DocumentId.parse(it.toString()) },
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
        direction: DocumentDirection? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<FinancialDocumentDto.InvoiceDto>> = runCatching {
        dbQuery {
            var query = InvoicesTable.selectAll().where {
                InvoicesTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            // Apply filters
            if (status != null) {
                query = query.andWhere { InvoicesTable.status eq status }
            }
            if (direction != null) {
                query = query.andWhere { InvoicesTable.direction eq direction }
            }
            if (fromDate != null) {
                query = query.andWhere { InvoicesTable.issueDate greaterEq fromDate }
            }
            if (toDate != null) {
                query = query.andWhere { InvoicesTable.issueDate lessEq toDate }
            }

            val total = query.count()

            // Apply pagination and ordering
            val items = query.orderBy(InvoicesTable.createdAt to SortOrder.DESC)
                .limit(limit + offset)
                .map { row ->
                    // For list view, we don't fetch items to improve performance
                    // Items will be loaded when getting individual invoice
                    FinancialDocumentDto.InvoiceDto(
                        id = InvoiceId.parse(row[InvoicesTable.id].value.toString()),
                        tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                        direction = row[InvoicesTable.direction],
                        contactId = ContactId.parse(row[InvoicesTable.contactId].toString()),
                        invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                        issueDate = row[InvoicesTable.issueDate],
                        dueDate = row[InvoicesTable.dueDate],
                        subtotalAmount = Money.fromDbDecimal(row[InvoicesTable.subtotalAmount]),
                        vatAmount = Money.fromDbDecimal(row[InvoicesTable.vatAmount]),
                        totalAmount = Money.fromDbDecimal(row[InvoicesTable.totalAmount]),
                        paidAmount = Money.fromDbDecimal(row[InvoicesTable.paidAmount]),
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
                        documentId = row[InvoicesTable.documentId]?.let { DocumentId.parse(it.toString()) },
                        createdAt = row[InvoicesTable.createdAt],
                        updatedAt = row[InvoicesTable.updatedAt]
                    )
                }
                .drop(offset)

            PaginatedResponse(
                items = items,
                total = total,
                limit = limit,
                offset = offset
            )
        }
    }

    /**
     * List overdue invoices for a tenant
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listOverdueInvoices(
        tenantId: TenantId,
        direction: DocumentDirection = DocumentDirection.Outbound
    ): Result<List<FinancialDocumentDto.InvoiceDto>> =
        runCatching {
            dbQuery {
                val today = Clock.System.now()
                    .toLocalDateTime(TimeZone.UTC).date

                InvoicesTable.selectAll().where {
                    (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString())) and
                        (InvoicesTable.direction eq direction) and
                        (InvoicesTable.dueDate less today) and
                        (
                            InvoicesTable.status inList listOf(
                                InvoiceStatus.Sent,
                                InvoiceStatus.Draft
                            )
                            )
                }.orderBy(InvoicesTable.dueDate)
                    .map { row ->
                        FinancialDocumentDto.InvoiceDto(
                            id = InvoiceId.parse(row[InvoicesTable.id].value.toString()),
                            tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                            direction = row[InvoicesTable.direction],
                            contactId = ContactId.parse(row[InvoicesTable.contactId].toString()),
                            invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                            issueDate = row[InvoicesTable.issueDate],
                            dueDate = row[InvoicesTable.dueDate],
                            subtotalAmount = Money.fromDbDecimal(row[InvoicesTable.subtotalAmount]),
                            vatAmount = Money.fromDbDecimal(row[InvoicesTable.vatAmount]),
                            totalAmount = Money.fromDbDecimal(row[InvoicesTable.totalAmount]),
                            paidAmount = Money.fromDbDecimal(row[InvoicesTable.paidAmount]),
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
                            documentId = row[InvoicesTable.documentId]?.let { DocumentId.parse(it.toString()) },
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

    data class InvoicePaymentUpdate(
        val totalAmount: Money,
        val paidAmount: Money,
        val status: InvoiceStatus
    )

    /**
     * Record a payment against an invoice by updating paid amount and status.
     *
     * This is a simplified payment model (no payment ledger yet):
     * - paid_amount is accumulated
     * - status transitions to PARTIALLY_PAID or PAID
     * - paid_at is set when the invoice becomes fully paid
     */
    suspend fun recordPayment(
        invoiceId: InvoiceId,
        tenantId: TenantId,
        amount: Money,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod
    ): Result<InvoicePaymentUpdate> = runCatching {
        require(amount.minor > 0) { "Payment amount must be positive" }

        dbQuery {
            val invoiceUuid = UUID.fromString(invoiceId.toString())
            val tenantUuid = UUID.fromString(tenantId.toString())

            val row = InvoicesTable.selectAll().where {
                (InvoicesTable.id eq invoiceUuid) and (InvoicesTable.tenantId eq tenantUuid)
            }.singleOrNull() ?: throw IllegalArgumentException("Invoice not found or access denied")

            val total = Money.fromDbDecimal(row[InvoicesTable.totalAmount])
            val currentPaid = Money.fromDbDecimal(row[InvoicesTable.paidAmount])

            // Idempotency: already fully paid
            if (currentPaid.minor >= total.minor) {
                return@dbQuery InvoicePaymentUpdate(
                    totalAmount = total,
                    paidAmount = total,
                    status = InvoiceStatus.Paid
                )
            }

            val newPaidRaw = currentPaid + amount
            val newPaid = if (newPaidRaw.minor >= total.minor) total else newPaidRaw

            val newStatus = if (newPaid.minor >= total.minor) {
                InvoiceStatus.Paid
            } else {
                InvoiceStatus.PartiallyPaid
            }

            val paidAt = if (newStatus == InvoiceStatus.Paid) {
                LocalDateTime(paymentDate.year, paymentDate.monthNumber, paymentDate.dayOfMonth, 12, 0, 0)
            } else {
                null
            }

            InvoicesTable.update({
                (InvoicesTable.id eq invoiceUuid) and (InvoicesTable.tenantId eq tenantUuid)
            }) {
                it[InvoicesTable.paidAmount] = newPaid.toDbDecimal()
                it[InvoicesTable.status] = newStatus
                it[InvoicesTable.paymentMethod] = paymentMethod
                it[InvoicesTable.paidAt] = paidAt
            }

            InvoicePaymentUpdate(
                totalAmount = total,
                paidAmount = newPaid,
                status = newStatus
            )
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
    ): Result<FinancialDocumentDto.InvoiceDto> = runCatching {
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
                it[contactId] = UUID.fromString(request.contactId.toString())
                it[direction] = request.direction
                it[subtotalAmount] =
                    request.items.sumOf { item -> item.lineTotal.toDbDecimal() }
                it[vatAmount] =
                    request.items.sumOf { item -> item.vatAmount.toDbDecimal() }
                it[totalAmount] = request.items.sumOf { item ->
                    item.lineTotal.toDbDecimal() + item.vatAmount.toDbDecimal()
                }
                request.issueDate?.let { date -> it[issueDate] = date }
                request.dueDate?.let { date -> it[dueDate] = date }
                it[notes] = request.notes
                request.documentId?.let { docId ->
                    it[documentId] = UUID.fromString(docId.toString())
                }
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
                    it[quantity] = BigDecimal.valueOf(item.quantity)
                    it[unitPrice] = item.unitPrice.toDbDecimal()
                    it[vatRate] = item.vatRate.toDbDecimal()
                    it[lineTotal] = item.lineTotal.toDbDecimal()
                    it[vatAmount] = item.vatAmount.toDbDecimal()
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
                    InvoiceItemDto(
                        id = itemRow[InvoiceItemsTable.id].value.toString(),
                        invoiceId = invoiceId,
                        description = itemRow[InvoiceItemsTable.description],
                        quantity = itemRow[InvoiceItemsTable.quantity].toDouble(),
                        unitPrice = Money.fromDbDecimal(itemRow[InvoiceItemsTable.unitPrice]),
                        vatRate = VatRate.fromDbDecimal(itemRow[InvoiceItemsTable.vatRate]),
                        lineTotal = Money.fromDbDecimal(itemRow[InvoiceItemsTable.lineTotal]),
                        vatAmount = Money.fromDbDecimal(itemRow[InvoiceItemsTable.vatAmount]),
                        sortOrder = itemRow[InvoiceItemsTable.sortOrder]
                    )
                }

            FinancialDocumentDto.InvoiceDto(
                id = InvoiceId.parse(row[InvoicesTable.id].value.toString()),
                tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                direction = row[InvoicesTable.direction],
                contactId = ContactId.parse(row[InvoicesTable.contactId].toString()),
                invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                issueDate = row[InvoicesTable.issueDate],
                dueDate = row[InvoicesTable.dueDate],
                subtotalAmount = Money.fromDbDecimal(row[InvoicesTable.subtotalAmount]),
                vatAmount = Money.fromDbDecimal(row[InvoicesTable.vatAmount]),
                totalAmount = Money.fromDbDecimal(row[InvoicesTable.totalAmount]),
                paidAmount = Money.fromDbDecimal(row[InvoicesTable.paidAmount]),
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
                documentId = row[InvoicesTable.documentId]?.let { DocumentId.parse(it.toString()) },
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

    /**
     * Update invoice's document reference.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateDocumentId(
        invoiceId: InvoiceId,
        tenantId: TenantId,
        documentId: DocumentId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updatedRows = InvoicesTable.update({
                (InvoicesTable.id eq UUID.fromString(invoiceId.toString())) and
                    (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[InvoicesTable.documentId] = UUID.fromString(documentId.toString())
            }
            updatedRows > 0
        }
    }

    /**
     * Find invoice by document ID.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun findByDocumentId(
        tenantId: TenantId,
        documentId: DocumentId
    ): FinancialDocumentDto.InvoiceDto? = dbQuery {
        InvoicesTable.selectAll().where {
            (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (InvoicesTable.documentId eq UUID.fromString(documentId.toString()))
        }.singleOrNull()?.let { row ->
            // Fetch invoice items
            val invoiceId = InvoiceId.parse(row[InvoicesTable.id].value.toString())
            val items = InvoiceItemsTable.selectAll().where {
                InvoiceItemsTable.invoiceId eq UUID.fromString(invoiceId.toString())
            }.orderBy(InvoiceItemsTable.sortOrder)
                .map { itemRow ->
                    InvoiceItemDto(
                        id = itemRow[InvoiceItemsTable.id].value.toString(),
                        invoiceId = invoiceId,
                        description = itemRow[InvoiceItemsTable.description],
                        quantity = itemRow[InvoiceItemsTable.quantity].toDouble(),
                        unitPrice = Money.fromDbDecimal(itemRow[InvoiceItemsTable.unitPrice]),
                        vatRate = VatRate.fromDbDecimal(itemRow[InvoiceItemsTable.vatRate]),
                        lineTotal = Money.fromDbDecimal(itemRow[InvoiceItemsTable.lineTotal]),
                        vatAmount = Money.fromDbDecimal(itemRow[InvoiceItemsTable.vatAmount]),
                        sortOrder = itemRow[InvoiceItemsTable.sortOrder]
                    )
                }

            FinancialDocumentDto.InvoiceDto(
                id = invoiceId,
                tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                direction = row[InvoicesTable.direction],
                contactId = ContactId.parse(row[InvoicesTable.contactId].toString()),
                invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                issueDate = row[InvoicesTable.issueDate],
                dueDate = row[InvoicesTable.dueDate],
                subtotalAmount = Money.fromDbDecimal(row[InvoicesTable.subtotalAmount]),
                vatAmount = Money.fromDbDecimal(row[InvoicesTable.vatAmount]),
                totalAmount = Money.fromDbDecimal(row[InvoicesTable.totalAmount]),
                paidAmount = Money.fromDbDecimal(row[InvoicesTable.paidAmount]),
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
                documentId = row[InvoicesTable.documentId]?.let { DocumentId.parse(it.toString()) },
                createdAt = row[InvoicesTable.createdAt],
                updatedAt = row[InvoicesTable.updatedAt]
            )
        }
    }

    /**
     * Find invoice by invoice number.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun findByInvoiceNumber(
        tenantId: TenantId,
        invoiceNumber: String
    ): FinancialDocumentDto.InvoiceDto? = dbQuery {
        InvoicesTable.selectAll().where {
            (InvoicesTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (InvoicesTable.invoiceNumber eq invoiceNumber)
        }.singleOrNull()?.let { row ->
            val invoiceId = InvoiceId.parse(row[InvoicesTable.id].value.toString())
            val items = InvoiceItemsTable.selectAll().where {
                InvoiceItemsTable.invoiceId eq UUID.fromString(invoiceId.toString())
            }.orderBy(InvoiceItemsTable.sortOrder)
                .map { itemRow ->
                    InvoiceItemDto(
                        id = itemRow[InvoiceItemsTable.id].value.toString(),
                        invoiceId = invoiceId,
                        description = itemRow[InvoiceItemsTable.description],
                        quantity = itemRow[InvoiceItemsTable.quantity].toDouble(),
                        unitPrice = Money.fromDbDecimal(itemRow[InvoiceItemsTable.unitPrice]),
                        vatRate = VatRate.fromDbDecimal(itemRow[InvoiceItemsTable.vatRate]),
                        lineTotal = Money.fromDbDecimal(itemRow[InvoiceItemsTable.lineTotal]),
                        vatAmount = Money.fromDbDecimal(itemRow[InvoiceItemsTable.vatAmount]),
                        sortOrder = itemRow[InvoiceItemsTable.sortOrder]
                    )
                }

            FinancialDocumentDto.InvoiceDto(
                id = invoiceId,
                tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
                direction = row[InvoicesTable.direction],
                contactId = ContactId.parse(row[InvoicesTable.contactId].toString()),
                invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
                issueDate = row[InvoicesTable.issueDate],
                dueDate = row[InvoicesTable.dueDate],
                subtotalAmount = Money.fromDbDecimal(row[InvoicesTable.subtotalAmount]),
                vatAmount = Money.fromDbDecimal(row[InvoicesTable.vatAmount]),
                totalAmount = Money.fromDbDecimal(row[InvoicesTable.totalAmount]),
                paidAmount = Money.fromDbDecimal(row[InvoicesTable.paidAmount]),
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
                documentId = row[InvoicesTable.documentId]?.let { DocumentId.parse(it.toString()) },
                createdAt = row[InvoicesTable.createdAt],
                updatedAt = row[InvoicesTable.updatedAt]
            )
        }
    }
}
