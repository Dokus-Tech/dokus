package tech.dokus.peppol.provider.client

import java.util.Locale
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.utils.json
import tech.dokus.peppol.model.PeppolDocumentList
import tech.dokus.peppol.model.PeppolDocumentSummary
import tech.dokus.peppol.model.PeppolInboxItem
import tech.dokus.peppol.model.PeppolLineItem
import tech.dokus.peppol.model.PeppolMonetaryTotals
import tech.dokus.peppol.model.PeppolParty
import tech.dokus.peppol.model.PeppolReceivedDocument
import tech.dokus.peppol.model.PeppolReceivedLineItem
import tech.dokus.peppol.model.PeppolSendRequest
import tech.dokus.peppol.model.PeppolSendResponse
import tech.dokus.peppol.model.PeppolTaxSubtotal
import tech.dokus.peppol.model.PeppolTaxTotal
import tech.dokus.peppol.model.PeppolVerifyResponse
import tech.dokus.peppol.provider.client.recommand.model.RecommandCreditNote
import tech.dokus.peppol.provider.client.recommand.model.RecommandDocumentDetail
import tech.dokus.peppol.provider.client.recommand.model.RecommandDocumentDirection
import tech.dokus.peppol.provider.client.recommand.model.RecommandDocumentSummary
import tech.dokus.peppol.provider.client.recommand.model.RecommandDocumentType
import tech.dokus.peppol.provider.client.recommand.model.RecommandGetDocumentsResponse
import tech.dokus.peppol.provider.client.recommand.model.RecommandInboxDocument
import tech.dokus.peppol.provider.client.recommand.model.RecommandInvoice
import tech.dokus.peppol.provider.client.recommand.model.RecommandLine
import tech.dokus.peppol.provider.client.recommand.model.RecommandParty
import tech.dokus.peppol.provider.client.recommand.model.RecommandPaymentMeans
import tech.dokus.peppol.provider.client.recommand.model.RecommandPaymentMethod
import tech.dokus.peppol.provider.client.recommand.model.RecommandSendDocumentRequest
import tech.dokus.peppol.provider.client.recommand.model.RecommandSendDocumentResponse
import tech.dokus.peppol.provider.client.recommand.model.RecommandSendInvoice
import tech.dokus.peppol.provider.client.recommand.model.RecommandSelfBillingCreditNote
import tech.dokus.peppol.provider.client.recommand.model.RecommandSelfBillingInvoice
import tech.dokus.peppol.provider.client.recommand.model.RecommandTotals
import tech.dokus.peppol.provider.client.recommand.model.RecommandVat
import tech.dokus.peppol.provider.client.recommand.model.RecommandVatCategory
import tech.dokus.peppol.provider.client.recommand.model.RecommandVatTotals
import tech.dokus.peppol.provider.client.recommand.model.RecommandVerifyRecipientResponse

/**
 * Maps between provider-agnostic Peppol models and Recommand-specific API models.
 */
object RecommandMapper {

    // ========================================================================
    // SEND REQUEST MAPPING
    // ========================================================================

    /**
     * Convert provider-agnostic send request to Recommand send request body.
     *
     * Used in:
     * - `POST /api/v1/{companyId}/send` (path params: `companyId`)
     */
    fun toRecommandRequest(request: PeppolSendRequest): RecommandSendDocumentRequest {
        val documentType = toRecommandDocumentType(request.documentType)

        val documentJson = when (documentType) {
            RecommandDocumentType.Invoice -> json.encodeToJsonElement(toRecommandSendInvoice(request.invoice))
            else -> error("Unsupported Peppol documentType=$documentType for current PeppolSendRequest mapping")
        }

        return RecommandSendDocumentRequest(
            recipient = request.recipientPeppolId,
            documentType = documentType,
            document = documentJson,
        )
    }

