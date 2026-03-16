package tech.dokus.domain.model

import tech.dokus.domain.enums.SourceTrust
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.IngestionRunId

/**
 * Builds a typed [DocumentFieldProvenance] for a [DocumentDraftData].
 *
 * Only populated (non-null, non-default) fields get provenance entries.
 * All fields from the same extraction share the same base provenance.
 */
fun DocumentDraftData.buildProvenance(
    sourceTrust: SourceTrust,
    sourceId: DocumentSourceId? = null,
    sourceRunId: IngestionRunId? = null,
    extractionConfidence: Double? = null,
): DocumentFieldProvenance {
    val base = FieldProvenance(
        sourceId = sourceId,
        sourceRunId = sourceRunId,
        sourceTrust = sourceTrust,
        extractionConfidence = extractionConfidence,
    )
    return when (this) {
        is InvoiceDraftData -> buildInvoiceProvenance(base)
        is CreditNoteDraftData -> buildCreditNoteProvenance(base)
        is ReceiptDraftData -> buildReceiptProvenance(base)
        is BankStatementDraftData -> buildBankStatementProvenance(base)
        is ProFormaDraftData,
        is QuoteDraftData,
        is OrderConfirmationDraftData,
        is DeliveryNoteDraftData,
        is ReminderDraftData,
        is StatementOfAccountDraftData,
        is PurchaseOrderDraftData,
        is ExpenseClaimDraftData,
        is BankFeeDraftData,
        is InterestStatementDraftData,
        is PaymentConfirmationDraftData,
        is VatReturnDraftData,
        is VatListingDraftData,
        is VatAssessmentDraftData,
        is IcListingDraftData,
        is OssReturnDraftData,
        is CorporateTaxDraftData,
        is CorporateTaxAdvanceDraftData,
        is TaxAssessmentDraftData,
        is PersonalTaxDraftData,
        is WithholdingTaxDraftData,
        is SocialContributionDraftData,
        is SocialFundDraftData,
        is SelfEmployedContributionDraftData,
        is VapzDraftData,
        is SalarySlipDraftData,
        is PayrollSummaryDraftData,
        is EmploymentContractDraftData,
        is DimonaDraftData,
        is C4DraftData,
        is HolidayPayDraftData,
        is ContractDraftData,
        is LeaseDraftData,
        is LoanDraftData,
        is InsuranceDraftData,
        is DividendDraftData,
        is ShareholderRegisterDraftData,
        is CompanyExtractDraftData,
        is AnnualAccountsDraftData,
        is BoardMinutesDraftData,
        is SubsidyDraftData,
        is FineDraftData,
        is PermitDraftData,
        is CustomsDeclarationDraftData,
        is IntrastatDraftData,
        is DepreciationScheduleDraftData,
        is InventoryDraftData,
        is OtherDraftData -> DirectionOnlyFieldProvenance(direction = base)
    }
}

private fun InvoiceDraftData.buildInvoiceProvenance(base: FieldProvenance) = InvoiceFieldProvenance(
    direction = base,
    invoiceNumber = if (invoiceNumber != null) base else null,
    issueDate = if (issueDate != null) base else null,
    dueDate = if (dueDate != null) base else null,
    currency = base,
    subtotalAmount = if (subtotalAmount != null) base else null,
    vatAmount = if (vatAmount != null) base else null,
    totalAmount = if (totalAmount != null) base else null,
    lineItems = if (lineItems.isNotEmpty()) base else null,
    vatBreakdown = if (vatBreakdown.isNotEmpty()) base else null,
    iban = if (iban != null) base else null,
    payment = if (payment != null) base else null,
    notes = if (notes != null) base else null,
    seller = seller.buildPartyProvenance(base),
    buyer = buyer.buildPartyProvenance(base),
)

private fun CreditNoteDraftData.buildCreditNoteProvenance(base: FieldProvenance) = CreditNoteFieldProvenance(
    direction = base,
    creditNoteNumber = if (creditNoteNumber != null) base else null,
    issueDate = if (issueDate != null) base else null,
    currency = base,
    subtotalAmount = if (subtotalAmount != null) base else null,
    vatAmount = if (vatAmount != null) base else null,
    totalAmount = if (totalAmount != null) base else null,
    lineItems = if (lineItems.isNotEmpty()) base else null,
    vatBreakdown = if (vatBreakdown.isNotEmpty()) base else null,
    originalInvoiceNumber = if (originalInvoiceNumber != null) base else null,
    reason = if (reason != null) base else null,
    notes = if (notes != null) base else null,
    seller = seller.buildPartyProvenance(base),
    buyer = buyer.buildPartyProvenance(base),
)

private fun ReceiptDraftData.buildReceiptProvenance(base: FieldProvenance) = ReceiptFieldProvenance(
    direction = base,
    merchantName = if (merchantName != null) base else null,
    merchantVat = if (merchantVat != null) base else null,
    date = if (date != null) base else null,
    currency = base,
    totalAmount = if (totalAmount != null) base else null,
    vatAmount = if (vatAmount != null) base else null,
    lineItems = if (lineItems.isNotEmpty()) base else null,
    vatBreakdown = if (vatBreakdown.isNotEmpty()) base else null,
    receiptNumber = if (receiptNumber != null) base else null,
    paymentMethod = if (paymentMethod != null) base else null,
    notes = if (notes != null) base else null,
)

private fun BankStatementDraftData.buildBankStatementProvenance(base: FieldProvenance) = BankStatementFieldProvenance(
    direction = base,
    transactions = if (transactions.isNotEmpty()) base else null,
    accountIban = if (accountIban != null) base else null,
    openingBalance = if (openingBalance != null) base else null,
    closingBalance = if (closingBalance != null) base else null,
    periodStart = if (periodStart != null) base else null,
    periodEnd = if (periodEnd != null) base else null,
    notes = if (notes != null) base else null,
)

private fun PartyDraft.buildPartyProvenance(base: FieldProvenance) = PartyFieldProvenance(
    name = if (name != null) base else null,
    vat = if (vat != null) base else null,
    email = if (email != null) base else null,
    iban = if (iban != null) base else null,
    streetLine1 = if (streetLine1 != null) base else null,
    streetLine2 = if (streetLine2 != null) base else null,
    postalCode = if (postalCode != null) base else null,
    city = if (city != null) base else null,
    country = if (country != null) base else null,
)
