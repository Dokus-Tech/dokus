package tech.dokus.backend.services.documents

import kotlinx.datetime.LocalDate
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.repository.documents.CreateDocumentLinkPayload
import tech.dokus.database.repository.documents.DocumentLinkRepository
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.DocumentLinkType
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Service for ProForma document operations.
 *
 * ProForma is informational only - no cashflow impact on confirmation.
 * It can be explicitly converted to an Invoice via the convertToInvoice action.
 *
 * NOTE: This service is in the documents package (NOT cashflow) because
 * ProForma has no financial impact until converted.
 */
class ProFormaService(
    private val draftRepository: DocumentDraftRepository,
    private val invoiceRepository: InvoiceRepository,
    private val documentLinkRepository: DocumentLinkRepository,
) {
    private val logger = loggerFor()

    /**
     * Convert a ProForma document to an Invoice.
     *
     * This creates a new Invoice entity populated from the ProForma's extracted data
     * and creates a DocumentLink (ConvertedTo) for traceability.
     *
     * Validation:
     * - ProForma must exist and be confirmed
     * - ProForma must not have already been converted
     * - Contact (customer) must be provided
     *
     * @param tenantId The tenant ID
     * @param proFormaDocumentId The ProForma document ID to convert
     * @param contactId The contact (customer) to use for the invoice
     * @param issueDate Optional override for issue date (defaults to ProForma's issue date)
     * @param dueDate Optional override for due date (defaults to 30 days from issue date)
     * @return The created Invoice
     */
    @Suppress("CyclomaticComplexMethod", "MagicNumber")
    suspend fun convertToInvoice(
        tenantId: TenantId,
        proFormaDocumentId: DocumentId,
        contactId: ContactId,
        issueDate: LocalDate? = null,
        dueDate: LocalDate? = null,
    ): Result<FinancialDocumentDto.InvoiceDto> = runCatching {
        logger.info("Converting ProForma to Invoice: documentId=$proFormaDocumentId, tenant=$tenantId")

        // 1. Get the ProForma draft
        val draft = draftRepository.getByDocumentId(proFormaDocumentId, tenantId)
            ?: error("ProForma document not found: $proFormaDocumentId")

        // 2. Validate it's a ProForma
        if (draft.documentType != DocumentType.ProForma) {
            error("Document is not a ProForma: type=${draft.documentType}")
        }

        // 3. Validate draft status (must be confirmed)
        if (draft.documentStatus != DocumentStatus.Confirmed) {
            error("ProForma must be confirmed before conversion: status=${draft.documentStatus}")
        }

        // 4. Check not already converted
        val alreadyConverted = documentLinkRepository.hasConversionLink(tenantId, proFormaDocumentId)
        if (alreadyConverted) {
            error("ProForma has already been converted to Invoice")
        }

        // 5. Extract ProForma data
        val proFormaFields = draft.extractedData?.proForma
            ?: error("ProForma extracted data not found")

        // 6. Build CreateInvoiceRequest from ProForma data
        val effectiveIssueDate = issueDate
            ?: proFormaFields.issueDate
            ?: error("Issue date is required")

        val effectiveDueDate = dueDate
            ?: proFormaFields.validUntil
            ?: LocalDate.fromEpochDays(effectiveIssueDate.toEpochDays() + 30)

        val defaultVatRate = VatRate.STANDARD_BE

        // Build invoice items from ProForma items or create synthetic item from totals
        val invoiceItems = proFormaFields.items?.mapIndexed { index, item ->
            val unitPrice = item.unitPrice ?: Money.ZERO
            val vatRate = item.vatRate ?: defaultVatRate
            val lineTotal = item.lineTotal ?: unitPrice
            val vatAmount = item.vatAmount ?: vatRate.applyTo(lineTotal)

            InvoiceItemDto(
                description = item.description ?: "Line item ${index + 1}",
                quantity = item.quantity ?: 1.0,
                unitPrice = unitPrice,
                vatRate = vatRate,
                lineTotal = lineTotal,
                vatAmount = vatAmount,
                sortOrder = index
            )
        } ?: run {
            // Create a single synthetic line item from totals if no items
            val subtotal = proFormaFields.subtotalAmount ?: proFormaFields.totalAmount ?: Money.ZERO
            val vatAmount = proFormaFields.vatAmount ?: defaultVatRate.applyTo(subtotal)

            listOf(
                InvoiceItemDto(
                    description = "Services (from Pro Forma${proFormaFields.proFormaNumber?.let { " #$it" } ?: ""})",
                    quantity = 1.0,
                    unitPrice = subtotal,
                    vatRate = defaultVatRate,
                    lineTotal = subtotal,
                    vatAmount = vatAmount,
                    sortOrder = 0
                )
            )
        }

        val notes = buildString {
            append("Converted from Pro Forma")
            proFormaFields.proFormaNumber?.let { append(" #$it") }
            proFormaFields.notes?.let { append("\n\n$it") }
        }

        val invoiceRequest = CreateInvoiceRequest(
            contactId = contactId,
            items = invoiceItems,
            issueDate = effectiveIssueDate,
            dueDate = effectiveDueDate,
            notes = notes,
            documentId = null, // ProForma document stays with ProForma
        )

        // 7. Create the Invoice
        val invoice = invoiceRepository.createInvoice(tenantId, invoiceRequest).getOrThrow()
        logger.info("Created Invoice from ProForma: invoiceId=${invoice.id}")

        // 8. Create the DocumentLink for traceability
        documentLinkRepository.create(
            tenantId = tenantId,
            payload = CreateDocumentLinkPayload(
                sourceDocumentId = proFormaDocumentId,
                targetDocumentId = null, // Invoice has no document
                externalReference = invoice.id.toString(),
                linkType = DocumentLinkType.ConvertedTo,
            )
        )
        logger.info("Created ConvertedTo link: ProForma $proFormaDocumentId -> Invoice ${invoice.id}")

        invoice
    }.onFailure { e ->
        logger.error("Failed to convert ProForma to Invoice: ${e.message}", e)
    }

    /**
     * Check if a ProForma has already been converted to an Invoice.
     */
    suspend fun hasBeenConverted(
        tenantId: TenantId,
        proFormaDocumentId: DocumentId
    ): Boolean {
        return documentLinkRepository.hasConversionLink(tenantId, proFormaDocumentId)
    }

    /**
     * Get the Invoice ID that a ProForma was converted to.
     * Returns null if not converted.
     */
    suspend fun getConvertedInvoiceId(
        tenantId: TenantId,
        proFormaDocumentId: DocumentId
    ): String? {
        val links = documentLinkRepository.getBySourceAndType(
            tenantId = tenantId,
            sourceDocumentId = proFormaDocumentId,
            linkType = DocumentLinkType.ConvertedTo
        )
        return links.firstOrNull()?.externalReference
    }
}
