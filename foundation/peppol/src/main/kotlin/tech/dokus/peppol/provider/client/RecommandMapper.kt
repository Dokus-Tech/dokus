package tech.dokus.peppol.provider.client

import tech.dokus.domain.enums.PeppolDocumentType.Companion.toApiValue
import tech.dokus.domain.model.RecommandDocumentsResponse
import tech.dokus.domain.model.RecommandInboxDocument
import tech.dokus.domain.model.RecommandInvoiceDocument
import tech.dokus.domain.model.RecommandLineItem
import tech.dokus.domain.model.RecommandParty
import tech.dokus.domain.model.RecommandPaymentMeans
import tech.dokus.domain.model.RecommandReceivedDocument
import tech.dokus.domain.model.RecommandSendDocumentType
import tech.dokus.domain.model.RecommandSendRequest
import tech.dokus.domain.model.RecommandSendResponse
import tech.dokus.domain.model.RecommandVerifyResponse
import tech.dokus.peppol.model.PeppolDirection
import tech.dokus.peppol.model.PeppolDocumentList
import tech.dokus.peppol.model.PeppolDocumentSummary
import tech.dokus.peppol.model.PeppolDocumentType
import tech.dokus.peppol.model.PeppolError
import tech.dokus.peppol.model.PeppolInboxItem
import tech.dokus.peppol.model.PeppolMonetaryTotals
import tech.dokus.peppol.model.PeppolParty
import tech.dokus.peppol.model.PeppolReceivedDocument
import tech.dokus.peppol.model.PeppolReceivedLineItem
import tech.dokus.peppol.model.PeppolSendRequest
import tech.dokus.peppol.model.PeppolSendResponse
import tech.dokus.peppol.model.PeppolTaxSubtotal
import tech.dokus.peppol.model.PeppolTaxTotal
import tech.dokus.peppol.model.PeppolVerifyResponse
import tech.dokus.domain.enums.RecommandDirection
import tech.dokus.domain.model.RecommandReceivedLineItem
import tech.dokus.peppol.model.PeppolLineItem

/**
 * Maps between provider-agnostic Peppol models and Recommand-specific models.
 */
object RecommandMapper {

    // ========================================================================
    // SEND REQUEST MAPPING
    // ========================================================================

    /**
     * Convert generic Peppol send request to Recommand-specific format.
     */
    fun toRecommandRequest(request: PeppolSendRequest): RecommandSendRequest {
        val invoice = request.invoice

        return RecommandSendRequest(
            recipient = request.recipientPeppolId,
            documentType = toRecommandDocumentType(request.documentType),
            document = RecommandInvoiceDocument(
                invoiceNumber = invoice.invoiceNumber,
                issueDate = invoice.issueDate.toString(),
                dueDate = invoice.dueDate.toString(),
                buyer = toRecommandParty(invoice.buyer),
                seller = toRecommandParty(invoice.seller),
                lineItems = invoice.lineItems.map { toRecommandLineItem(it) },
                note = invoice.note,
                buyerReference = invoice.buyer.companyNumber ?: invoice.buyer.vatNumber,
                paymentMeans = invoice.paymentInfo?.let {
                    RecommandPaymentMeans(
                        iban = it.iban,
                        bic = it.bic,
                        paymentMeansCode = it.paymentMeansCode,
                        paymentId = it.paymentId
                    )
                },
                documentCurrencyCode = invoice.currencyCode
            )
        )
    }

    private fun toRecommandParty(party: PeppolParty): RecommandParty {
        return RecommandParty(
            vatNumber = party.vatNumber,
            name = party.name,
            streetName = party.streetName,
            cityName = party.cityName,
            postalZone = party.postalZone,
            countryCode = party.countryCode,
            contactEmail = party.contactEmail,
            contactName = party.contactName
        )
    }

    private fun toRecommandLineItem(item: PeppolLineItem): RecommandLineItem {
        return RecommandLineItem(
            id = item.id,
            name = item.name,
            description = item.description,
            quantity = item.quantity,
            unitCode = item.unitCode,
            unitPrice = item.unitPrice,
            lineTotal = item.lineTotal,
            taxCategory = item.taxCategory,
            taxPercent = item.taxPercent
        )
    }

    // ========================================================================
    // SEND RESPONSE MAPPING
    // ========================================================================

    /**
     * Convert Recommand response to generic Peppol format.
     */
    fun fromRecommandResponse(response: RecommandSendResponse): PeppolSendResponse {
        return PeppolSendResponse(
            success = response.success,
            externalDocumentId = response.documentId,
            errorMessage = if (!response.success) {
                response.errors?.joinToString("; ") { it.message } ?: response.message
            } else null,
            errors = response.errors?.map {
                PeppolError(
                    code = it.code,
                    message = it.message,
                    field = it.field
                )
            } ?: emptyList()
        )
    }