    private fun toRecommandSendInvoice(invoice: tech.dokus.peppol.model.PeppolInvoiceData): RecommandSendInvoice {
        return RecommandSendInvoice(
            invoiceNumber = invoice.invoiceNumber,
            issueDate = invoice.issueDate,
            dueDate = invoice.dueDate,
            seller = toRecommandParty(invoice.seller),
            buyer = toRecommandParty(invoice.buyer),
            buyerReference = invoice.buyer.companyNumber ?: invoice.buyer.vatNumber,
            lines = invoice.lineItems.map(::toRecommandLine),
            paymentMeans = invoice.paymentInfo
                ?.takeIf { it.iban != null }
                ?.let {
                    listOf(
                        RecommandPaymentMeans(
                            iban = requireNotNull(it.iban),
                            paymentMethod = paymentMethodForPaymentMeansCode(it.paymentMeansCode),
                            reference = it.paymentId ?: "",
                        )
                    )
                },
            note = invoice.note,
            currency = invoice.currencyCode,
        )
    }

    private fun toRecommandParty(party: PeppolParty): RecommandParty {
        val street = requireNotNull(party.streetName) { "Missing streetName for party ${party.name}" }
        val city = requireNotNull(party.cityName) { "Missing cityName for party ${party.name}" }
        val postalZone = requireNotNull(party.postalZone) { "Missing postalZone for party ${party.name}" }
        val country = requireNotNull(party.countryCode) { "Missing countryCode for party ${party.name}" }

        return RecommandParty(
            vatNumber = party.vatNumber,
            enterpriseNumber = party.companyNumber,
            name = party.name,
            street = street,
            city = city,
            postalZone = postalZone,
            country = country,
            email = party.contactEmail,
        )
    }

    private fun toRecommandLine(item: PeppolLineItem): RecommandLine {
        val vatCategory = runCatching { RecommandVatCategory.valueOf(item.taxCategory) }.getOrDefault(RecommandVatCategory.S)
        return RecommandLine(
            id = item.id,
            name = item.name,
            description = item.description,
            quantity = item.quantity.asDecimalString(),
            unitCode = item.unitCode,
            netPriceAmount = item.unitPrice.asDecimalString(),
            netAmount = item.lineTotal.asDecimalString(),
            vat = RecommandVat(
                category = vatCategory,
                percentage = item.taxPercent.asDecimalString(),
            ),
        )
    }

    private fun paymentMethodForPaymentMeansCode(code: String): RecommandPaymentMethod = when (code) {
        "10" -> RecommandPaymentMethod.Cash
        "30", "58" -> RecommandPaymentMethod.CreditTransfer
        "31", "59" -> RecommandPaymentMethod.DebitTransfer
        "48", "49" -> RecommandPaymentMethod.BankCard
        else -> RecommandPaymentMethod.CreditTransfer
    }

    private fun Double.asDecimalString(scale: Int = 2): String = "%.${scale}f".format(Locale.ROOT, this)

    private fun toRecommandDocumentType(type: PeppolDocumentType): RecommandDocumentType = when (type) {
        PeppolDocumentType.Invoice -> RecommandDocumentType.Invoice
        PeppolDocumentType.CreditNote -> RecommandDocumentType.CreditNote
        PeppolDocumentType.SelfBillingInvoice -> RecommandDocumentType.SelfBillingInvoice
        PeppolDocumentType.SelfBillingCreditNote -> RecommandDocumentType.SelfBillingCreditNote
        PeppolDocumentType.Xml -> RecommandDocumentType.Xml
    }

    // ========================================================================
    // SEND RESPONSE MAPPING
    // ========================================================================

    fun fromRecommandResponse(response: RecommandSendDocumentResponse): PeppolSendResponse {
        return PeppolSendResponse(
            success = response.success,
            externalDocumentId = response.id,
        )
    }

    // ========================================================================
    // VERIFY RESPONSE MAPPING
    // ========================================================================

    fun fromRecommandVerifyResponse(response: RecommandVerifyRecipientResponse): PeppolVerifyResponse {
        return PeppolVerifyResponse(
            registered = response.isValid,
            participantId = null,
            name = null,
            documentTypes = emptyList(),
        )
    }

    // ========================================================================
    // INBOX MAPPING
    // ========================================================================

