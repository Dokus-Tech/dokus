package tech.dokus.domain.model

import tech.dokus.domain.Money
import tech.dokus.domain.Quantity
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType

// =============================================================================
// FinancialLineItem → DocLineItem
// =============================================================================

fun FinancialLineItem.toDocLineItem(): DocLineItem = DocLineItem(
    description = description,
    quantity = quantity?.let { Quantity(it.toDouble()) },
    unitPrice = unitPrice?.let { Money(it) },
    vatRate = vatRate?.let { tech.dokus.domain.VatRate(it) },
    netAmount = netAmount?.let { Money(it) },
)

fun InvoiceItemDto.toDocLineItem(): DocLineItem = DocLineItem(
    description = description,
    quantity = Quantity(quantity),
    unitPrice = unitPrice,
    vatRate = vatRate,
    netAmount = lineTotal,
    vatAmount = vatAmount,
    sortOrder = sortOrder,
)

// =============================================================================
// Counterparty resolution
// =============================================================================

/**
 * Resolve the counterparty from seller/buyer based on direction.
 * Outbound (you're selling) → counterparty is buyer.
 * Inbound (you're buying) → counterparty is seller.
 * Unknown → use whichever has a name, prefer seller.
 */
private fun resolveCounterparty(
    direction: DocumentDirection,
    seller: PartyDraft,
    buyer: PartyDraft,
): PartyDraft = when (direction) {
    DocumentDirection.Inbound -> seller
    DocumentDirection.Outbound -> buyer
    DocumentDirection.Unknown, DocumentDirection.Neutral -> {
        if (seller.name != null) seller else buyer
    }
}

/**
 * Reverse: split counterparty back into seller/buyer for DB storage.
 * Returns (seller, buyer).
 */
private fun splitCounterparty(
    direction: DocumentDirection,
    counterparty: PartyDraft,
): kotlin.Pair<PartyDraft, PartyDraft> = when (direction) {
    DocumentDirection.Inbound -> counterparty to PartyDraft()
    DocumentDirection.Outbound -> PartyDraft() to counterparty
    DocumentDirection.Unknown, DocumentDirection.Neutral -> counterparty to PartyDraft()
}

// =============================================================================
// DocumentDraftData → DocDto (Draft variants)
// =============================================================================

