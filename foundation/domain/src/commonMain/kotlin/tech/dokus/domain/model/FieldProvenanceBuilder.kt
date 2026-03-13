package tech.dokus.domain.model

import tech.dokus.domain.enums.SourceTrust
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.IngestionRunId

/**
 * Builds a per-field provenance map for a [DocumentDraftData].
 *
 * Only populated (non-null, non-default) fields get provenance entries.
 * All fields from the same extraction share the same source trust and confidence.
 *
 * Line items and VAT breakdown are treated as single units ("lineItems", "vatBreakdown")
 * — no per-row provenance in v1.
 */
fun DocumentDraftData.buildProvenance(
    sourceTrust: SourceTrust,
    sourceId: DocumentSourceId? = null,
    sourceRunId: IngestionRunId? = null,
    extractionConfidence: Double? = null,
): Map<String, FieldProvenance> {
    val fields = populatedFieldPaths()
    if (fields.isEmpty()) return emptyMap()

    val provenance = FieldProvenance(
        sourceId = sourceId,
        sourceRunId = sourceRunId,
        sourceTrust = sourceTrust,
        extractionConfidence = extractionConfidence,
    )
    return fields.associateWith { provenance }
}

/**
 * Returns the set of field paths that have non-null/non-default values.
 */
private fun DocumentDraftData.populatedFieldPaths(): Set<String> = when (this) {
    is InvoiceDraftData -> populatedFields()
    is CreditNoteDraftData -> populatedFields()
    is ReceiptDraftData -> populatedFields()
    is BankStatementDraftData -> populatedFields()
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
    is OtherDraftData -> setOf("direction")
}

private fun InvoiceDraftData.populatedFields(): Set<String> = buildSet {
    add("direction")
    if (invoiceNumber != null) add("invoiceNumber")
    if (issueDate != null) add("issueDate")
    if (dueDate != null) add("dueDate")
    add("currency")
    if (subtotalAmount != null) add("subtotalAmount")
    if (vatAmount != null) add("vatAmount")
    if (totalAmount != null) add("totalAmount")
    if (lineItems.isNotEmpty()) add("lineItems")
    if (vatBreakdown.isNotEmpty()) add("vatBreakdown")
    if (iban != null) add("iban")
    if (payment != null) add("payment")
    if (notes != null) add("notes")
    addPartyFields("seller", seller)
    addPartyFields("buyer", buyer)
}

private fun CreditNoteDraftData.populatedFields(): Set<String> = buildSet {
    add("direction")
    if (creditNoteNumber != null) add("creditNoteNumber")
    if (issueDate != null) add("issueDate")
    add("currency")
    if (subtotalAmount != null) add("subtotalAmount")
    if (vatAmount != null) add("vatAmount")
    if (totalAmount != null) add("totalAmount")
    if (lineItems.isNotEmpty()) add("lineItems")
    if (vatBreakdown.isNotEmpty()) add("vatBreakdown")
    if (originalInvoiceNumber != null) add("originalInvoiceNumber")
    if (reason != null) add("reason")
    if (notes != null) add("notes")
    addPartyFields("seller", seller)
    addPartyFields("buyer", buyer)
}

private fun ReceiptDraftData.populatedFields(): Set<String> = buildSet {
    add("direction")
    if (merchantName != null) add("merchantName")
    if (merchantVat != null) add("merchantVat")
    if (date != null) add("date")
    add("currency")
    if (totalAmount != null) add("totalAmount")
    if (vatAmount != null) add("vatAmount")
    if (lineItems.isNotEmpty()) add("lineItems")
    if (vatBreakdown.isNotEmpty()) add("vatBreakdown")
    if (receiptNumber != null) add("receiptNumber")
    if (paymentMethod != null) add("paymentMethod")
    if (notes != null) add("notes")
}

private fun BankStatementDraftData.populatedFields(): Set<String> = buildSet {
    add("direction")
    if (transactions.isNotEmpty()) add("transactions")
    if (accountIban != null) add("accountIban")
    if (openingBalance != null) add("openingBalance")
    if (closingBalance != null) add("closingBalance")
    if (periodStart != null) add("periodStart")
    if (periodEnd != null) add("periodEnd")
    if (notes != null) add("notes")
}

private fun MutableSet<String>.addPartyFields(prefix: String, party: PartyDraft) {
    if (party.name != null) add("$prefix.name")
    if (party.vat != null) add("$prefix.vat")
    if (party.email != null) add("$prefix.email")
    if (party.iban != null) add("$prefix.iban")
    if (party.streetLine1 != null) add("$prefix.streetLine1")
    if (party.streetLine2 != null) add("$prefix.streetLine2")
    if (party.postalCode != null) add("$prefix.postalCode")
    if (party.city != null) add("$prefix.city")
    if (party.country != null) add("$prefix.country")
}
