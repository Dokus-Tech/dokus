package tech.dokus.peppol.provider.client.recommand.model

import kotlin.jvm.JvmInline
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Email fallback settings.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) request body field `email`.
 */
@Serializable
data class RecommandEmail(
    @SerialName("when")
    val whenToSend: RecommandEmailWhen = RecommandEmailWhen.OnPeppolFailure,
    val to: List<String>,
    val subject: String? = null,
    val htmlBody: String? = null,
)

/**
 * When to send the email when using email fallback.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) request body field `email.when`.
 */
@Serializable
enum class RecommandEmailWhen {
    @SerialName("always")
    Always,

    @SerialName("on_peppol_failure")
    OnPeppolFailure,
}

/**
 * PDF generation options.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) request body field `pdfGeneration`.
 */
@Serializable
data class RecommandPdfGeneration(
    val enabled: Boolean = false,
    val filename: String? = null,
)

/**
 * Party (seller/buyer) used in invoice and credit note payloads.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) as part of `document` payloads
 * - `GET /api/v1/documents` and `GET /api/v1/documents/{documentId}` as part of `parsed` document payloads
 */
@Serializable
data class RecommandParty(
    val vatNumber: String? = null,
    val enterpriseNumber: String? = null,
    val name: String,
    val street: String,
    val street2: String? = null,
    val city: String,
    val postalZone: String,
    val country: String,
    val email: String? = null,
    val phone: String? = null,
)

/**
 * Delivery information used in invoice and credit note payloads.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) as part of `document` payloads
 * - `GET /api/v1/documents` and `GET /api/v1/documents/{documentId}` as part of `parsed` document payloads
 */
@Serializable
data class RecommandDelivery(
    val date: LocalDate? = null,
    val locationIdentifier: RecommandLocationIdentifier? = null,
    val location: RecommandDeliveryLocation? = null,
    val recipientName: String? = null,
)

/**
 * Generic (scheme, identifier) structure used for item standard IDs and delivery location identifiers.
 *
 * Used in:
 * - `RecommandLine.standardId`
 * - `RecommandDelivery.locationIdentifier`
 */
@Serializable
data class RecommandLocationIdentifier(
    val scheme: String,
    val identifier: String,
)

/**
 * Delivery location.
 *
 * Used in:
 * - `RecommandDelivery.location` in invoice/credit note payloads.
 */
@Serializable
data class RecommandDeliveryLocation(
    val country: String,
    val street: String? = null,
    val street2: String? = null,
    val city: String? = null,
    val postalZone: String? = null,
)

/**
 * Payment means.
 *
 * Used in:
 * - `RecommandInvoice.paymentMeans`, `RecommandCreditNote.paymentMeans`, `RecommandSelfBillingInvoice.paymentMeans`,
 *   `RecommandSelfBillingCreditNote.paymentMeans`
 * - `RecommandSendInvoice.paymentMeans`, `RecommandSendCreditNote.paymentMeans`, `RecommandSendSelfBillingInvoice.paymentMeans`,
 *   `RecommandSendSelfBillingCreditNote.paymentMeans`
 */
@Serializable
data class RecommandPaymentMeans(
    val iban: String,
    val name: String? = null,
    val paymentMethod: RecommandPaymentMethod = RecommandPaymentMethod.CreditTransfer,
    val reference: String = "",
    val financialInstitutionBranch: String? = null,
)

/**
 * Payment method used in `RecommandPaymentMeans`.
 */
@Serializable
enum class RecommandPaymentMethod {
    @SerialName("cash")
    Cash,

    @SerialName("credit_transfer")
    CreditTransfer,

    @SerialName("debit_transfer")
    DebitTransfer,

    @SerialName("bank_card")
    BankCard,

    @SerialName("credit_card")
    CreditCard,

    @SerialName("debit_card")
    DebitCard,

    @SerialName("other")
    Other,
}

/**
 * Payment terms wrapper.
 *
 * Used in:
 * - `RecommandInvoice.paymentTerms`, `RecommandCreditNote.paymentTerms`, `RecommandSelfBillingInvoice.paymentTerms`,
 *   `RecommandSelfBillingCreditNote.paymentTerms`
 * - `RecommandSendInvoice.paymentTerms`, `RecommandSendCreditNote.paymentTerms`, `RecommandSendSelfBillingInvoice.paymentTerms`,
 *   `RecommandSendSelfBillingCreditNote.paymentTerms`
 */