    fun fromRecommandInboxItem(item: RecommandInboxDocument): PeppolInboxItem {
        return PeppolInboxItem(
            id = item.id,
            documentType = recommandToDocumentType(item.type),
            senderPeppolId = item.senderId,
            receiverPeppolId = item.receiverId,
            receivedAt = item.createdAt,
            isRead = item.readAt != null,
        )
    }

    // ========================================================================
    // DOCUMENT DETAIL MAPPING
    // ========================================================================

    fun fromRecommandDocumentDetail(detail: RecommandDocumentDetail): PeppolReceivedDocument {
        val parsed = detail.parsed ?: return PeppolReceivedDocument(
            id = detail.id,
            documentType = recommandToDocumentType(detail.type),
            senderPeppolId = detail.senderId,
            invoiceNumber = null,
            issueDate = null,
            dueDate = null,
            seller = null,
            buyer = null,
            lineItems = null,
            totals = null,
            taxTotal = null,
            note = null,
            currencyCode = null,
        )

        return when (detail.type) {
            RecommandDocumentType.Invoice -> fromParsedInvoice(detail, json.decodeFromJsonElement<RecommandInvoice>(parsed))
            RecommandDocumentType.CreditNote -> fromParsedCreditNote(detail, json.decodeFromJsonElement<RecommandCreditNote>(parsed))
            RecommandDocumentType.SelfBillingInvoice ->
                fromParsedSelfBillingInvoice(detail, json.decodeFromJsonElement<RecommandSelfBillingInvoice>(parsed))
            RecommandDocumentType.SelfBillingCreditNote ->
                fromParsedSelfBillingCreditNote(detail, json.decodeFromJsonElement<RecommandSelfBillingCreditNote>(parsed))
            RecommandDocumentType.MessageLevelResponse, RecommandDocumentType.Xml -> PeppolReceivedDocument(
                id = detail.id,
                documentType = recommandToDocumentType(detail.type),
                senderPeppolId = detail.senderId,
                invoiceNumber = null,
                issueDate = null,
                dueDate = null,
                seller = null,
                buyer = null,
                lineItems = null,
                totals = null,
                taxTotal = null,
                note = null,
                currencyCode = null,
            )
        }
    }

    private fun fromParsedInvoice(detail: RecommandDocumentDetail, invoice: RecommandInvoice): PeppolReceivedDocument {
        return PeppolReceivedDocument(
            id = detail.id,
            documentType = recommandToDocumentType(detail.type),
            senderPeppolId = detail.senderId,
            invoiceNumber = invoice.invoiceNumber,
            issueDate = invoice.issueDate.toString(),
            dueDate = invoice.dueDate?.toString(),
            seller = invoice.seller.let(::fromRecommandParty),
            buyer = invoice.buyer.let(::fromRecommandParty),
            lineItems = invoice.lines.map(::fromRecommandLine),
            totals = invoice.totals?.let(::toPeppolMonetaryTotals),
            taxTotal = invoice.vat?.let(::toPeppolTaxTotal),
            note = invoice.note,
            currencyCode = invoice.currency,
        )
    }

    private fun fromParsedCreditNote(detail: RecommandDocumentDetail, creditNote: RecommandCreditNote): PeppolReceivedDocument {
        return PeppolReceivedDocument(
            id = detail.id,
            documentType = recommandToDocumentType(detail.type),
            senderPeppolId = detail.senderId,
            invoiceNumber = creditNote.creditNoteNumber,
            issueDate = creditNote.issueDate.toString(),
            dueDate = null,
            seller = creditNote.seller.let(::fromRecommandParty),
            buyer = creditNote.buyer.let(::fromRecommandParty),
            lineItems = creditNote.lines.map(::fromRecommandLine),
            totals = creditNote.totals?.let(::toPeppolMonetaryTotals),
            taxTotal = creditNote.vat?.let(::toPeppolTaxTotal),
            note = creditNote.note,
            currencyCode = creditNote.currency,
        )
    }

