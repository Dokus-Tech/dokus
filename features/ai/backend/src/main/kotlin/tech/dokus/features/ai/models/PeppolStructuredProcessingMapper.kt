package tech.dokus.features.ai.models

import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.model.AnnualAccountsDraftData
import tech.dokus.domain.model.BankFeeDraftData
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BoardMinutesDraftData
import tech.dokus.domain.model.C4DraftData
import tech.dokus.domain.model.CompanyExtractDraftData
import tech.dokus.domain.model.ContractDraftData
import tech.dokus.domain.model.CorporateTaxAdvanceDraftData
import tech.dokus.domain.model.CorporateTaxDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.CustomsDeclarationDraftData
import tech.dokus.domain.model.DeliveryNoteDraftData
import tech.dokus.domain.model.DepreciationScheduleDraftData
import tech.dokus.domain.model.DimonaDraftData
import tech.dokus.domain.model.DividendDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.EmploymentContractDraftData
import tech.dokus.domain.model.ExpenseClaimDraftData
import tech.dokus.domain.model.FineDraftData
import tech.dokus.domain.model.HolidayPayDraftData
import tech.dokus.domain.model.IcListingDraftData
import tech.dokus.domain.model.InsuranceDraftData
import tech.dokus.domain.model.InterestStatementDraftData
import tech.dokus.domain.model.IntrastatDraftData
import tech.dokus.domain.model.InventoryDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.LeaseDraftData
import tech.dokus.domain.model.LoanDraftData
import tech.dokus.domain.model.OrderConfirmationDraftData
import tech.dokus.domain.model.OssReturnDraftData
import tech.dokus.domain.model.OtherDraftData
import tech.dokus.domain.model.PaymentConfirmationDraftData
import tech.dokus.domain.model.PayrollSummaryDraftData
import tech.dokus.domain.model.PermitDraftData
import tech.dokus.domain.model.PersonalTaxDraftData
import tech.dokus.domain.model.ProFormaDraftData
import tech.dokus.domain.model.PurchaseOrderDraftData
import tech.dokus.domain.model.QuoteDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.ReminderDraftData
import tech.dokus.domain.model.SalarySlipDraftData
import tech.dokus.domain.model.SelfEmployedContributionDraftData
import tech.dokus.domain.model.ShareholderRegisterDraftData
import tech.dokus.domain.model.SocialContributionDraftData
import tech.dokus.domain.model.SocialFundDraftData
import tech.dokus.domain.model.StatementOfAccountDraftData
import tech.dokus.domain.model.SubsidyDraftData
import tech.dokus.domain.model.TaxAssessmentDraftData
import tech.dokus.domain.model.TransactionCommunication
import tech.dokus.domain.model.VapzDraftData
import tech.dokus.domain.model.VatAssessmentDraftData
import tech.dokus.domain.model.VatListingDraftData
import tech.dokus.domain.model.VatReturnDraftData
import tech.dokus.domain.model.WithholdingTaxDraftData
import tech.dokus.domain.model.resolvedCounterpartyName
import tech.dokus.domain.model.resolvedCounterpartyVat
import tech.dokus.domain.model.toDirection
import tech.dokus.domain.model.toDocumentType
import tech.dokus.features.ai.graph.sub.ClassificationResult
import tech.dokus.features.ai.graph.sub.extraction.financial.BankStatementExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.BankStatementTransactionExtractionRow
import tech.dokus.features.ai.graph.sub.extraction.financial.CreditNoteExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.InvoiceExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.ReceiptExtractionResult
import tech.dokus.features.ai.validation.AuditReport

private const val PeppolSnapshotVersion = 1

fun DocumentDraftData.toPeppolProcessingResult(snapshotVersion: Int?): DocumentAiProcessingResult {
    require(snapshotVersion == null || snapshotVersion == PeppolSnapshotVersion) {
        "Unsupported PEPPOL structured snapshot version: $snapshotVersion"
    }

    val documentType = toDocumentType()
    val direction = toDirection()
    val directionSource = if (direction == DocumentDirection.Unknown) {
        DirectionResolutionSource.Unknown
    } else {
        DirectionResolutionSource.VatMatch
    }

    return DocumentAiProcessingResult(
        classification = ClassificationResult(
            documentType = documentType,
            confidence = 1.0,
            language = "structured",
            reasoning = "PEPPOL structured extraction snapshot"
        ),
        extraction = toPeppolExtractionResult(),
        directionResolution = DirectionResolution(
            direction = direction,
            source = directionSource,
            confidence = if (direction == DocumentDirection.Unknown) 0.0 else 1.0,
            reasoning = "Resolved from structured PEPPOL payload"
        ),
        auditReport = AuditReport.EMPTY
    )
}