@Serializable
data class RecommandPaymentTerms(
    val note: String,
)

/**
 * Invoice/credit note line item.
 *
 * Used in:
 * - `RecommandInvoice.lines`, `RecommandCreditNote.lines`, `RecommandSelfBillingInvoice.lines`,
 *   `RecommandSelfBillingCreditNote.lines`
 * - `RecommandSendInvoice.lines`, `RecommandSendCreditNote.lines`, `RecommandSendSelfBillingInvoice.lines`,
 *   `RecommandSendSelfBillingCreditNote.lines`
 */
@Serializable
data class RecommandLine(
    val netPriceAmount: String,
    val vat: RecommandVat,
    val id: String? = null,
    val name: String = "",
    val description: String? = null,
    val note: String? = null,
    val buyersId: String? = null,
    val sellersId: String? = null,
    val standardId: RecommandLocationIdentifier? = null,
    val documentReference: String? = null,
    val additionalItemProperties: List<RecommandAdditionalItemProperty>? = null,
    val originCountry: String? = null,
    val quantity: String = "1.00",
    val unitCode: String = "C62",
    val discounts: List<RecommandLineDiscount>? = null,
    val surcharges: List<RecommandLineSurcharge>? = null,
    val netAmount: String? = null,
)

/**
 * Additional item property for a line.
 *
 * Used in:
 * - `RecommandLine.additionalItemProperties`
 */
@Serializable
data class RecommandAdditionalItemProperty(
    val name: String,
    val value: String,
)

/**
 * Discount on a line level.
 *
 * Used in:
 * - `RecommandLine.discounts`
 */
@Serializable
data class RecommandLineDiscount(
    val amount: String,
    val reasonCode: String? = null,
    val reason: String? = null,
)

/**
 * Surcharge on a line level.
 *
 * Used in:
 * - `RecommandLine.surcharges`
 */
@Serializable
data class RecommandLineSurcharge(
    val amount: String,
    val reasonCode: String? = null,
    val reason: String? = null,
)

/**
 * VAT information for a line or a surcharge/discount.
 */
@Serializable
data class RecommandVat(
    val percentage: String,
    val category: RecommandVatCategory = RecommandVatCategory.S,
)

/**
 * VAT category code.
 */
@Serializable
enum class RecommandVatCategory {
    AE,
    E,
    S,
    Z,
    G,
    O,
    K,
    L,
    M,
    B,
}

/**
 * Global discount.
 *
 * Used in:
 * - `RecommandInvoice.discounts`, `RecommandCreditNote.discounts`, `RecommandSelfBillingInvoice.discounts`,
 *   `RecommandSelfBillingCreditNote.discounts`
 * - `RecommandSendInvoice.discounts`, `RecommandSendCreditNote.discounts`, `RecommandSendSelfBillingInvoice.discounts`,
 *   `RecommandSendSelfBillingCreditNote.discounts`
 */
@Serializable
data class RecommandDiscount(
    val amount: String,
    val vat: RecommandVat,
    val reasonCode: String? = null,
    val reason: String? = null,
)

/**
 * Global surcharge.
 *
 * Used in:
 * - `RecommandInvoice.surcharges`, `RecommandCreditNote.surcharges`, `RecommandSelfBillingInvoice.surcharges`,
 *   `RecommandSelfBillingCreditNote.surcharges`
 * - `RecommandSendInvoice.surcharges`, `RecommandSendCreditNote.surcharges`, `RecommandSendSelfBillingInvoice.surcharges`,
 *   `RecommandSendSelfBillingCreditNote.surcharges`
 */
@Serializable
data class RecommandSurcharge(
    val amount: String,
    val vat: RecommandVat,
    val reasonCode: String? = null,
    val reason: String? = null,
)

/**
 * Monetary totals.
 *
 * Used in:
 * - `RecommandInvoice.totals`, `RecommandCreditNote.totals`, `RecommandSelfBillingInvoice.totals`,
 *   `RecommandSelfBillingCreditNote.totals`
 * - `RecommandSendInvoice.totals`, `RecommandSendCreditNote.totals`, `RecommandSendSelfBillingInvoice.totals`,
 *   `RecommandSendSelfBillingCreditNote.totals`
 */
