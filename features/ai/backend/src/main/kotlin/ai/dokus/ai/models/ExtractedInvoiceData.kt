package ai.dokus.ai.models

import kotlinx.serialization.Serializable

/**
 * Extracted data from an invoice document.
 */
@Serializable
data class ExtractedInvoiceData(
    // Vendor information
    val vendorName: String? = null,
    val vendorVatNumber: String? = null,
    val vendorAddress: String? = null,

    // Invoice details
    val invoiceNumber: String? = null,
    val issueDate: String? = null,       // ISO format YYYY-MM-DD
    val dueDate: String? = null,
    val paymentTerms: String? = null,    // e.g., "Net 30"

    // Line items
    val lineItems: List<InvoiceLineItem> = emptyList(),

    // Totals
    val currency: String? = null,        // EUR, USD, etc.
    val subtotal: String? = null,
    val vatBreakdown: List<VatBreakdown> = emptyList(),
    val totalVatAmount: String? = null,
    val totalAmount: String? = null,

    // Payment information
    val iban: String? = null,
    val bic: String? = null,
    val paymentReference: String? = null,

    // Metadata
    val confidence: Double = 0.0
)

/**
 * A line item on an invoice.
 */
@Serializable
data class InvoiceLineItem(
    val description: String,
    val quantity: Double? = null,
    val unitPrice: String? = null,
    val vatRate: String? = null,         // e.g., "21%"
    val total: String? = null
)

/**
 * VAT breakdown by rate.
 */
@Serializable
data class VatBreakdown(
    val rate: String,                    // e.g., "21%"
    val base: String? = null,
    val amount: String? = null
)
