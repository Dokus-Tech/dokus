package tech.dokus.features.ai.validation

import tech.dokus.domain.Money
import tech.dokus.features.ai.graph.sub.extraction.financial.CreditNoteExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.InvoiceExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.ProFormaExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.PurchaseOrderExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.QuoteExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.ReceiptExtractionResult
import tech.dokus.features.ai.models.FinancialExtractionResult

object FinancialExtractionAuditor {
    fun audit(extraction: FinancialExtractionResult): AuditReport {
        val checks = when (extraction) {
            is FinancialExtractionResult.Invoice -> auditInvoice(extraction.data)
            is FinancialExtractionResult.CreditNote -> auditCreditNote(extraction.data)
            is FinancialExtractionResult.Quote -> auditQuote(extraction.data)
            is FinancialExtractionResult.ProForma -> auditProForma(extraction.data)
            is FinancialExtractionResult.PurchaseOrder -> auditPurchaseOrder(extraction.data)
            is FinancialExtractionResult.Receipt -> auditReceipt(extraction.data)
            is FinancialExtractionResult.Unsupported -> emptyList()
        }

        return if (checks.isEmpty()) {
            AuditReport.EMPTY
        } else {
            AuditReport.fromChecks(checks)
        }
    }

    private fun auditInvoice(data: InvoiceExtractionResult): List<AuditCheck> = buildList {
        val subtotal = data.subtotalAmount
        val vat = data.vatAmount
        val total = data.totalAmount

        add(MathValidator.verifyTotals(subtotal, vat, total))
        add(BelgianVatRateValidator.verify(subtotal, vat, data.issueDate, null))
        addAll(LineItemsValidator.verify(data.lineItems, subtotal, required = true))
        addAll(
            VatBreakdownValidator.verify(
                vatBreakdown = data.vatBreakdown,
                subtotal = subtotal,
                vatAmount = vat,
                documentDate = data.issueDate,
                required = true
            )
        )
        add(ChecksumValidator.auditIban(data.iban))
        add(ChecksumValidator.auditOgm(data.payment?.structuredComm))
    }

    private fun auditReceipt(data: ReceiptExtractionResult): List<AuditCheck> = buildList {
        val total = data.totalAmount
        val vat = data.vatAmount
        val subtotal = derivedSubtotal(total, vat)

        add(BelgianVatRateValidator.verify(subtotal, vat, data.date, null))
        addAll(LineItemsValidator.verify(data.lineItems, subtotal, required = false))
        addAll(
            VatBreakdownValidator.verify(
                vatBreakdown = data.vatBreakdown,
                subtotal = subtotal,
                vatAmount = vat,
                documentDate = data.date,
                required = false
            )
        )
    }

    private fun auditCreditNote(data: CreditNoteExtractionResult): List<AuditCheck> = buildList {
        val subtotal = data.subtotalAmount
        val vat = data.vatAmount
        val total = data.totalAmount

        add(MathValidator.verifyTotals(subtotal, vat, total))
        add(BelgianVatRateValidator.verify(subtotal, vat, data.issueDate, null))
        addAll(LineItemsValidator.verify(data.lineItems, subtotal, required = true))
        addAll(
            VatBreakdownValidator.verify(
                vatBreakdown = data.vatBreakdown,
                subtotal = subtotal,
                vatAmount = vat,
                documentDate = data.issueDate,
                required = true
            )
        )
    }

    private fun auditQuote(data: QuoteExtractionResult): List<AuditCheck> = buildList {
        val subtotal = data.subtotalAmount
        val vat = data.vatAmount
        val total = data.totalAmount

        add(MathValidator.verifyTotals(subtotal, vat, total))
        add(BelgianVatRateValidator.verify(subtotal, vat, data.issueDate, null))
        add(ChecksumValidator.auditIban(data.iban))
        add(ChecksumValidator.auditOgm(data.payment?.structuredComm))
    }

    private fun auditProForma(data: ProFormaExtractionResult): List<AuditCheck> = buildList {
        val subtotal = data.subtotalAmount
        val vat = data.vatAmount
        val total = data.totalAmount

        add(MathValidator.verifyTotals(subtotal, vat, total))
        add(BelgianVatRateValidator.verify(subtotal, vat, data.issueDate, null))
    }

    private fun auditPurchaseOrder(data: PurchaseOrderExtractionResult): List<AuditCheck> = buildList {
        val subtotal = data.subtotalAmount
        val vat = data.vatAmount
        val total = data.totalAmount

        add(MathValidator.verifyTotals(subtotal, vat, total))
        add(BelgianVatRateValidator.verify(subtotal, vat, data.orderDate, null))
        add(ChecksumValidator.auditIban(data.iban))
        add(ChecksumValidator.auditOgm(data.payment?.structuredComm))
    }

    private fun derivedSubtotal(total: Money?, vat: Money?): Money? {
        if (total == null || vat == null) return null
        return total - vat
    }
}