@Serializable
data class RecommandTotals(
    val taxExclusiveAmount: String,
    val taxInclusiveAmount: String,
    val linesAmount: String? = null,
    val discountAmount: String? = null,
    val surchargeAmount: String? = null,
    val payableAmount: String? = null,
    val paidAmount: String? = null,
)

/**
 * VAT totals (provided totals).
 *
 * Used in:
 * - Document payloads (`vat`) in parsed documents:
 *   `GET /api/v1/documents`, `GET /api/v1/documents/{documentId}`
 * - Document payloads (`vat`) in send documents when totals are explicitly provided:
 *   `POST /api/v1/{companyId}/send` (path params: `companyId`)
 */
@Serializable
data class RecommandVatTotals(
    val totalVatAmount: String,
    val subtotals: List<RecommandVatSubtotal>,
)

/**
 * VAT subtotal.
 *
 * Used in:
 * - `RecommandVatTotals.subtotals`
 */
@Serializable
data class RecommandVatSubtotal(
    val taxableAmount: String,
    val vatAmount: String,
    val category: RecommandVatCategory,
    val percentage: String,
    val exemptionReasonCode: String? = null,
    val exemptionReason: String? = null,
)

/**
 * VAT totals auto-calculation configuration.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) send document payload field `vat`
 *   (when the API should calculate VAT totals and an exemption reason needs to be provided).
 */
@Serializable
data class RecommandVatTotalsAutoCalculation(
    val exemptionReasonCode: String? = null,
    val exemptionReason: String? = null,
)

/**
 * Attachment (embedded or URL).
 *
 * Used in:
 * - Document payloads (`attachments`) in parsed documents:
 *   `GET /api/v1/documents`, `GET /api/v1/documents/{documentId}`
 * - Document payloads (`attachments`) in send documents:
 *   `POST /api/v1/{companyId}/send` (path params: `companyId`)
 */
@Serializable
data class RecommandAttachment(
    val id: String,
    val filename: String,
    val mimeCode: String = "application/pdf",
    val description: String? = null,
    val embeddedDocument: String? = null,
    val url: String? = null,
)

/**
 * Parsed Invoice returned in `documents[].parsed` and `document.parsed`.
 *
 * Used in:
 * - `GET /api/v1/documents` response field `documents[].parsed` (query params: `page`, `limit`, ...)
 * - `GET /api/v1/documents/{documentId}` response field `document.parsed` (path params: `documentId`)
 */
@Serializable
data class RecommandInvoice(
    val invoiceNumber: String,
    val issueDate: LocalDate,
    val seller: RecommandParty,
    val buyer: RecommandParty,
    val lines: List<RecommandLine>,
    val dueDate: LocalDate? = null,
    val note: String? = null,
    val buyerReference: String? = null,
    val purchaseOrderReference: String? = null,
    val salesOrderReference: String? = null,
    val despatchReference: String? = null,
    val delivery: RecommandDelivery? = null,
    val paymentMeans: List<RecommandPaymentMeans>? = null,
    val paymentTerms: RecommandPaymentTerms? = null,
    val discounts: List<RecommandDiscount>? = null,
    val surcharges: List<RecommandSurcharge>? = null,
    val totals: RecommandTotals? = null,
    val vat: RecommandVatTotals? = null,
    val attachments: List<RecommandAttachment>? = null,
    val currency: String = "EUR",
)

/**
 * Reference to an invoice credited by a credit note.
 */
@Serializable
data class RecommandInvoiceReference(
    val id: String,
    val issueDate: LocalDate? = null,
)

/**
 * Parsed Credit Note returned in `documents[].parsed` and `document.parsed`.
 *
 * Used in:
 * - `GET /api/v1/documents` response field `documents[].parsed` (query params: `page`, `limit`, ...)
 * - `GET /api/v1/documents/{documentId}` response field `document.parsed` (path params: `documentId`)
 */
@Serializable
data class RecommandCreditNote(
    val creditNoteNumber: String,
    val issueDate: LocalDate,
    val seller: RecommandParty,
    val buyer: RecommandParty,
    val lines: List<RecommandLine>,
    val note: String? = null,
    val buyerReference: String? = null,
    val invoiceReferences: List<RecommandInvoiceReference> = emptyList(),
    val purchaseOrderReference: String? = null,
    val salesOrderReference: String? = null,
    val despatchReference: String? = null,
    val delivery: RecommandDelivery? = null,
    val paymentMeans: List<RecommandPaymentMeans>? = null,
    val paymentTerms: RecommandPaymentTerms? = null,
    val discounts: List<RecommandDiscount>? = null,
    val surcharges: List<RecommandSurcharge>? = null,
    val totals: RecommandTotals? = null,
    val vat: RecommandVatTotals? = null,
    val attachments: List<RecommandAttachment>? = null,
    val currency: String = "EUR",
)