    private fun fromParsedSelfBillingInvoice(
        detail: RecommandDocumentDetail,
        invoice: RecommandSelfBillingInvoice
    ): PeppolReceivedDocument {
        return PeppolReceivedDocument(
            id = detail.id,
            documentType = recommandToDocumentType(detail.type),
            senderPeppolId = detail.senderId,
            invoiceNumber = invoice.invoiceNumber,
            issueDate = invoice.issueDate.toString(),
            dueDate = invoice.dueDate?.toString(),
            seller = invoice.seller.let(::fromRecommandParty),
            buyer = invoice.buyer.let(::fromRecommandParty),
            lineItems = invoice.lines.map(::fromRecommandLine),
            totals = invoice.totals?.let(::toPeppolMonetaryTotals),
            taxTotal = invoice.vat?.let(::toPeppolTaxTotal),
            note = invoice.note,
            currencyCode = invoice.currency,
        )
    }

    private fun fromParsedSelfBillingCreditNote(
        detail: RecommandDocumentDetail,
        creditNote: RecommandSelfBillingCreditNote
    ): PeppolReceivedDocument {
        return PeppolReceivedDocument(
            id = detail.id,
            documentType = recommandToDocumentType(detail.type),
            senderPeppolId = detail.senderId,
            invoiceNumber = creditNote.creditNoteNumber,
            issueDate = creditNote.issueDate.toString(),
            dueDate = null,
            seller = creditNote.seller.let(::fromRecommandParty),
            buyer = creditNote.buyer.let(::fromRecommandParty),
            lineItems = creditNote.lines.map(::fromRecommandLine),
            totals = creditNote.totals?.let(::toPeppolMonetaryTotals),
            taxTotal = creditNote.vat?.let(::toPeppolTaxTotal),
            note = creditNote.note,
            currencyCode = creditNote.currency,
        )
    }

    private fun fromRecommandParty(party: RecommandParty): PeppolParty {
        return PeppolParty(
            name = party.name,
            vatNumber = party.vatNumber,
            streetName = party.street,
            cityName = party.city,
            postalZone = party.postalZone,
            countryCode = party.country,
            contactEmail = party.email,
            contactName = null,
            companyNumber = party.enterpriseNumber,
        )
    }

    private fun fromRecommandLine(line: RecommandLine): PeppolReceivedLineItem {
        return PeppolReceivedLineItem(
            id = line.id,
            name = line.name,
            description = line.description,
            quantity = line.quantity.toDoubleOrNull(),
            unitCode = line.unitCode,
            unitPrice = line.netPriceAmount.toDoubleOrNull(),
            lineTotal = line.netAmount?.toDoubleOrNull(),
            taxCategory = line.vat.category.name,
            taxPercent = line.vat.percentage.toDoubleOrNull(),
        )
    }

    private fun toPeppolMonetaryTotals(totals: RecommandTotals): PeppolMonetaryTotals {
        return PeppolMonetaryTotals(
            lineExtensionAmount = totals.linesAmount?.toDoubleOrNull(),
            taxExclusiveAmount = totals.taxExclusiveAmount.toDoubleOrNull(),
            taxInclusiveAmount = totals.taxInclusiveAmount.toDoubleOrNull(),
            payableAmount = (totals.payableAmount ?: totals.taxInclusiveAmount).toDoubleOrNull(),
        )
    }

    private fun toPeppolTaxTotal(vat: RecommandVatTotals): PeppolTaxTotal {
        return PeppolTaxTotal(
            taxAmount = vat.totalVatAmount.toDoubleOrNull(),
            taxSubtotals = vat.subtotals.map { sub ->
                PeppolTaxSubtotal(
                    taxableAmount = sub.taxableAmount.toDoubleOrNull(),
                    taxAmount = sub.vatAmount.toDoubleOrNull(),
                    taxCategory = sub.category.name,
                    taxPercent = sub.percentage.toDoubleOrNull(),
                )
            },
        )
    }

    // ========================================================================
    // DOCUMENT LIST MAPPING
    // ========================================================================

