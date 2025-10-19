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
import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.InvoiceItem
import ai.dokus.foundation.domain.model.InvoiceTotals
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
    private val tenantService: TenantService,
    private val auditService: AuditServiceImpl
) : InvoiceService {
    private val logger = LoggerFactory.getLogger(InvoiceServiceImpl::class.java)

    override suspend fun create(request: CreateInvoiceRequest): Invoice {
        val invoiceNumber = tenantService.getNextInvoiceNumber(request.tenantId)
        val totals = calculateTotals(request.items)
        val today = Clock.System.todayIn(TimeZone.UTC)

        val invoiceId = dbQuery {
            val id = InvoicesTable.insertAndGetId {
                it[tenantId] = request.tenantId.value.toJavaUuid()
                it[clientId] = request.clientId.value.toJavaUuid()
                it[InvoicesTable.invoiceNumber] = invoiceNumber.value
                it[issueDate] = request.issueDate ?: today
                it[dueDate] = request.dueDate ?: today.plus(DatePeriod(days = 30))
                it[subtotalAmount] = BigDecimal(totals.subtotal.value)
                it[InvoicesTable.vatAmount] = BigDecimal(totals.vatAmount.value)
                it[totalAmount] = BigDecimal(totals.total.value)
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

        val invoice = getInvoiceWithItems(invoiceId.toKotlinUuid())

        // Audit log
        auditService.logAction(
            tenantId = request.tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.InvoiceCreated,
            entityType = EntityType.Invoice,
            entityId = invoice.id.value,
            oldValues = null,
            newValues = mapOf(
                "invoiceNumber" to invoice.invoiceNumber.value,
                "clientId" to invoice.clientId.value.toString(),
                "total" to invoice.totalAmount.value,
                "status" to invoice.status.name
            )
        )

        return invoice
    }

    override suspend fun update(
        invoiceId: InvoiceId,
        issueDate: LocalDate?,
        dueDate: LocalDate?,
        notes: String?,
        termsAndConditions: String?
    ) {
        // Capture old values and perform update
        val (tenantId, oldValues, newValues) = dbQuery {
            val javaUuid = invoiceId.value.toJavaUuid()

            val invoice = InvoicesTable.selectAll().where { InvoicesTable.id eq javaUuid }.singleOrNull()
                ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

            if (invoice[InvoicesTable.status] != InvoiceStatus.Draft) {
                throw IllegalArgumentException("Can only update draft invoices")
            }

            // Capture old values
            val oldVals = mutableMapOf<String, Any?>()
            val newVals = mutableMapOf<String, Any?>()
            if (issueDate != null) {
                oldVals["issueDate"] = invoice[InvoicesTable.issueDate].toString()
                newVals["issueDate"] = issueDate.toString()
            }
            if (dueDate != null) {
                oldVals["dueDate"] = invoice[InvoicesTable.dueDate].toString()
                newVals["dueDate"] = dueDate.toString()
            }
            if (notes != null) {
                oldVals["notes"] = invoice[InvoicesTable.notes]
                newVals["notes"] = notes
            }
            if (termsAndConditions != null) {
                oldVals["termsAndConditions"] = invoice[InvoicesTable.termsAndConditions]
                newVals["termsAndConditions"] = termsAndConditions
            }

            InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
                if (issueDate != null) it[InvoicesTable.issueDate] = issueDate
                if (dueDate != null) it[InvoicesTable.dueDate] = dueDate
                if (notes != null) it[InvoicesTable.notes] = notes
                if (termsAndConditions != null) it[InvoicesTable.termsAndConditions] = termsAndConditions
            }

            Triple(TenantId(invoice[InvoicesTable.tenantId].value.toKotlinUuid()), oldVals, newVals)
        }

        logger.info("Updated invoice $invoiceId")

        // Audit log
        auditService.logAction(
            tenantId = tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.InvoiceUpdated,
            entityType = EntityType.Invoice,
            entityId = invoiceId.value,
            oldValues = oldValues,
            newValues = newValues
        )
    }

    override suspend fun updateItems(invoiceId: InvoiceId, items: List<InvoiceItem>) {
        val totals = calculateTotals(items)

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
                it[subtotalAmount] = BigDecimal(totals.subtotal.value)
                it[InvoicesTable.vatAmount] = BigDecimal(totals.vatAmount.value)
                it[totalAmount] = BigDecimal(totals.total.value)
            }
        }

        logger.info("Updated items for invoice $invoiceId")
    }

    override suspend fun delete(invoiceId: InvoiceId) {
        val (tenantId, oldValues) = dbQuery {
            val javaUuid = invoiceId.value.toJavaUuid()

            val invoice = InvoicesTable.selectAll().where { InvoicesTable.id eq javaUuid }.singleOrNull()
                ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

            if (invoice[InvoicesTable.status] != InvoiceStatus.Draft) {
                throw IllegalArgumentException("Can only delete draft invoices")
            }

            // Capture invoice details before deletion
            val oldVals = mapOf(
                "invoiceNumber" to invoice[InvoicesTable.invoiceNumber],
                "status" to invoice[InvoicesTable.status].name,
                "totalAmount" to invoice[InvoicesTable.totalAmount].toString()
            )

            InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
                it[status] = InvoiceStatus.Cancelled
            }

            Pair(TenantId(invoice[InvoicesTable.tenantId].value.toKotlinUuid()), oldVals)
        }

        logger.info("Cancelled invoice $invoiceId")

        // Audit log
        auditService.logAction(
            tenantId = tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.InvoiceDeleted,
            entityType = EntityType.Invoice,
            entityId = invoiceId.value,
            oldValues = oldValues,
            newValues = mapOf("status" to InvoiceStatus.Cancelled.name)
        )
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

        // Fetch invoice to get details
        val invoice = findById(invoiceId)
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

        // Determine recipient email
        val toEmail = recipientEmail
            ?: throw IllegalArgumentException("Recipient email must be provided")

        // Generate PDF attachment
        val pdfBytes = generatePDF(invoiceId)

        // Prepare email content
        val subject = "Invoice ${invoice.invoiceNumber.value} - ${invoice.totalAmount.value}"
        val emailBody = buildString {
            appendLine("Dear Customer,")
            appendLine()
            appendLine(message ?: "Please find attached your invoice.")
            appendLine()
            appendLine("Invoice Details:")
            appendLine("  Invoice Number: ${invoice.invoiceNumber.value}")
            appendLine("  Issue Date: ${invoice.issueDate}")
            appendLine("  Due Date: ${invoice.dueDate}")
            appendLine("  Total Amount: ${invoice.totalAmount.value}")
            appendLine("  Amount Due: ${BigDecimal(invoice.totalAmount.value) - BigDecimal(invoice.paidAmount.value)}")
            appendLine()
            appendLine("Thank you for your business!")
            appendLine()
            appendLine("Best regards")
        }

        // TODO: Integrate with email service (e.g., JavaMail, SendGrid, AWS SES)
        // Example integration structure:
        // emailService.send(
        //     to = listOf(toEmail),
        //     cc = ccEmails ?: emptyList(),
        //     subject = subject,
        //     body = emailBody,
        //     attachments = listOf(
        //         EmailAttachment(
        //             filename = "invoice_${invoice.invoiceNumber.value}.pdf",
        //             content = pdfBytes,
        //             contentType = "application/pdf"
        //         )
        //     )
        // )

        logger.info("Email prepared for invoice $invoiceId to $toEmail (CC: ${ccEmails?.joinToString() ?: "none"})")
        logger.warn("Email sending not yet implemented - email service integration required")

        // Update invoice status to Sent after successful email
        updateStatus(UpdateInvoiceStatusRequest(invoiceId, InvoiceStatus.Sent))
    }

    override suspend fun sendViaPeppol(invoiceId: InvoiceId) {
        logger.info("Sending invoice $invoiceId via Peppol")

        // Fetch invoice with items
        val invoice = findById(invoiceId)
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

        // TODO: Full PEPPOL implementation requires:
        // 1. ClientService dependency to fetch client (customer) details
        // 2. TenantSettings query to get company details (companyName, peppolId, etc.)
        // 3. UBL conversion using PeppolUblConverter (already implemented)
        // 4. UBL validation using PeppolUblValidator (already implemented)
        // 5. Transmission using PeppolAccessPoint (MockPeppolAccessPoint for testing, OpenPeppolAccessPoint for production)
        //
        // Example full implementation:
        // val tenantSettings = tenantService.getSettings(invoice.tenantId)
        // val client = clientService.findById(invoice.clientId)
        //
        // val supplierInfo = TenantInfo(
        //     companyName = tenantSettings.companyName ?: throw IllegalArgumentException("Company name not configured"),
        //     vatNumber = tenantSettings.companyVatNumber?.value,
        //     companyNumber = null,
        //     peppolId = if (tenantSettings.enablePeppol) "0208:${tenantSettings.companyVatNumber?.value}" else null,
        //     email = tenant.email,
        //     phone = null,
        //     address = null
        // )
        //
        // val ublConverter = PeppolUblConverter()
        // val ublValidator = PeppolUblValidator()
        // val accessPoint = MockPeppolAccessPoint() // or OpenPeppolAccessPoint for production
        // val peppolService = PeppolInvoiceService(ublConverter, ublValidator, accessPoint)
        //
        // val result = peppolService.sendInvoice(invoice, supplierInfo, client, invoice.items)
        // if (!result.success) {
        //     throw IllegalStateException("Failed to send via Peppol: ${result.error}")
        // }
        //
        // markAsSent(invoiceId)

        logger.warn("PEPPOL infrastructure is ready but requires ClientService and TenantSettings integration")
        throw NotImplementedError(
            "PEPPOL sending requires: " +
            "1) ClientService dependency to fetch client details, " +
            "2) TenantSettings query for company/PEPPOL configuration. " +
            "Infrastructure ready: PeppolUblConverter, PeppolUblValidator, PeppolAccessPoint (Mock & OpenPeppol)"
        )
    }

    override suspend fun generatePDF(invoiceId: InvoiceId): ByteArray {
        logger.info("Generating PDF for invoice $invoiceId")

        // Fetch invoice with full details
        val invoice = findById(invoiceId)
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

        // TODO: Add PDF library dependency (e.g., Apache PDFBox or iText)
        // For now, generate a simple text-based representation
        val pdfContent = buildString {
            appendLine("=".repeat(60))
            appendLine("INVOICE")
            appendLine("=".repeat(60))
            appendLine()
            appendLine("Invoice Number: ${invoice.invoiceNumber.value}")
            appendLine("Issue Date: ${invoice.issueDate}")
            appendLine("Due Date: ${invoice.dueDate}")
            appendLine("Status: ${invoice.status}")
            appendLine()
            appendLine("-".repeat(60))
            appendLine("ITEMS")
            appendLine("-".repeat(60))
            appendLine()

            invoice.items.forEach { item ->
                appendLine("Description: ${item.description}")
                appendLine("Quantity: ${item.quantity.value}")
                appendLine("Unit Price: ${item.unitPrice.value}")
                appendLine("VAT Rate: ${item.vatRate.value}%")
                val lineTotal = BigDecimal(item.quantity.value) * BigDecimal(item.unitPrice.value)
                appendLine("Line Total: $lineTotal")
                appendLine()
            }

            appendLine("-".repeat(60))
            appendLine("TOTALS")
            appendLine("-".repeat(60))
            appendLine("Subtotal: ${invoice.subtotalAmount.value}")
            appendLine("VAT Amount: ${invoice.vatAmount.value}")
            appendLine("Total Amount: ${invoice.totalAmount.value}")
            appendLine("Paid Amount: ${invoice.paidAmount.value}")
            val remaining = BigDecimal(invoice.totalAmount.value) - BigDecimal(invoice.paidAmount.value)
            appendLine("Amount Due: $remaining")
            appendLine()

            if (invoice.notes != null) {
                appendLine("-".repeat(60))
                appendLine("NOTES")
                appendLine("-".repeat(60))
                appendLine(invoice.notes)
                appendLine()
            }

            appendLine("=".repeat(60))
        }

        // Return as bytes (in production, this would be actual PDF bytes)
        return pdfContent.toByteArray(Charsets.UTF_8)
    }

    override suspend fun generatePaymentLink(invoiceId: InvoiceId, expiresAt: Instant?): String {
        logger.info("Generating payment link for invoice $invoiceId")

        // Fetch invoice to get details
        val invoice = findById(invoiceId)
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

        // Calculate amount due
        val amountDue = BigDecimal(invoice.totalAmount.value) - BigDecimal(invoice.paidAmount.value)
        if (amountDue <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Invoice is already fully paid")
        }

        // Generate secure token for the payment link
        val token = generateSecureToken(invoiceId)

        // Set expiration (default 30 days if not provided)
        val expiry = expiresAt ?: Clock.System.now().plus(30, kotlinx.datetime.DateTimeUnit.DAY, TimeZone.UTC)

        // TODO: Integrate with payment gateway (e.g., Stripe, Mollie, PayPal)
        // Example integration structure for Stripe:
        // val session = stripeService.createCheckoutSession(
        //     amount = amountDue,
        //     currency = "EUR",
        //     description = "Invoice ${invoice.invoiceNumber.value}",
        //     metadata = mapOf(
        //         "invoiceId" to invoiceId.value.toString(),
        //         "tenantId" to invoice.tenantId.value.toString()
        //     ),
        //     successUrl = "https://dokus.ai/payment/success?session={CHECKOUT_SESSION_ID}",
        //     cancelUrl = "https://dokus.ai/payment/cancel"
        // )
        // return session.url

        // For now, return a placeholder payment link
        val paymentLink = "https://pay.dokus.ai/invoice/${invoice.invoiceNumber.value}?token=$token&expires=${expiry.toEpochMilliseconds()}"

        logger.info("Generated payment link for invoice $invoiceId: $paymentLink (expires: $expiry)")
        logger.warn("Payment gateway integration not yet implemented - this is a placeholder link")

        return paymentLink
    }

    private fun generateSecureToken(invoiceId: InvoiceId): String {
        // Generate a secure random token for the payment link
        // In production, this should use a cryptographically secure random generator
        val tokenBytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(tokenBytes)
        return tokenBytes.joinToString("") { "%02x".format(it) }
    }

    override suspend fun markAsSent(invoiceId: InvoiceId) = dbQuery {
        val javaUuid = invoiceId.value.toJavaUuid()
        InvoicesTable.update({ InvoicesTable.id eq javaUuid }) {
            it[status] = InvoiceStatus.Sent
        }

        logger.info("Marked invoice $invoiceId as sent")
    }

    override fun watchInvoices(tenantId: TenantId): Flow<Invoice> {
        return kotlinx.coroutines.flow.flow {
            var lastSeenInvoices = emptyMap<InvoiceId, Invoice>()

            while (true) {
                // Poll for invoice changes every 5 seconds
                kotlinx.coroutines.delay(5000)

                try {
                    val currentInvoices = listByTenant(
                        tenantId = tenantId,
                        status = null,
                        clientId = null,
                        fromDate = null,
                        toDate = null,
                        limit = null,
                        offset = null
                    )
                    val currentMap = currentInvoices.associateBy { it.id }

                    // Emit new or updated invoices
                    currentInvoices.forEach { invoice ->
                        val previous = lastSeenInvoices[invoice.id]
                        if (previous == null || previous != invoice) {
                            // New or updated invoice
                            emit(invoice)
                        }
                    }

                    lastSeenInvoices = currentMap
                } catch (e: Exception) {
                    // Log error but continue polling
                    logger.error("Error polling invoices for tenant $tenantId", e)
                }
            }
        }
    }

    override suspend fun calculateTotals(items: List<InvoiceItem>): InvoiceTotals {
        var subtotal = BigDecimal.ZERO
        var vatTotal = BigDecimal.ZERO

        items.forEach { item ->
            val lineTotal = BigDecimal(item.quantity.value) * BigDecimal(item.unitPrice.value)
            val vatAmount = lineTotal * BigDecimal(item.vatRate.value) / BigDecimal("100")
            subtotal += lineTotal
            vatTotal += vatAmount
        }

        val total = subtotal + vatTotal

        return InvoiceTotals(
            subtotal = Money(subtotal.toString()),
            vatAmount = Money(vatTotal.toString()),
            total = Money(total.toString())
        )
    }

    override suspend fun getStatistics(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Map<String, Money> = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        var query = InvoicesTable.selectAll().where { InvoicesTable.tenantId eq javaUuid }

        // Apply date filters if provided
        if (fromDate != null) query = query.andWhere { InvoicesTable.issueDate greaterEq fromDate }
        if (toDate != null) query = query.andWhere { InvoicesTable.issueDate lessEq toDate }

        val invoices = query.toList()
        val today = Clock.System.todayIn(TimeZone.UTC)

        // Calculate statistics
        var totalInvoiced = BigDecimal.ZERO
        var totalPaid = BigDecimal.ZERO
        var totalOutstanding = BigDecimal.ZERO
        var totalOverdue = BigDecimal.ZERO

        invoices.forEach { row ->
            val total = row[InvoicesTable.totalAmount]
            val paid = row[InvoicesTable.paidAmount]
            val remaining = total - paid
            val dueDate = row[InvoicesTable.dueDate]
            val status = row[InvoicesTable.status]

            totalInvoiced += total
            totalPaid += paid

            if (status != InvoiceStatus.Cancelled) {
                totalOutstanding += remaining

                // Check if overdue (unpaid and past due date)
                if (remaining > BigDecimal.ZERO && dueDate < today && status != InvoiceStatus.Paid) {
                    totalOverdue += remaining
                }
            }
        }

        mapOf(
            "totalInvoiced" to Money(totalInvoiced.toString()),
            "totalPaid" to Money(totalPaid.toString()),
            "totalOutstanding" to Money(totalOutstanding.toString()),
            "totalOverdue" to Money(totalOverdue.toString())
        )
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