fun DocumentDraftData.toDocDto(): DocDto = when (this) {
    is InvoiceDraftData -> DocDto.Invoice.Draft(
        direction = direction,
        invoiceNumber = invoiceNumber,
        issueDate = issueDate,
        dueDate = dueDate,
        currency = currency,
        subtotalAmount = subtotalAmount,
        vatAmount = vatAmount,
        totalAmount = totalAmount,
        lineItems = lineItems.map { it.toDocLineItem() },
        iban = iban,
        notes = notes,
        vatBreakdown = vatBreakdown,
        payment = payment,
        counterparty = resolveCounterparty(direction, seller, buyer),
    )

    is CreditNoteDraftData -> DocDto.CreditNote.Draft(
        direction = direction,
        creditNoteNumber = creditNoteNumber,
        issueDate = issueDate,
        currency = currency,
        subtotalAmount = subtotalAmount,
        vatAmount = vatAmount,
        totalAmount = totalAmount,
        lineItems = lineItems.map { it.toDocLineItem() },
        reason = reason,
        notes = notes,
        vatBreakdown = vatBreakdown,
        originalInvoiceNumber = originalInvoiceNumber,
        counterparty = resolveCounterparty(direction, seller, buyer),
    )

    is ReceiptDraftData -> DocDto.Receipt.Draft(
        direction = direction,
        merchantName = merchantName,
        merchantVat = merchantVat,
        date = date,
        currency = currency,
        totalAmount = totalAmount,
        vatAmount = vatAmount,
        lineItems = lineItems.map { it.toDocLineItem() },
        receiptNumber = receiptNumber,
        notes = notes,
        vatBreakdown = vatBreakdown,
        paymentMethod = paymentMethod,
    )

    is BankStatementDraftData -> DocDto.BankStatement.Draft(
        direction = direction,
        accountIban = accountIban,
        openingBalance = openingBalance,
        closingBalance = closingBalance,
        periodStart = periodStart,
        periodEnd = periodEnd,
        notes = notes,
        transactions = transactions,
        institution = institution,
    )

    // Classified-only types
    is ProFormaDraftData -> DocDto.ProForma.Draft(direction)
    is QuoteDraftData -> DocDto.Quote.Draft(direction)
    is OrderConfirmationDraftData -> DocDto.OrderConfirmation.Draft(direction)
    is DeliveryNoteDraftData -> DocDto.DeliveryNote.Draft(direction)
    is ReminderDraftData -> DocDto.Reminder.Draft(direction)
    is StatementOfAccountDraftData -> DocDto.StatementOfAccount.Draft(direction)
    is PurchaseOrderDraftData -> DocDto.PurchaseOrder.Draft(direction)
    is ExpenseClaimDraftData -> DocDto.ExpenseClaim.Draft(direction)
    is BankFeeDraftData -> DocDto.BankFee.Draft(direction)
    is InterestStatementDraftData -> DocDto.InterestStatement.Draft(direction)
    is PaymentConfirmationDraftData -> DocDto.PaymentConfirmation.Draft(direction)
    is VatReturnDraftData -> DocDto.VatReturn.Draft(direction)
    is VatListingDraftData -> DocDto.VatListing.Draft(direction)
    is VatAssessmentDraftData -> DocDto.VatAssessment.Draft(direction)
    is IcListingDraftData -> DocDto.IcListing.Draft(direction)
    is OssReturnDraftData -> DocDto.OssReturn.Draft(direction)
    is CorporateTaxDraftData -> DocDto.CorporateTax.Draft(direction)
    is CorporateTaxAdvanceDraftData -> DocDto.CorporateTaxAdvance.Draft(direction)
    is TaxAssessmentDraftData -> DocDto.TaxAssessment.Draft(direction)
    is PersonalTaxDraftData -> DocDto.PersonalTax.Draft(direction)
    is WithholdingTaxDraftData -> DocDto.WithholdingTax.Draft(direction)
    is SocialContributionDraftData -> DocDto.SocialContribution.Draft(direction)
    is SocialFundDraftData -> DocDto.SocialFund.Draft(direction)
    is SelfEmployedContributionDraftData -> DocDto.SelfEmployedContribution.Draft(direction)
    is VapzDraftData -> DocDto.Vapz.Draft(direction)
    is SalarySlipDraftData -> DocDto.SalarySlip.Draft(direction)
    is PayrollSummaryDraftData -> DocDto.PayrollSummary.Draft(direction)
    is EmploymentContractDraftData -> DocDto.EmploymentContract.Draft(direction)
    is DimonaDraftData -> DocDto.Dimona.Draft(direction)
    is C4DraftData -> DocDto.C4.Draft(direction)
    is HolidayPayDraftData -> DocDto.HolidayPay.Draft(direction)
    is ContractDraftData -> DocDto.Contract.Draft(direction)
    is LeaseDraftData -> DocDto.Lease.Draft(direction)
    is LoanDraftData -> DocDto.Loan.Draft(direction)
    is InsuranceDraftData -> DocDto.Insurance.Draft(direction)
    is DividendDraftData -> DocDto.Dividend.Draft(direction)
    is ShareholderRegisterDraftData -> DocDto.ShareholderRegister.Draft(direction)
    is CompanyExtractDraftData -> DocDto.CompanyExtract.Draft(direction)
    is AnnualAccountsDraftData -> DocDto.AnnualAccounts.Draft(direction)
    is BoardMinutesDraftData -> DocDto.BoardMinutes.Draft(direction)
    is SubsidyDraftData -> DocDto.Subsidy.Draft(direction)
    is FineDraftData -> DocDto.Fine.Draft(direction)
    is PermitDraftData -> DocDto.Permit.Draft(direction)
    is CustomsDeclarationDraftData -> DocDto.CustomsDeclaration.Draft(direction)
    is IntrastatDraftData -> DocDto.Intrastat.Draft(direction)
    is DepreciationScheduleDraftData -> DocDto.DepreciationSchedule.Draft(direction)
    is InventoryDraftData -> DocDto.Inventory.Draft(direction)
    is OtherDraftData -> DocDto.Other.Draft(direction)
}

// =============================================================================
// FinancialDocumentDto → DocDto (Confirmed variants)
// =============================================================================