    fun fromRecommandDocumentsResponse(response: RecommandGetDocumentsResponse): PeppolDocumentList {
        val docs = response.documents.map { doc ->
            val counterparty = when (doc.direction) {
                RecommandDocumentDirection.Incoming -> doc.senderId
                RecommandDocumentDirection.Outgoing -> doc.receiverId
            }

            val parsedSummary = extractSummaryFromParsed(doc.type, doc.parsed)

            PeppolDocumentSummary(
                id = doc.id,
                documentType = recommandToDocumentType(doc.type),
                direction = when (doc.direction) {
                    RecommandDocumentDirection.Incoming -> PeppolTransmissionDirection.Inbound
                    RecommandDocumentDirection.Outgoing -> PeppolTransmissionDirection.Outbound
                },
                counterpartyPeppolId = counterparty,
                status = doc.validation.result.name.lowercase(),
                createdAt = doc.createdAt,
                readAt = doc.readAt,
                invoiceNumber = parsedSummary.invoiceNumber,
                totalAmount = parsedSummary.totalAmount,
                currency = parsedSummary.currency,
            )
        }

        return PeppolDocumentList(
            documents = docs,
            total = response.pagination.total.toInt(),
            hasMore = response.pagination.page < response.pagination.totalPages,
        )
    }

    private data class ParsedSummary(
        val invoiceNumber: String?,
        val totalAmount: Double?,
        val currency: String?,
    )

    private fun extractSummaryFromParsed(
        type: RecommandDocumentType,
        parsed: JsonElement?,
    ): ParsedSummary {
        if (parsed == null) return ParsedSummary(invoiceNumber = null, totalAmount = null, currency = null)

        return when (type) {
            RecommandDocumentType.Invoice -> {
                val invoice = json.decodeFromJsonElement<RecommandInvoice>(parsed)
                ParsedSummary(
                    invoiceNumber = invoice.invoiceNumber,
                    totalAmount = invoice.totals?.payableAmount?.toDoubleOrNull()
                        ?: invoice.totals?.taxInclusiveAmount?.toDoubleOrNull(),
                    currency = invoice.currency,
                )
            }
            RecommandDocumentType.CreditNote -> {
                val creditNote = json.decodeFromJsonElement<RecommandCreditNote>(parsed)
                ParsedSummary(
                    invoiceNumber = creditNote.creditNoteNumber,
                    totalAmount = creditNote.totals?.payableAmount?.toDoubleOrNull()
                        ?: creditNote.totals?.taxInclusiveAmount?.toDoubleOrNull(),
                    currency = creditNote.currency,
                )
            }
            RecommandDocumentType.SelfBillingInvoice -> {
                val invoice = json.decodeFromJsonElement<RecommandSelfBillingInvoice>(parsed)
                ParsedSummary(
                    invoiceNumber = invoice.invoiceNumber,
                    totalAmount = invoice.totals?.payableAmount?.toDoubleOrNull()
                        ?: invoice.totals?.taxInclusiveAmount?.toDoubleOrNull(),
                    currency = invoice.currency,
                )
            }
            RecommandDocumentType.SelfBillingCreditNote -> {
                val creditNote = json.decodeFromJsonElement<RecommandSelfBillingCreditNote>(parsed)
                ParsedSummary(
                    invoiceNumber = creditNote.creditNoteNumber,
                    totalAmount = creditNote.totals?.payableAmount?.toDoubleOrNull()
                        ?: creditNote.totals?.taxInclusiveAmount?.toDoubleOrNull(),
                    currency = creditNote.currency,
                )
            }
            RecommandDocumentType.MessageLevelResponse, RecommandDocumentType.Xml ->
                ParsedSummary(invoiceNumber = null, totalAmount = null, currency = null)
        }
    }

    private fun recommandToDocumentType(type: RecommandDocumentType): PeppolDocumentType = when (type) {
        RecommandDocumentType.Invoice -> PeppolDocumentType.Invoice
        RecommandDocumentType.CreditNote -> PeppolDocumentType.CreditNote
        RecommandDocumentType.SelfBillingInvoice -> PeppolDocumentType.SelfBillingInvoice
        RecommandDocumentType.SelfBillingCreditNote -> PeppolDocumentType.SelfBillingCreditNote
        RecommandDocumentType.MessageLevelResponse, RecommandDocumentType.Xml -> PeppolDocumentType.Xml
    }
}

