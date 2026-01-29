package tech.dokus.features.ai.validation

import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.features.ai.models.old.BillLineItem
import tech.dokus.features.ai.models.old.ExtractedBillData
import tech.dokus.features.ai.models.old.ExtractedExpenseData
import tech.dokus.features.ai.models.old.ExtractedInvoiceData
import tech.dokus.features.ai.models.old.ExtractedReceiptData
import tech.dokus.features.ai.models.old.InvoiceLineItem

/**
 * Layer 3: Legally-Aware Auditor
 *
 * Validates extracted document data against Belgian compliance rules.
 * Produces an AuditReport with detailed checks and hints for self-correction.
 *
 * Validation checks include:
 * - Math verification (subtotal + VAT = total, line items sum)
 * - Checksum validation (OGM payment reference, IBAN)
 * - VAT rate sanity check (Belgian rates: 0%, 6%, 12%, 21%)
 * - March 2026 reform awareness for 12% Horeca rate
 */
class ExtractionAuditService {

    /**
     * Audit an extracted invoice.
     */
    fun auditInvoice(invoice: ExtractedInvoiceData): AuditReport {
        val checks = mutableListOf<AuditCheck>()

        // Math validations
        checks += auditInvoiceMath(invoice)

        // Checksum validations
        checks += ChecksumValidator.auditOgm(invoice.paymentReference)
        checks += ChecksumValidator.auditIban(invoice.iban)

        // VAT rate validation
        checks += auditInvoiceVatRate(invoice)

        // Line item math (if present)
        checks += auditInvoiceLineItems(invoice)

        return AuditReport.fromChecks(checks)
    }

    /**
     * Audit an extracted bill.
     */
    fun auditBill(bill: ExtractedBillData): AuditReport {
        val checks = mutableListOf<AuditCheck>()

        // Math validations
        checks += auditBillMath(bill)

        // Checksum validations
        checks += ChecksumValidator.auditIban(bill.bankAccount)

        // VAT rate validation
        checks += auditBillVatRate(bill)

        // Line item math (if present)
        checks += auditBillLineItems(bill)

        return AuditReport.fromChecks(checks)
    }

    /**
     * Audit an extracted expense.
     */
    fun auditExpense(expense: ExtractedExpenseData): AuditReport {
        val checks = mutableListOf<AuditCheck>()

        // Math validation
        checks += auditExpenseMath(expense)

        // VAT rate validation
        checks += auditExpenseVatRate(expense)

        return AuditReport.fromChecks(checks)
    }

    /**
     * Audit an extracted receipt.
     */
    fun auditReceipt(receipt: ExtractedReceiptData): AuditReport {
        val checks = mutableListOf<AuditCheck>()

        // Math validation
        checks += auditReceiptMath(receipt)

        // VAT rate validation (if available)
        checks += auditReceiptVatRate(receipt)

        return AuditReport.fromChecks(checks)
    }

    // =========================================================================
    // Invoice-specific validations
    // =========================================================================

    private fun auditInvoiceMath(invoice: ExtractedInvoiceData): AuditCheck {
        val subtotal = invoice.subtotal?.let { Money.parse(it) }
        val vatAmount = invoice.totalVatAmount?.let { Money.parse(it) }
        val total = invoice.totalAmount?.let { Money.parse(it) }

        return MathValidator.verifyTotals(subtotal, vatAmount, total)
    }

    private fun auditInvoiceVatRate(invoice: ExtractedInvoiceData): AuditCheck {
        val subtotal = invoice.subtotal?.let { Money.parse(it) }
        val vatAmount = invoice.totalVatAmount?.let { Money.parse(it) }
        val documentDate = invoice.issueDate?.let { parseDate(it) }

        // No category available for invoices, pass null
        return BelgianVatRateValidator.verify(subtotal, vatAmount, documentDate, null)
    }

    private fun auditInvoiceLineItems(invoice: ExtractedInvoiceData): List<AuditCheck> {
        val checks = mutableListOf<AuditCheck>()

        if (invoice.lineItems.isEmpty()) {
            return checks
        }

        // Verify sum of line items equals subtotal
        val lineItemTotals = invoice.lineItems.mapNotNull { it.total?.let { t -> Money.parse(t) } }
        val subtotal = invoice.subtotal?.let { Money.parse(it) }

        if (lineItemTotals.isNotEmpty()) {
            checks += MathValidator.verifyLineItems(lineItemTotals, subtotal)
        }

        // Verify individual line item calculations
        invoice.lineItems.forEachIndexed { index, item ->
            checks += verifyInvoiceLineItemMath(item, index + 1)
        }

        return checks
    }

    private fun verifyInvoiceLineItemMath(item: InvoiceLineItem, lineIndex: Int): AuditCheck {
        val quantity = item.quantity
        val unitPrice = item.unitPrice?.let { Money.parse(it) }
        val lineTotal = item.total?.let { Money.parse(it) }

        return MathValidator.verifyLineItemCalculation(quantity, unitPrice, lineTotal, lineIndex)
    }

    // =========================================================================
    // Bill-specific validations
    // =========================================================================

