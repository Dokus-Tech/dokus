package tech.dokus.features.ai.models

import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.model.ExtractedBillFields
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.ExtractedExpenseFields
import tech.dokus.domain.model.ExtractedInvoiceFields
import tech.dokus.domain.model.ExtractedLineItem

/**
 * Convert DocumentAIResult to domain ExtractedDocumentData.
 *
 * This strips provenance information and creates a business-only DTO
 * suitable for persistence in the extracted_data column.
 */
fun DocumentAIResult.toExtractedDocumentData(): ExtractedDocumentData {
    return when (this) {
        is DocumentAIResult.Invoice -> ExtractedDocumentData(
            documentType = DocumentType.Invoice,
            rawText = rawText,
            invoice = extractedData.toInvoiceFields(),
            overallConfidence = confidence,
            fieldConfidences = extractedData.provenance?.toFieldConfidences() ?: emptyMap()
        )

        is DocumentAIResult.Bill -> ExtractedDocumentData(
            documentType = DocumentType.Bill,
            rawText = rawText,
            bill = extractedData.toBillFields(),
            overallConfidence = confidence,
            fieldConfidences = extractedData.provenance?.toFieldConfidences() ?: emptyMap()
        )

        is DocumentAIResult.Receipt -> ExtractedDocumentData(
            documentType = DocumentType.Expense,
            rawText = rawText,
            expense = extractedData.toExpenseFields(),
            overallConfidence = confidence,
            fieldConfidences = extractedData.provenance?.toFieldConfidences() ?: emptyMap()
        )

        is DocumentAIResult.Unknown -> ExtractedDocumentData(
            documentType = DocumentType.Unknown,
            rawText = rawText,
            overallConfidence = confidence
        )
    }
}

/**
 * Get the domain DocumentType for this result.
 */
fun DocumentAIResult.toDomainType(): DocumentType {
    return when (this) {
        is DocumentAIResult.Invoice -> DocumentType.Invoice
        is DocumentAIResult.Bill -> DocumentType.Bill
        is DocumentAIResult.Receipt -> DocumentType.Expense
        is DocumentAIResult.Unknown -> DocumentType.Unknown
    }
}

private fun ExtractedInvoiceData.toInvoiceFields(): ExtractedInvoiceFields {
    return ExtractedInvoiceFields(
        clientName = vendorName, // Invoice vendor is the sender, client is recipient in context
        clientVatNumber = vendorVatNumber,
        clientAddress = vendorAddress,
        invoiceNumber = invoiceNumber,
        issueDate = issueDate?.parseLocalDate(),
        dueDate = dueDate?.parseLocalDate(),
        items = lineItems.map { it.toExtractedLineItem() },
        subtotalAmount = subtotal?.parseMoney(),
        vatAmount = totalVatAmount?.parseMoney(),
        totalAmount = totalAmount?.parseMoney(),
        currency = Currency.fromDisplay(currency),
        paymentTerms = paymentTerms,
        bankAccount = iban
    )
}

private fun ExtractedBillData.toBillFields(): ExtractedBillFields {
    return ExtractedBillFields(
        supplierName = supplierName,
        supplierVatNumber = supplierVatNumber,
        supplierAddress = supplierAddress,
        invoiceNumber = invoiceNumber,
        issueDate = issueDate?.parseLocalDate(),
        dueDate = dueDate?.parseLocalDate(),
        amount = amount?.parseMoney(),
        vatAmount = vatAmount?.parseMoney(),
        vatRate = vatRate?.parseVatRate(),
        currency = Currency.fromDisplay(currency),
        category = category?.parseExpenseCategory(),
        description = description,
        notes = notes,
        paymentTerms = paymentTerms,
        bankAccount = bankAccount
    )
}

private fun ExtractedReceiptData.toExpenseFields(): ExtractedExpenseFields {
    return ExtractedExpenseFields(
        merchant = merchantName,
        merchantAddress = merchantAddress,
        merchantVatNumber = merchantVatNumber,
        date = transactionDate?.parseLocalDate(),
        amount = totalAmount?.parseMoney(),
        vatAmount = vatAmount?.parseMoney(),
        currency = Currency.fromDisplay(currency),
        category = suggestedCategory?.parseExpenseCategory()
    )
}

private fun InvoiceLineItem.toExtractedLineItem(): ExtractedLineItem {
    return ExtractedLineItem(
        description = description,
        quantity = quantity,
        unitPrice = unitPrice?.parseMoney(),
        vatRate = vatRate?.parseVatRate(),
        lineTotal = total?.parseMoney()
    )
}
