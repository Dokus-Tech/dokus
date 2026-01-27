package tech.dokus.features.ai.models

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.model.ExtractedBillFields
import tech.dokus.domain.model.ExtractedCreditNoteFields
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.ExtractedExpenseFields
import tech.dokus.domain.model.ExtractedInvoiceFields
import tech.dokus.domain.model.ExtractedLineItem
import tech.dokus.domain.model.ExtractedProFormaFields
import tech.dokus.domain.utils.json

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
    val grossAmount = totalAmount?.parseMoney() ?: amount?.parseMoney()
    return ExtractedBillFields(
        supplierName = supplierName,
        supplierVatNumber = supplierVatNumber,
        supplierAddress = supplierAddress,
        invoiceNumber = invoiceNumber,
        issueDate = issueDate?.parseLocalDate(),
        dueDate = dueDate?.parseLocalDate(),
        amount = grossAmount,
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

private fun ExtractedInvoiceData.toCreditNoteFields(): ExtractedCreditNoteFields {
    return ExtractedCreditNoteFields(
        counterpartyName = vendorName,
        counterpartyVatNumber = vendorVatNumber,
        counterpartyAddress = vendorAddress,
        creditNoteNumber = invoiceNumber, // Credit notes use same number field
        originalInvoiceNumber = creditNoteMeta?.originalDocumentReference,
        issueDate = issueDate?.parseLocalDate(),
        items = lineItems.map { it.toExtractedLineItem() },
        subtotalAmount = subtotal?.parseMoney(),
        vatAmount = totalVatAmount?.parseMoney(),
        totalAmount = totalAmount?.parseMoney(),
        currency = Currency.fromDisplay(currency),
        reason = creditNoteMeta?.creditReason
    )
}

private fun ExtractedInvoiceData.toProFormaFields(): ExtractedProFormaFields {
    return ExtractedProFormaFields(
        clientName = vendorName,
        clientVatNumber = vendorVatNumber,
        clientAddress = vendorAddress,
        proFormaNumber = invoiceNumber,
        issueDate = issueDate?.parseLocalDate(),
        validUntil = dueDate?.parseLocalDate(),
        items = lineItems.map { it.toExtractedLineItem() },
        subtotalAmount = subtotal?.parseMoney(),
        vatAmount = totalVatAmount?.parseMoney(),
        totalAmount = totalAmount?.parseMoney(),
        currency = Currency.fromDisplay(currency),
        termsAndConditions = paymentTerms
    )
}

private fun ExtractedExpenseData.toExpenseFieldsFromExpense(): ExtractedExpenseFields {
    return ExtractedExpenseFields(
        merchant = merchantName,
        date = date?.parseLocalDate(),
        amount = totalAmount?.parseMoney(),
        vatAmount = vatAmount?.parseMoney(),
        vatRate = vatRate?.parseVatRate(),
        currency = Currency.fromDisplay(currency),
        category = category?.parseExpenseCategory(),
        description = description
    )
}

// =============================================================================
// ClassifiedDocumentType / JsonElement extensions for Orchestrator
// =============================================================================

/**
 * Convert ClassifiedDocumentType to domain DocumentType.
 */
fun ClassifiedDocumentType.toDomainType(): DocumentType = when (this) {
    ClassifiedDocumentType.INVOICE -> DocumentType.Invoice
    ClassifiedDocumentType.CREDIT_NOTE -> DocumentType.CreditNote
    ClassifiedDocumentType.PRO_FORMA -> DocumentType.ProForma
    ClassifiedDocumentType.BILL -> DocumentType.Bill
    ClassifiedDocumentType.RECEIPT -> DocumentType.Receipt
    ClassifiedDocumentType.EXPENSE -> DocumentType.Expense
    ClassifiedDocumentType.UNKNOWN -> DocumentType.Unknown
}

/**
 * Convert extraction JsonElement to domain ExtractedDocumentData.
 *
 * @param documentType The classified document type to determine the extraction model.
 */
fun JsonElement.toExtractedDocumentData(documentType: DocumentType): ExtractedDocumentData? {
    return try {
        when (documentType) {
            DocumentType.Invoice -> {
                val data = json.decodeFromJsonElement<ExtractedInvoiceData>(this)
                ExtractedDocumentData(
                    documentType = documentType,
                    rawText = null,
                    invoice = data.toInvoiceFields(),
                    overallConfidence = null,
                    fieldConfidences = data.provenance?.toFieldConfidences() ?: emptyMap()
                )
            }

            DocumentType.Bill -> {
                val data = json.decodeFromJsonElement<ExtractedBillData>(this)
                ExtractedDocumentData(
                    documentType = documentType,
                    rawText = null,
                    bill = data.toBillFields(),
                    overallConfidence = null,
                    fieldConfidences = data.provenance?.toFieldConfidences() ?: emptyMap()
                )
            }

            DocumentType.Receipt -> {
                val data = json.decodeFromJsonElement<ExtractedReceiptData>(this)
                ExtractedDocumentData(
                    documentType = documentType,
                    rawText = null,
                    expense = data.toExpenseFields(),
                    overallConfidence = null,
                    fieldConfidences = data.provenance?.toFieldConfidences() ?: emptyMap()
                )
            }

            DocumentType.Expense -> {
                val data = json.decodeFromJsonElement<ExtractedExpenseData>(this)
                ExtractedDocumentData(
                    documentType = documentType,
                    rawText = null,
                    expense = data.toExpenseFieldsFromExpense(),
                    overallConfidence = null,
                    fieldConfidences = data.provenance?.toFieldConfidences() ?: emptyMap()
                )
            }

            DocumentType.CreditNote -> {
                val data = json.decodeFromJsonElement<ExtractedInvoiceData>(this)
                ExtractedDocumentData(
                    documentType = documentType,
                    rawText = null,
                    creditNote = data.toCreditNoteFields(),
                    overallConfidence = null,
                    fieldConfidences = data.provenance?.toFieldConfidences() ?: emptyMap()
                )
            }

            DocumentType.ProForma -> {
                val data = json.decodeFromJsonElement<ExtractedInvoiceData>(this)
                ExtractedDocumentData(
                    documentType = documentType,
                    rawText = null,
                    proForma = data.toProFormaFields(),
                    overallConfidence = null,
                    fieldConfidences = data.provenance?.toFieldConfidences() ?: emptyMap()
                )
            }

            DocumentType.Unknown -> null
            else -> null
        }
    } catch (e: Exception) {
        // If deserialization fails, return null
        null
    }
}