    private fun auditBillMath(bill: ExtractedBillData): AuditCheck {
        val amounts = resolveBillAmounts(bill)

        return MathValidator.verifyTotals(amounts.net, amounts.vat, amounts.gross)
    }

    private fun auditBillVatRate(bill: ExtractedBillData): AuditCheck {
        val amounts = resolveBillAmounts(bill)
        val documentDate = bill.issueDate?.let { parseDate(it) }
        val category = bill.category

        return BelgianVatRateValidator.verify(amounts.net, amounts.vat, documentDate, category)
    }

    private fun auditBillLineItems(bill: ExtractedBillData): List<AuditCheck> {
        val checks = mutableListOf<AuditCheck>()

        if (bill.lineItems.isEmpty()) {
            return checks
        }

        val amounts = resolveBillAmounts(bill)
        val lineItemTotals = bill.lineItems
            .filterNot(::isIncludedFeeLineItem)
            .mapNotNull { it.total?.let { t -> Money.parse(t) } }

        if (lineItemTotals.isNotEmpty()) {
            checks += MathValidator.verifyLineItems(lineItemTotals, amounts.net)
        }

        // Verify individual line item calculations
        bill.lineItems.forEachIndexed { index, item ->
            checks += verifyBillLineItemMath(item, index + 1)
        }

        return checks
    }

    private fun verifyBillLineItemMath(item: BillLineItem, lineIndex: Int): AuditCheck {
        val quantity = item.quantity
        val unitPrice = item.unitPrice?.let { Money.parse(it) }
        val lineTotal = item.total?.let { Money.parse(it) }

        return MathValidator.verifyLineItemCalculation(quantity, unitPrice, lineTotal, lineIndex)
    }

    private data class BillAmountContext(
        val net: Money?,
        val vat: Money?,
        val gross: Money?
    )

    private fun resolveBillAmounts(bill: ExtractedBillData): BillAmountContext {
        val amount = bill.amount?.let { Money.parse(it) }
        val explicitTotal = bill.totalAmount?.let { Money.parse(it) }
        val vatAmount = bill.vatAmount?.let { Money.parse(it) }
        val gross = explicitTotal ?: amount
        val net = when {
            explicitTotal != null && amount != null && explicitTotal != amount -> amount
            gross != null && vatAmount != null -> gross - vatAmount
            else -> null
        }

        return BillAmountContext(net = net, vat = vatAmount, gross = gross)
    }

    private fun isIncludedFeeLineItem(item: BillLineItem): Boolean {
        val normalized = item.description
            .lowercase()
            .replace('\n', ' ')
            .replace('\t', ' ')
            .trim()

        return normalized.startsWith("incl ") ||
            normalized.startsWith("incl.") ||
            normalized.startsWith("included ") ||
            normalized.startsWith("inclusief ") ||
            normalized.contains("recupel") ||
            normalized.contains("auvibel")
    }

    // =========================================================================
    // Expense-specific validations
    // =========================================================================

    private fun auditExpenseMath(expense: ExtractedExpenseData): AuditCheck {
        // Expense may not have subtotal - derive it from total - VAT if possible
        val vatAmount = expense.vatAmount?.let { Money.parse(it) }
        val total = expense.totalAmount?.let { Money.parse(it) }

        // If we have total and VAT, we can derive subtotal and verify
        val subtotal = if (total != null && vatAmount != null) {
            total - vatAmount
        } else {
            null
        }

        return MathValidator.verifyTotals(subtotal, vatAmount, total)
    }

    private fun auditExpenseVatRate(expense: ExtractedExpenseData): AuditCheck {
        val vatAmount = expense.vatAmount?.let { Money.parse(it) }
        val total = expense.totalAmount?.let { Money.parse(it) }
        val documentDate = expense.date?.let { parseDate(it) }
        val category = expense.category

        // Derive subtotal from total - VAT for VAT rate calculation
        val subtotal = if (total != null && vatAmount != null) {
            total - vatAmount
        } else {
            null
        }

        return BelgianVatRateValidator.verify(subtotal, vatAmount, documentDate, category)
    }

    // =========================================================================
    // Receipt-specific validations
    // =========================================================================

    private fun auditReceiptMath(receipt: ExtractedReceiptData): AuditCheck {
        val subtotal = receipt.subtotal?.let { Money.parse(it) }
        val vatAmount = receipt.vatAmount?.let { Money.parse(it) }
        val total = receipt.totalAmount?.let { Money.parse(it) }

        return MathValidator.verifyTotals(subtotal, vatAmount, total)
    }

    private fun auditReceiptVatRate(receipt: ExtractedReceiptData): AuditCheck {
        val subtotal = receipt.subtotal?.let { Money.parse(it) }
        val vatAmount = receipt.vatAmount?.let { Money.parse(it) }
        val documentDate = receipt.transactionDate?.let { parseDate(it) }
        val category = receipt.suggestedCategory

        return BelgianVatRateValidator.verify(subtotal, vatAmount, documentDate, category)
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    /**
     * Parse a date string in ISO format (YYYY-MM-DD) to LocalDate.
     */
    private fun parseDate(dateString: String): LocalDate? {
        return try {
            LocalDate.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}