fun FinancialDocumentDto.toDocDto(): DocDto = when (this) {
    is FinancialDocumentDto.InvoiceDto -> DocDto.Invoice.Confirmed(
        id = id,
        tenantId = tenantId,
        contactId = contactId,
        direction = direction,
        invoiceNumber = invoiceNumber.value,
        issueDate = issueDate,
        dueDate = dueDate,
        currency = currency,
        subtotalAmount = subtotalAmount,
        vatAmount = vatAmount,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        lineItems = items.map { it.toDocLineItem() },
        iban = senderIban,
        notes = notes,
        status = status,
        structuredCommunication = structuredCommunication,
        peppol = peppol,
        paymentLinkInfo = paymentLinkInfo,
        paymentInfo = paymentInfo,
        documentId = documentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    is FinancialDocumentDto.ExpenseDto -> DocDto.Receipt.Confirmed(
        id = id,
        tenantId = tenantId,
        direction = DocumentDirection.Inbound,
        merchantName = merchant,
        merchantVat = null,
        date = date,
        currency = currency,
        totalAmount = amount,
        vatAmount = vatAmount,
        lineItems = emptyList(),
        receiptNumber = null,
        notes = notes,
        vatRate = vatRate,
        category = category,
        isDeductible = isDeductible,
        deductiblePercentage = deductiblePercentage,
        paymentMethod = paymentMethod,
        contactId = contactId,
        documentId = documentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    is FinancialDocumentDto.CreditNoteDto -> DocDto.CreditNote.Confirmed(
        id = id,
        tenantId = tenantId,
        contactId = contactId,
        creditNoteType = creditNoteType,
        direction = when (creditNoteType) {
            tech.dokus.domain.enums.CreditNoteType.Sales -> DocumentDirection.Outbound
            tech.dokus.domain.enums.CreditNoteType.Purchase -> DocumentDirection.Inbound
        },
        creditNoteNumber = creditNoteNumber,
        issueDate = issueDate,
        currency = currency,
        subtotalAmount = subtotalAmount,
        vatAmount = vatAmount,
        totalAmount = totalAmount,
        lineItems = emptyList(),
        status = status,
        settlementIntent = settlementIntent,
        reason = reason,
        notes = notes,
        documentId = documentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    is FinancialDocumentDto.QuoteDto,
    is FinancialDocumentDto.ProFormaDto,
    is FinancialDocumentDto.PurchaseOrderDto,
    -> error("Unsupported FinancialDocumentDto type for DocDto conversion: ${this::class.simpleName}")
}

// =============================================================================
// DocDto extension properties
// =============================================================================

fun DocDto.toDocumentType(): DocumentType = when (this) {
    is DocDto.Invoice -> DocumentType.Invoice
    is DocDto.CreditNote -> DocumentType.CreditNote
    is DocDto.Receipt -> DocumentType.Receipt
    is DocDto.BankStatement -> DocumentType.BankStatement
    is DocDto.ProForma -> DocumentType.ProForma
    is DocDto.Quote -> DocumentType.Quote
    is DocDto.OrderConfirmation -> DocumentType.OrderConfirmation
    is DocDto.DeliveryNote -> DocumentType.DeliveryNote
    is DocDto.Reminder -> DocumentType.Reminder
    is DocDto.StatementOfAccount -> DocumentType.StatementOfAccount
    is DocDto.PurchaseOrder -> DocumentType.PurchaseOrder
    is DocDto.ExpenseClaim -> DocumentType.ExpenseClaim
    is DocDto.BankFee -> DocumentType.BankFee
    is DocDto.InterestStatement -> DocumentType.InterestStatement
    is DocDto.PaymentConfirmation -> DocumentType.PaymentConfirmation
    is DocDto.VatReturn -> DocumentType.VatReturn
    is DocDto.VatListing -> DocumentType.VatListing
    is DocDto.VatAssessment -> DocumentType.VatAssessment
    is DocDto.IcListing -> DocumentType.IcListing
    is DocDto.OssReturn -> DocumentType.OssReturn
    is DocDto.CorporateTax -> DocumentType.CorporateTax
    is DocDto.CorporateTaxAdvance -> DocumentType.CorporateTaxAdvance
    is DocDto.TaxAssessment -> DocumentType.TaxAssessment
    is DocDto.PersonalTax -> DocumentType.PersonalTax
    is DocDto.WithholdingTax -> DocumentType.WithholdingTax
    is DocDto.SocialContribution -> DocumentType.SocialContribution
    is DocDto.SocialFund -> DocumentType.SocialFund
    is DocDto.SelfEmployedContribution -> DocumentType.SelfEmployedContribution
    is DocDto.Vapz -> DocumentType.Vapz
    is DocDto.SalarySlip -> DocumentType.SalarySlip
    is DocDto.PayrollSummary -> DocumentType.PayrollSummary
    is DocDto.EmploymentContract -> DocumentType.EmploymentContract
    is DocDto.Dimona -> DocumentType.Dimona
    is DocDto.C4 -> DocumentType.C4
    is DocDto.HolidayPay -> DocumentType.HolidayPay
    is DocDto.Contract -> DocumentType.Contract
    is DocDto.Lease -> DocumentType.Lease
    is DocDto.Loan -> DocumentType.Loan
    is DocDto.Insurance -> DocumentType.Insurance
    is DocDto.Dividend -> DocumentType.Dividend
    is DocDto.ShareholderRegister -> DocumentType.ShareholderRegister
    is DocDto.CompanyExtract -> DocumentType.CompanyExtract
    is DocDto.AnnualAccounts -> DocumentType.AnnualAccounts
    is DocDto.BoardMinutes -> DocumentType.BoardMinutes
    is DocDto.Subsidy -> DocumentType.Subsidy
    is DocDto.Fine -> DocumentType.Fine
    is DocDto.Permit -> DocumentType.Permit
    is DocDto.CustomsDeclaration -> DocumentType.CustomsDeclaration
    is DocDto.Intrastat -> DocumentType.Intrastat
    is DocDto.DepreciationSchedule -> DocumentType.DepreciationSchedule
    is DocDto.Inventory -> DocumentType.Inventory
    is DocDto.Other -> DocumentType.Other
}

val DocDto?.isContactRequired: Boolean
    get() = this is DocDto.Invoice || this is DocDto.CreditNote

val DocDto.isDraft: Boolean
    get() = when (this) {
        is DocDto.Invoice.Draft -> true
        is DocDto.Invoice.Confirmed -> false
        is DocDto.CreditNote.Draft -> true
        is DocDto.CreditNote.Confirmed -> false
        is DocDto.Receipt.Draft -> true
        is DocDto.Receipt.Confirmed -> false
        is DocDto.BankStatement.Draft -> true
        is DocDto.BankStatement.Confirmed -> false
        is DocDto.ClassifiedDoc -> this::class.simpleName == "Draft"
    }

val DocDto.totalAmount: Money?
    get() = when (this) {
        is DocDto.Invoice -> totalAmount
        is DocDto.CreditNote -> totalAmount
        is DocDto.Receipt -> totalAmount
        is DocDto.BankStatement,
        is DocDto.ClassifiedDoc -> null
    }

val DocDto.currency: Currency
    get() = when (this) {
        is DocDto.Invoice -> currency
        is DocDto.CreditNote -> currency
        is DocDto.Receipt -> currency
        is DocDto.BankStatement,
        is DocDto.ClassifiedDoc -> Currency.default
    }

val DocDto.sortDate: kotlinx.datetime.LocalDate?
    get() = when (this) {
        is DocDto.Invoice -> issueDate
        is DocDto.CreditNote -> issueDate
        is DocDto.Receipt -> date
        is DocDto.BankStatement -> periodEnd
        is DocDto.ClassifiedDoc -> null
    }

val DocDto.hasRequiredDates: Boolean
    get() = when (this) {
        is DocDto.Invoice -> issueDate != null
        is DocDto.CreditNote -> issueDate != null
        is DocDto.Receipt -> date != null
        is DocDto.BankStatement,
        is DocDto.ClassifiedDoc -> true
    }

val DocDto.hasKnownDirectionForConfirmation: Boolean
    get() = when (this) {
        is DocDto.Invoice -> direction != DocumentDirection.Unknown
        is DocDto.CreditNote -> direction != DocumentDirection.Unknown
        is DocDto.Receipt -> true
        is DocDto.BankStatement -> direction == DocumentDirection.Neutral
        is DocDto.ClassifiedDoc -> true
    }

val DocDto.hasRequiredIdentityForConfirmation: Boolean
    get() = when (this) {
        is DocDto.Invoice -> true
        is DocDto.CreditNote -> !creditNoteNumber.isNullOrBlank()
        is DocDto.Receipt -> !merchantName.isNullOrBlank()
        is DocDto.BankStatement,
        is DocDto.ClassifiedDoc -> true
    }

val DocDto.hasRequiredSubtotalForConfirmation: Boolean
    get() = when (this) {
        is DocDto.CreditNote -> subtotalAmount != null
        is DocDto.Invoice,
        is DocDto.Receipt,
        is DocDto.BankStatement,
        is DocDto.ClassifiedDoc -> true
    }

val DocDto.hasRequiredTotalForConfirmation: Boolean
    get() = when (this) {
        is DocDto.Invoice -> totalAmount != null
        is DocDto.CreditNote -> totalAmount != null
        is DocDto.Receipt -> totalAmount != null
        is DocDto.BankStatement,
        is DocDto.ClassifiedDoc -> true
    }

val DocDto.hasRequiredVatForConfirmation: Boolean
    get() = when (this) {
        is DocDto.Invoice -> vatAmount != null
        is DocDto.CreditNote -> vatAmount != null
        is DocDto.Receipt,
        is DocDto.BankStatement,
        is DocDto.ClassifiedDoc -> true
    }

val DocDto?.displayContextDescription: String?
    get() = when (this) {
        is DocDto.Invoice -> notes?.takeIf { it.isNotBlank() }
        is DocDto.Receipt -> notes?.takeIf { it.isNotBlank() }
        is DocDto.CreditNote -> reason?.takeIf { it.isNotBlank() }
        is DocDto.BankStatement -> notes?.takeIf { it.isNotBlank() }
        is DocDto.ClassifiedDoc,
        null -> null
    }

// =============================================================================
// DocLineItem → FinancialLineItem (reverse)
// =============================================================================

fun DocLineItem.toFinancialLineItem(): FinancialLineItem = FinancialLineItem(
    description = description,
    quantity = quantity?.value?.toLong(),
    unitPrice = unitPrice?.minor,
    vatRate = vatRate?.basisPoints,
    netAmount = netAmount?.minor,
)

// =============================================================================
// DocDto → DocumentDraftData (Draft variants only)
// =============================================================================

@Suppress("CyclomaticComplexMethod")
fun DocDto.toDraftData(): DocumentDraftData = when (this) {
    is DocDto.Invoice.Draft -> {
        val (seller, buyer) = splitCounterparty(direction, counterparty)
        InvoiceDraftData(
            direction = direction,
            invoiceNumber = invoiceNumber,
            issueDate = issueDate,
            dueDate = dueDate,
            currency = currency,
            subtotalAmount = subtotalAmount,
            vatAmount = vatAmount,
            totalAmount = totalAmount,
            lineItems = lineItems.map { it.toFinancialLineItem() },
            iban = iban,
            notes = notes,
            vatBreakdown = vatBreakdown,
            payment = payment,
            seller = seller,
            buyer = buyer,
        )
    }
    is DocDto.Invoice.Confirmed -> InvoiceDraftData(
        direction = direction,
        invoiceNumber = invoiceNumber,
        issueDate = issueDate,
        dueDate = dueDate,
        currency = currency,
        subtotalAmount = subtotalAmount,
        vatAmount = vatAmount,
        totalAmount = totalAmount,
        lineItems = lineItems.map { it.toFinancialLineItem() },
        iban = iban,
        notes = notes,
    )
    is DocDto.CreditNote.Draft -> {
        val (seller, buyer) = splitCounterparty(direction, counterparty)
        CreditNoteDraftData(
            direction = direction,
            creditNoteNumber = creditNoteNumber,
            issueDate = issueDate,
            currency = currency,
            subtotalAmount = subtotalAmount,
            vatAmount = vatAmount,
            totalAmount = totalAmount,
            lineItems = lineItems.map { it.toFinancialLineItem() },
            reason = reason,
            notes = notes,
            vatBreakdown = vatBreakdown,
            originalInvoiceNumber = originalInvoiceNumber,
            seller = seller,
            buyer = buyer,
        )
    }
    is DocDto.CreditNote.Confirmed -> CreditNoteDraftData(
        direction = direction,
        creditNoteNumber = creditNoteNumber,
        issueDate = issueDate,
        currency = currency,
        subtotalAmount = subtotalAmount,
        vatAmount = vatAmount,
        totalAmount = totalAmount,
        lineItems = lineItems.map { it.toFinancialLineItem() },
        reason = reason,
        notes = notes,
    )
    is DocDto.Receipt.Draft -> ReceiptDraftData(
        direction = direction,
        merchantName = merchantName,
        merchantVat = merchantVat,
        date = date,
        currency = currency,
        totalAmount = totalAmount,
        vatAmount = vatAmount,
        lineItems = lineItems.map { it.toFinancialLineItem() },
        receiptNumber = receiptNumber,
        notes = notes,
        vatBreakdown = vatBreakdown,
        paymentMethod = paymentMethod,
    )
    is DocDto.Receipt.Confirmed -> ReceiptDraftData(
        direction = direction,
        merchantName = merchantName,
        merchantVat = merchantVat,
        date = date,
        currency = currency,
        totalAmount = totalAmount,
        vatAmount = vatAmount,
        lineItems = lineItems.map { it.toFinancialLineItem() },
        receiptNumber = receiptNumber,
        notes = notes,
    )
    is DocDto.BankStatement.Draft -> BankStatementDraftData(
        direction = direction,
        accountIban = accountIban,
        openingBalance = openingBalance,
        closingBalance = closingBalance,
        periodStart = periodStart,
        periodEnd = periodEnd,
        notes = notes,
        transactions = transactions,
        institution = institution,
    )
    is DocDto.BankStatement.Confirmed -> BankStatementDraftData(
        direction = direction,
        accountIban = accountIban,
        openingBalance = openingBalance,
        closingBalance = closingBalance,
        periodStart = periodStart,
        periodEnd = periodEnd,
        notes = notes,
    )
    is DocDto.ProForma -> ProFormaDraftData(direction)
    is DocDto.Quote -> QuoteDraftData(direction)
    is DocDto.OrderConfirmation -> OrderConfirmationDraftData(direction)
    is DocDto.DeliveryNote -> DeliveryNoteDraftData(direction)
    is DocDto.Reminder -> ReminderDraftData(direction)
    is DocDto.StatementOfAccount -> StatementOfAccountDraftData(direction)
    is DocDto.PurchaseOrder -> PurchaseOrderDraftData(direction)
    is DocDto.ExpenseClaim -> ExpenseClaimDraftData(direction)
    is DocDto.BankFee -> BankFeeDraftData(direction)
    is DocDto.InterestStatement -> InterestStatementDraftData(direction)
    is DocDto.PaymentConfirmation -> PaymentConfirmationDraftData(direction)
    is DocDto.VatReturn -> VatReturnDraftData(direction)
    is DocDto.VatListing -> VatListingDraftData(direction)
    is DocDto.VatAssessment -> VatAssessmentDraftData(direction)
    is DocDto.IcListing -> IcListingDraftData(direction)
    is DocDto.OssReturn -> OssReturnDraftData(direction)
    is DocDto.CorporateTax -> CorporateTaxDraftData(direction)
    is DocDto.CorporateTaxAdvance -> CorporateTaxAdvanceDraftData(direction)
    is DocDto.TaxAssessment -> TaxAssessmentDraftData(direction)
    is DocDto.PersonalTax -> PersonalTaxDraftData(direction)
    is DocDto.WithholdingTax -> WithholdingTaxDraftData(direction)
    is DocDto.SocialContribution -> SocialContributionDraftData(direction)
    is DocDto.SocialFund -> SocialFundDraftData(direction)
    is DocDto.SelfEmployedContribution -> SelfEmployedContributionDraftData(direction)
    is DocDto.Vapz -> VapzDraftData(direction)
    is DocDto.SalarySlip -> SalarySlipDraftData(direction)
    is DocDto.PayrollSummary -> PayrollSummaryDraftData(direction)
    is DocDto.EmploymentContract -> EmploymentContractDraftData(direction)
    is DocDto.Dimona -> DimonaDraftData(direction)
    is DocDto.C4 -> C4DraftData(direction)
    is DocDto.HolidayPay -> HolidayPayDraftData(direction)
    is DocDto.Contract -> ContractDraftData(direction)
    is DocDto.Lease -> LeaseDraftData(direction)
    is DocDto.Loan -> LoanDraftData(direction)
    is DocDto.Insurance -> InsuranceDraftData(direction)
    is DocDto.Dividend -> DividendDraftData(direction)
    is DocDto.ShareholderRegister -> ShareholderRegisterDraftData(direction)
    is DocDto.CompanyExtract -> CompanyExtractDraftData(direction)
    is DocDto.AnnualAccounts -> AnnualAccountsDraftData(direction)
    is DocDto.BoardMinutes -> BoardMinutesDraftData(direction)
    is DocDto.Subsidy -> SubsidyDraftData(direction)
    is DocDto.Fine -> FineDraftData(direction)
    is DocDto.Permit -> PermitDraftData(direction)
    is DocDto.CustomsDeclaration -> CustomsDeclarationDraftData(direction)
    is DocDto.Intrastat -> IntrastatDraftData(direction)
    is DocDto.DepreciationSchedule -> DepreciationScheduleDraftData(direction)
    is DocDto.Inventory -> InventoryDraftData(direction)
    is DocDto.Other -> OtherDraftData(direction)
}