    // ========================================================================
    // VERIFY RESPONSE MAPPING
    // ========================================================================

    fun fromRecommandVerifyResponse(response: RecommandVerifyResponse): PeppolVerifyResponse {
        return PeppolVerifyResponse(
            registered = response.registered,
            participantId = response.participantId,
            name = response.name,
            documentTypes = response.documentTypes ?: emptyList()
        )
    }

    // ========================================================================
    // INBOX MAPPING
    // ========================================================================

    fun fromRecommandInboxItem(item: RecommandInboxDocument): PeppolInboxItem {
        return PeppolInboxItem(
            id = item.id,
            documentType = item.documentType,
            senderPeppolId = item.sender,
            receiverPeppolId = item.receiver,
            receivedAt = item.receivedAt,
            isRead = item.isRead
        )
    }

    fun fromRecommandDocument(
        item: RecommandInboxDocument,
        document: RecommandReceivedDocument
    ): PeppolReceivedDocument {
        return PeppolReceivedDocument(
            id = item.id,
            documentType = item.documentType,
            senderPeppolId = item.sender,
            invoiceNumber = document.invoiceNumber,
            issueDate = document.issueDate,
            dueDate = document.dueDate,
            seller = document.seller?.let { fromRecommandParty(it) },
            buyer = document.buyer?.let { fromRecommandParty(it) },
            lineItems = document.lineItems?.map { fromRecommandReceivedLineItem(it) },
            totals = document.legalMonetaryTotal?.let {
                PeppolMonetaryTotals(
                    lineExtensionAmount = it.lineExtensionAmount,
                    taxExclusiveAmount = it.taxExclusiveAmount,
                    taxInclusiveAmount = it.taxInclusiveAmount,
                    payableAmount = it.payableAmount
                )
            },
            taxTotal = document.taxTotal?.let {
                PeppolTaxTotal(
                    taxAmount = it.taxAmount,
                    taxSubtotals = it.taxSubtotals?.map { sub ->
                        PeppolTaxSubtotal(
                            taxableAmount = sub.taxableAmount,
                            taxAmount = sub.taxAmount,
                            taxCategory = sub.taxCategory,
                            taxPercent = sub.taxPercent
                        )
                    }
                )
            },
            note = document.note,
            currencyCode = document.documentCurrencyCode
        )
    }

    private fun fromRecommandParty(party: RecommandParty): PeppolParty {
        return PeppolParty(
            name = party.name,
            vatNumber = party.vatNumber,
            streetName = party.streetName,
            cityName = party.cityName,
            postalZone = party.postalZone,
            countryCode = party.countryCode,
            contactEmail = party.contactEmail,
            contactName = party.contactName
        )
    }

    private fun fromRecommandReceivedLineItem(
        item: RecommandReceivedLineItem
    ): PeppolReceivedLineItem {
        return PeppolReceivedLineItem(
            id = item.id,
            name = item.name,
            description = item.description,
            quantity = item.quantity,
            unitCode = item.unitCode,
            unitPrice = item.unitPrice,
            lineTotal = item.lineExtensionAmount,
            taxCategory = item.taxCategory,
            taxPercent = item.taxPercent
        )
    }

    // ========================================================================
    // DOCUMENT LIST MAPPING
    // ========================================================================

    fun fromRecommandDocumentsResponse(response: RecommandDocumentsResponse): PeppolDocumentList {
        return PeppolDocumentList(
            documents = response.data.map { doc ->
                PeppolDocumentSummary(
                    id = doc.id,
                    documentType = doc.documentType.toApiValue(),
                    direction = when (doc.direction) {
                        RecommandDirection.Outgoing -> PeppolDirection.OUTBOUND
                        RecommandDirection.Incoming -> PeppolDirection.INBOUND
                    },
                    counterpartyPeppolId = doc.counterparty,
                    status = doc.status.toPeppolStatus().name,
                    createdAt = doc.createdAt,
                    invoiceNumber = doc.invoiceNumber,
                    totalAmount = doc.totalAmount,
                    currency = doc.currency?.code
                )
            },
            total = response.total,
            hasMore = response.hasMore
        )
    }

    // ========================================================================
    // DOCUMENT TYPE MAPPING
    // ========================================================================

    /**
     * Map provider-agnostic document type to Recommand-specific enum.
     */
    private fun toRecommandDocumentType(type: PeppolDocumentType): RecommandSendDocumentType {
        return when (type) {
            PeppolDocumentType.INVOICE -> RecommandSendDocumentType.Invoice
            PeppolDocumentType.CREDIT_NOTE -> RecommandSendDocumentType.CreditNote
            PeppolDocumentType.DEBIT_NOTE -> RecommandSendDocumentType.Invoice // Debit notes sent as invoice
            PeppolDocumentType.ORDER -> RecommandSendDocumentType.Xml // Orders require UBL XML
        }
    }
}