/**
 * Parsed Self Billing Invoice returned in `documents[].parsed` and `document.parsed`.
 *
 * Used in:
 * - `GET /api/v1/documents` response field `documents[].parsed` (query params: `page`, `limit`, ...)
 * - `GET /api/v1/documents/{documentId}` response field `document.parsed` (path params: `documentId`)
 */
@Serializable
data class RecommandSelfBillingInvoice(
    val invoiceNumber: String,
    val issueDate: LocalDate,
    val seller: RecommandParty,
    val buyer: RecommandParty,
    val lines: List<RecommandLine>,
    val currency: String = "EUR",
    val dueDate: LocalDate? = null,
    val note: String? = null,
    val buyerReference: String? = null,
    val purchaseOrderReference: String? = null,
    val salesOrderReference: String? = null,
    val despatchReference: String? = null,
    val delivery: RecommandDelivery? = null,
    val paymentMeans: List<RecommandPaymentMeans>? = null,
    val paymentTerms: RecommandPaymentTerms? = null,
    val discounts: List<RecommandDiscount>? = null,
    val surcharges: List<RecommandSurcharge>? = null,
    val totals: RecommandTotals? = null,
    val vat: RecommandVatTotals? = null,
    val attachments: List<RecommandAttachment>? = null,
)

/**
 * Parsed Self Billing Credit Note returned in `documents[].parsed` and `document.parsed`.
 *
 * Used in:
 * - `GET /api/v1/documents` response field `documents[].parsed` (query params: `page`, `limit`, ...)
 * - `GET /api/v1/documents/{documentId}` response field `document.parsed` (path params: `documentId`)
 */
@Serializable
data class RecommandSelfBillingCreditNote(
    val creditNoteNumber: String,
    val issueDate: LocalDate,
    val invoiceReferences: List<RecommandInvoiceReference> = emptyList(),
    val seller: RecommandParty,
    val buyer: RecommandParty,
    val lines: List<RecommandLine>,
    val currency: String = "EUR",
    val note: String? = null,
    val buyerReference: String? = null,
    val purchaseOrderReference: String? = null,
    val salesOrderReference: String? = null,
    val despatchReference: String? = null,
    val delivery: RecommandDelivery? = null,
    val paymentMeans: List<RecommandPaymentMeans>? = null,
    val paymentTerms: RecommandPaymentTerms? = null,
    val discounts: List<RecommandDiscount>? = null,
    val surcharges: List<RecommandSurcharge>? = null,
    val totals: RecommandTotals? = null,
    val vat: RecommandVatTotals? = null,
    val attachments: List<RecommandAttachment>? = null,
)

/**
 * Message Level Response received from a recipient.
 *
 * Used in:
 * - `GET /api/v1/documents` and `GET /api/v1/documents/{documentId}` for documents with `type=messageLevelResponse`
 *   (the document may be represented in other fields depending on the API implementation).
 */
@Serializable
data class RecommandMessageLevelResponse(
    val id: String,
    val issueDate: LocalDate,
    val responseCode: RecommandMessageLevelResponseCode,
    val envelopeId: String,
)

/**
 * Message level response code.
 */
@Serializable
enum class RecommandMessageLevelResponseCode {
    AB,
    AP,
    RE,
}

/**
 * Message Level Response to send.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) when `documentType=messageLevelResponse`.
 */
@Serializable
data class RecommandSendMessageLevelResponse(
    val responseCode: RecommandMessageLevelResponseCode,
    val envelopeId: String,
    val id: String? = null,
    val issueDate: LocalDate? = null,
)

/**
 * XML document as a string.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) when `documentType=xml`.
 */
@JvmInline
@Serializable
value class RecommandXmlDocument(
    val value: String,
)

/**
 * Invoice payload for sending.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) when `documentType=invoice`.
 */