private fun DocumentDraftData.toPeppolExtractionResult(): FinancialExtractionResult = when (this) {
    is InvoiceDraftData -> FinancialExtractionResult.Invoice(
        InvoiceExtractionResult(
            invoiceNumber = invoiceNumber,
            issueDate = issueDate,
            dueDate = dueDate,
            currency = currency,
            subtotalAmount = subtotalAmount,
            vatAmount = vatAmount,
            totalAmount = totalAmount,
            lineItems = lineItems,
            vatBreakdown = vatBreakdown,
            sellerName = seller.name,
            sellerVat = seller.vat,
            sellerEmail = seller.email,
            sellerStreet = seller.streetLine1,
            sellerPostalCode = seller.postalCode,
            sellerCity = seller.city,
            sellerCountry = seller.country,
            buyerName = buyer.name,
            buyerVat = buyer.vat,
            buyerEmail = buyer.email,
            buyerStreet = buyer.streetLine1,
            buyerPostalCode = buyer.postalCode,
            buyerCity = buyer.city,
            buyerCountry = buyer.country,
            counterparty = counterpartyExtractionForInvoice(this),
            iban = iban,
            payment = payment,
            directionHint = direction,
            directionHintConfidence = if (direction == DocumentDirection.Unknown) 0.0 else 1.0,
            confidence = 1.0,
            reasoning = "Structured PEPPOL invoice payload"
        )
    )

    is CreditNoteDraftData -> FinancialExtractionResult.CreditNote(
        CreditNoteExtractionResult(
            creditNoteNumber = creditNoteNumber,
            issueDate = issueDate,
            currency = currency,
            subtotalAmount = subtotalAmount,
            vatAmount = vatAmount,
            totalAmount = totalAmount,
            lineItems = lineItems,
            vatBreakdown = vatBreakdown,
            sellerName = seller.name,
            sellerVat = seller.vat,
            buyerName = buyer.name,
            buyerVat = buyer.vat,
            counterparty = counterpartyExtractionForCreditNote(this),
            directionHint = direction,
            directionHintConfidence = if (direction == DocumentDirection.Unknown) 0.0 else 1.0,
            originalInvoiceNumber = originalInvoiceNumber,
            reason = reason,
            confidence = 1.0,
            reasoning = "Structured PEPPOL credit note payload"
        )
    )

    is ReceiptDraftData -> FinancialExtractionResult.Receipt(
        ReceiptExtractionResult(
            merchantName = merchantName,
            merchantVat = merchantVat,
            date = date,
            currency = currency,
            totalAmount = totalAmount,
            vatAmount = vatAmount,
            lineItems = lineItems,
            vatBreakdown = vatBreakdown,
            receiptNumber = receiptNumber,
            paymentMethod = paymentMethod,
            counterparty = CounterpartyExtraction(
                name = merchantName,
                vatNumber = merchantVat?.value,
                role = CounterpartyRole.Merchant,
                reasoning = "Merchant extracted from structured PEPPOL payload"
            ),
            directionHint = direction,
            directionHintConfidence = if (direction == DocumentDirection.Unknown) 0.0 else 1.0,
            confidence = 1.0,
            reasoning = "Structured PEPPOL receipt payload"
        )
    )

    // Bank statements are never received via Peppol; this branch is a defensive catch-all
    is BankStatementDraftData -> FinancialExtractionResult.BankStatement(
        BankStatementExtractionResult(
            rows = transactions.map { row ->
                BankStatementTransactionExtractionRow(
                    transactionDate = row.transactionDate,
                    signedAmount = row.signedAmount,
                    counterpartyName = row.counterparty.name,
                    counterpartyIban = row.counterparty.iban,
                    structuredCommunicationRaw = (row.communication as? TransactionCommunication.Structured)?.raw,
                    freeCommunication = (row.communication as? TransactionCommunication.FreeForm)?.text,
                    descriptionRaw = row.descriptionRaw,
                    rowConfidence = row.rowConfidence.coerceIn(0.0, 1.0),
                )
            },
            confidence = 1.0,
            reasoning = "Structured PEPPOL bank statement payload"
        )
    )

    // Classified-only types are never produced by PEPPOL structured extraction
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
    is OtherDraftData ->
        error("${this::class.simpleName} is not supported in PEPPOL structured extraction")
}

private fun counterpartyExtractionForInvoice(data: InvoiceDraftData): CounterpartyExtraction {
    val counterparty = when (data.direction) {
        DocumentDirection.Inbound -> data.seller to CounterpartyRole.Seller
        DocumentDirection.Outbound -> data.buyer to CounterpartyRole.Buyer
        DocumentDirection.Neutral -> data.seller to CounterpartyRole.Unknown
        DocumentDirection.Unknown -> {
            val fallback = if (!data.seller.name.isNullOrBlank()) data.seller else data.buyer
            fallback to CounterpartyRole.Unknown
        }
    }
    return CounterpartyExtraction(
        name = counterparty.first.name,
        vatNumber = counterparty.first.vat?.value,
        email = counterparty.first.email?.value,
        streetLine1 = counterparty.first.streetLine1,
        postalCode = counterparty.first.postalCode,
        city = counterparty.first.city,
        country = counterparty.first.country,
        role = counterparty.second,
        reasoning = "Counterparty derived from structured PEPPOL parties"
    )
}

private fun counterpartyExtractionForCreditNote(data: CreditNoteDraftData): CounterpartyExtraction {
    val role = when (data.direction) {
        DocumentDirection.Inbound -> CounterpartyRole.Seller
        DocumentDirection.Outbound -> CounterpartyRole.Buyer
        DocumentDirection.Neutral -> CounterpartyRole.Unknown
        DocumentDirection.Unknown -> CounterpartyRole.Unknown
    }
    val name = data.resolvedCounterpartyName
    val vat = data.resolvedCounterpartyVat?.value
    return CounterpartyExtraction(
        name = name,
        vatNumber = vat,
        role = role,
        reasoning = "Counterparty derived from structured PEPPOL credit note payload"
    )
}

