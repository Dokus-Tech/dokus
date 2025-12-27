package tech.dokus.backend.processor

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.Percentage
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.ExtractedBillFields
import ai.dokus.foundation.domain.model.ExtractedExpenseFields
import ai.dokus.foundation.domain.model.ExtractedInvoiceFields
import ai.dokus.foundation.domain.model.ExtractedLineItem
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Shared data classes for AI extraction response parsing.
 * Used by both OpenAI and Ollama extraction providers.
 */

@Serializable
internal data class ExtractedJsonResponse(
    val documentType: String,
    val confidence: Double,
    val fieldConfidences: Map<String, Double>? = null,
    val invoice: ExtractedInvoiceJson? = null,
    val bill: ExtractedBillJson? = null,
    val expense: ExtractedExpenseJson? = null
)

@Serializable
internal data class ExtractedInvoiceJson(
    val clientName: String? = null,
    val clientVatNumber: String? = null,
    val clientEmail: String? = null,
    val clientAddress: String? = null,
    val invoiceNumber: String? = null,
    val issueDate: String? = null,
    val dueDate: String? = null,
    val items: List<ExtractedLineItemJson>? = null,
    val subtotalAmount: String? = null,
    val vatAmount: String? = null,
    val totalAmount: String? = null,
    val currency: String? = null,
    val notes: String? = null,
    val paymentTerms: String? = null,
    val bankAccount: String? = null
) {
    fun toExtractedFields() = ExtractedInvoiceFields(
        clientName = clientName,
        clientVatNumber = clientVatNumber,
        clientEmail = clientEmail,
        clientAddress = clientAddress,
        invoiceNumber = invoiceNumber,
        issueDate = issueDate?.let { parseDate(it) },
        dueDate = dueDate?.let { parseDate(it) },
        items = items?.map { it.toExtractedLineItem() },
        subtotalAmount = subtotalAmount?.let { Money(it) },
        vatAmount = vatAmount?.let { Money(it) },
        totalAmount = totalAmount?.let { Money(it) },
        currency = currency?.let { parseCurrency(it) },
        notes = notes,
        paymentTerms = paymentTerms,
        bankAccount = bankAccount
    )
}

@Serializable
internal data class ExtractedLineItemJson(
    val description: String? = null,
    val quantity: Double? = null,
    val unitPrice: String? = null,
    val vatRate: String? = null,
    val lineTotal: String? = null
) {
    fun toExtractedLineItem() = ExtractedLineItem(
        description = description,
        quantity = quantity,
        unitPrice = unitPrice?.let { Money(it) },
        vatRate = vatRate?.let { VatRate(it) },
        lineTotal = lineTotal?.let { Money(it) }
    )
}

@Serializable
internal data class ExtractedBillJson(
    val supplierName: String? = null,
    val supplierVatNumber: String? = null,
    val supplierAddress: String? = null,
    val invoiceNumber: String? = null,
    val issueDate: String? = null,
    val dueDate: String? = null,
    val amount: String? = null,
    val vatAmount: String? = null,
    val vatRate: String? = null,
    val currency: String? = null,
    val category: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val paymentTerms: String? = null,
    val bankAccount: String? = null
) {
    fun toExtractedFields() = ExtractedBillFields(
        supplierName = supplierName,
        supplierVatNumber = supplierVatNumber,
        supplierAddress = supplierAddress,
        invoiceNumber = invoiceNumber,
        issueDate = issueDate?.let { parseDate(it) },
        dueDate = dueDate?.let { parseDate(it) },
        amount = amount?.let { Money(it) },
        vatAmount = vatAmount?.let { Money(it) },
        vatRate = vatRate?.let { VatRate(it) },
        currency = currency?.let { parseCurrency(it) },
        category = category?.let { parseCategory(it) },
        description = description,
        notes = notes,
        paymentTerms = paymentTerms,
        bankAccount = bankAccount
    )
}

@Serializable
internal data class ExtractedExpenseJson(
    val merchant: String? = null,
    val merchantAddress: String? = null,
    val merchantVatNumber: String? = null,
    val date: String? = null,
    val amount: String? = null,
    val vatAmount: String? = null,
    val vatRate: String? = null,
    val currency: String? = null,
    val category: String? = null,
    val description: String? = null,
    val isDeductible: Boolean? = null,
    val deductiblePercentage: Double? = null,
    val paymentMethod: String? = null,
    val notes: String? = null,
    val receiptNumber: String? = null
) {
    fun toExtractedFields() = ExtractedExpenseFields(
        merchant = merchant,
        merchantAddress = merchantAddress,
        merchantVatNumber = merchantVatNumber,
        date = date?.let { parseDate(it) },
        amount = amount?.let { Money(it) },
        vatAmount = vatAmount?.let { Money(it) },
        vatRate = vatRate?.let { VatRate(it) },
        currency = currency?.let { parseCurrency(it) },
        category = category?.let { parseCategory(it) },
        description = description,
        isDeductible = isDeductible,
        deductiblePercentage = deductiblePercentage?.let { Percentage(it.toString()) },
        paymentMethod = paymentMethod?.let { parsePaymentMethod(it) },
        notes = notes,
        receiptNumber = receiptNumber
    )
}

internal fun parseDate(dateStr: String): LocalDate? {
    return try {
        LocalDate.parse(dateStr)
    } catch (e: Exception) {
        null
    }
}

internal fun parseCategory(categoryStr: String): ExpenseCategory? {
    return try {
        ExpenseCategory.valueOf(categoryStr)
    } catch (e: Exception) {
        ExpenseCategory.Other
    }
}

internal fun parsePaymentMethod(methodStr: String): PaymentMethod? {
    return try {
        PaymentMethod.valueOf(methodStr)
    } catch (e: Exception) {
        PaymentMethod.Other
    }
}

internal fun parseCurrency(currencyStr: String): Currency? {
    return when (currencyStr.uppercase()) {
        "EUR" -> Currency.Eur
        "USD" -> Currency.Usd
        "GBP" -> Currency.Gbp
        "CHF" -> Currency.Chf
        "CAD" -> Currency.Cad
        "AUD" -> Currency.Aud
        else -> Currency.Eur  // Default to EUR for European context
    }
}