@Serializable
data class RecommandSendInvoice(
    val invoiceNumber: String,
    val buyer: RecommandParty,
    val lines: List<RecommandLine>,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val note: String? = null,
    val buyerReference: String? = null,
    val purchaseOrderReference: String? = null,
    val salesOrderReference: String? = null,
    val despatchReference: String? = null,
    val seller: RecommandParty? = null,
    val delivery: RecommandDelivery? = null,
    val paymentMeans: List<RecommandPaymentMeans>? = null,
    val paymentTerms: RecommandPaymentTerms? = null,
    val discounts: List<RecommandDiscount>? = null,
    val surcharges: List<RecommandSurcharge>? = null,
    val totals: RecommandTotals? = null,
    /**
     * One of:
     * - `RecommandVatTotals`
     * - `RecommandVatTotalsAutoCalculation`
     * - `null`
     */
    val vat: JsonElement? = null,
    val attachments: List<RecommandAttachment>? = null,
    val currency: String = "EUR",
)

/**
 * Credit note payload for sending.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) when `documentType=creditNote`.
 */
@Serializable
data class RecommandSendCreditNote(
    val creditNoteNumber: String,
    val buyer: RecommandParty,
    val lines: List<RecommandLine>,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val note: String? = null,
    val buyerReference: String? = null,
    val invoiceReferences: List<RecommandInvoiceReference> = emptyList(),
    val purchaseOrderReference: String? = null,
    val salesOrderReference: String? = null,
    val despatchReference: String? = null,
    val seller: RecommandParty? = null,
    val delivery: RecommandDelivery? = null,
    val paymentMeans: List<RecommandPaymentMeans>? = null,
    val paymentTerms: RecommandPaymentTerms? = null,
    val discounts: List<RecommandDiscount>? = null,
    val surcharges: List<RecommandSurcharge>? = null,
    val totals: RecommandTotals? = null,
    /**
     * One of:
     * - `RecommandVatTotals`
     * - `RecommandVatTotalsAutoCalculation`
     * - `null`
     */
    val vat: JsonElement? = null,
    val attachments: List<RecommandAttachment>? = null,
    val currency: String = "EUR",
)

/**
 * Self billing invoice payload for sending.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) when `documentType=selfBillingInvoice`.
 */
@Serializable
data class RecommandSendSelfBillingInvoice(
    val invoiceNumber: String,
    val seller: RecommandParty,
    val lines: List<RecommandLine>,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val note: String? = null,
    val buyerReference: String? = null,
    val purchaseOrderReference: String? = null,
    val salesOrderReference: String? = null,
    val despatchReference: String? = null,
    val buyer: RecommandParty? = null,
    val delivery: RecommandDelivery? = null,
    val paymentMeans: List<RecommandPaymentMeans>? = null,
    val paymentTerms: RecommandPaymentTerms? = null,
    val discounts: List<RecommandDiscount>? = null,
    val surcharges: List<RecommandSurcharge>? = null,
    val totals: RecommandTotals? = null,
    /**
     * One of:
     * - `RecommandVatTotals`
     * - `RecommandVatTotalsAutoCalculation`
     * - `null`
     */
    val vat: JsonElement? = null,
    val attachments: List<RecommandAttachment>? = null,
    val currency: String = "EUR",
)

/**
 * Self billing credit note payload for sending.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) when `documentType=selfBillingCreditNote`.
 */
@Serializable
data class RecommandSendSelfBillingCreditNote(
    val creditNoteNumber: String,
    val seller: RecommandParty,
    val lines: List<RecommandLine>,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val note: String? = null,
    val buyerReference: String? = null,
    val invoiceReferences: List<RecommandInvoiceReference> = emptyList(),
    val purchaseOrderReference: String? = null,
    val salesOrderReference: String? = null,
    val despatchReference: String? = null,
    val buyer: RecommandParty? = null,
    val delivery: RecommandDelivery? = null,
    val paymentMeans: List<RecommandPaymentMeans>? = null,
    val paymentTerms: RecommandPaymentTerms? = null,
    val discounts: List<RecommandDiscount>? = null,
    val surcharges: List<RecommandSurcharge>? = null,
    val totals: RecommandTotals? = null,
    /**
     * One of:
     * - `RecommandVatTotals`
     * - `RecommandVatTotalsAutoCalculation`
     * - `null`
     */
    val vat: JsonElement? = null,
    val attachments: List<RecommandAttachment>? = null,
    val currency: String = "EUR",
)

