package ai.dokus.ai.models

import kotlinx.serialization.Serializable

/**
 * Extracted data from a receipt document.
 */
@Serializable
data class ExtractedReceiptData(
    // Merchant information
    val merchantName: String? = null,
    val merchantAddress: String? = null,
    val merchantVatNumber: String? = null,

    // Transaction details
    val receiptNumber: String? = null,
    val transactionDate: String? = null,  // ISO format YYYY-MM-DD
    val transactionTime: String? = null,  // HH:mm format

    // Items (simplified compared to invoices)
    val items: List<ReceiptItem> = emptyList(),

    // Totals
    val currency: String? = null,
    val subtotal: String? = null,
    val vatAmount: String? = null,
    val totalAmount: String? = null,

    // Payment
    val paymentMethod: String? = null,    // Cash, Card, etc.
    val cardLastFour: String? = null,     // Last 4 digits if card payment

    // Category suggestion
    val suggestedCategory: String? = null,

    // Metadata
    val confidence: Double = 0.0
)

/**
 * An item on a receipt.
 */
@Serializable
data class ReceiptItem(
    val description: String,
    val quantity: Double? = null,
    val price: String? = null
)
