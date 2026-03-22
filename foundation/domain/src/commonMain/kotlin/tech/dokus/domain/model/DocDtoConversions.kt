package tech.dokus.domain.model

import tech.dokus.domain.Money
import tech.dokus.domain.Quantity
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType

// =============================================================================
// FinancialLineItemDto → DocLineItem
// =============================================================================

fun DocLineItem.Companion.from(dto: FinancialLineItemDto): DocLineItem = DocLineItem(
    description = dto.description,
    quantity = dto.quantity?.let { Quantity(it.toDouble()) },
    unitPrice = dto.unitPrice?.let { Money(it) },
    vatRate = dto.vatRate?.let { tech.dokus.domain.VatRate(it) },
    netAmount = dto.netAmount?.let { Money(it) },
)

fun DocLineItem.Companion.from(dto: InvoiceItemDto): DocLineItem = DocLineItem(
    description = dto.description,
    quantity = Quantity(dto.quantity),
    unitPrice = dto.unitPrice,
    vatRate = dto.vatRate,
    netAmount = dto.lineTotal,
    vatAmount = dto.vatAmount,
    sortOrder = dto.sortOrder,
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
    seller: PartyDraftDto,
    buyer: PartyDraftDto,
): PartyDraftDto = when (direction) {
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
    counterparty: PartyDraftDto,
): kotlin.Pair<PartyDraftDto, PartyDraftDto> = when (direction) {
    DocumentDirection.Inbound -> counterparty to PartyDraftDto()
    DocumentDirection.Outbound -> PartyDraftDto() to counterparty
    DocumentDirection.Unknown, DocumentDirection.Neutral -> counterparty to PartyDraftDto()
}

// =============================================================================
// DocumentDraftData → DocDto (Draft variants)
// =============================================================================

fun DocDto.Companion.from(data: DocumentDraftData): DocDto = when (data) {
    is InvoiceDraftData -> DocDto.Invoice.Draft(
        direction = data.direction,
        invoiceNumber = data.invoiceNumber,
        issueDate = data.issueDate,
        dueDate = data.dueDate,
        currency = data.currency,
        subtotalAmount = data.subtotalAmount,
        vatAmount = data.vatAmount,
        totalAmount = data.totalAmount,
        lineItems = data.lineItems.map { DocLineItem.from(it) },
        iban = data.iban,
        notes = data.notes,
        vatBreakdown = data.vatBreakdown,
        payment = data.payment,
        counterparty = resolveCounterparty(data.direction, data.seller, data.buyer),
    )

    is CreditNoteDraftData -> DocDto.CreditNote.Draft(
        direction = data.direction,
        creditNoteNumber = data.creditNoteNumber,
        issueDate = data.issueDate,
        currency = data.currency,
        subtotalAmount = data.subtotalAmount,
        vatAmount = data.vatAmount,
        totalAmount = data.totalAmount,
        lineItems = data.lineItems.map { DocLineItem.from(it) },
        reason = data.reason,
        notes = data.notes,
        vatBreakdown = data.vatBreakdown,
        originalInvoiceNumber = data.originalInvoiceNumber,
        counterparty = resolveCounterparty(data.direction, data.seller, data.buyer),
    )

    is ReceiptDraftData -> DocDto.Receipt.Draft(
        direction = data.direction,
        merchantName = data.merchantName,
        merchantVat = data.merchantVat,
        date = data.date,
        currency = data.currency,
        totalAmount = data.totalAmount,
        vatAmount = data.vatAmount,
        lineItems = data.lineItems.map { DocLineItem.from(it) },
        receiptNumber = data.receiptNumber,
        notes = data.notes,
        vatBreakdown = data.vatBreakdown,
        paymentMethod = data.paymentMethod,
    )

    is BankStatementDraftData -> DocDto.BankStatement.Draft(
        direction = data.direction,
        accountIban = data.accountIban,
        openingBalance = data.openingBalance,
        closingBalance = data.closingBalance,
        periodStart = data.periodStart,
        periodEnd = data.periodEnd,
        notes = data.notes,
        transactions = data.transactions,
        institution = data.institution,
    )

    // Classified-only types
    is ProFormaDraftData -> DocDto.ProForma.Draft(data.direction)
    is QuoteDraftData -> DocDto.Quote.Draft(data.direction)
    is OrderConfirmationDraftData -> DocDto.OrderConfirmation.Draft(data.direction)
    is DeliveryNoteDraftData -> DocDto.DeliveryNote.Draft(data.direction)
    is ReminderDraftData -> DocDto.Reminder.Draft(data.direction)
    is StatementOfAccountDraftData -> DocDto.StatementOfAccount.Draft(data.direction)
    is PurchaseOrderDraftData -> DocDto.PurchaseOrder.Draft(data.direction)
    is ExpenseClaimDraftData -> DocDto.ExpenseClaim.Draft(data.direction)
    is BankFeeDraftData -> DocDto.BankFee.Draft(data.direction)
    is InterestStatementDraftData -> DocDto.InterestStatement.Draft(data.direction)
    is PaymentConfirmationDraftData -> DocDto.PaymentConfirmation.Draft(data.direction)
    is VatReturnDraftData -> DocDto.VatReturn.Draft(data.direction)
    is VatListingDraftData -> DocDto.VatListing.Draft(data.direction)
    is VatAssessmentDraftData -> DocDto.VatAssessment.Draft(data.direction)
    is IcListingDraftData -> DocDto.IcListing.Draft(data.direction)
    is OssReturnDraftData -> DocDto.OssReturn.Draft(data.direction)
    is CorporateTaxDraftData -> DocDto.CorporateTax.Draft(data.direction)
    is CorporateTaxAdvanceDraftData -> DocDto.CorporateTaxAdvance.Draft(data.direction)
    is TaxAssessmentDraftData -> DocDto.TaxAssessment.Draft(data.direction)
    is PersonalTaxDraftData -> DocDto.PersonalTax.Draft(data.direction)
    is WithholdingTaxDraftData -> DocDto.WithholdingTax.Draft(data.direction)
    is SocialContributionDraftData -> DocDto.SocialContribution.Draft(data.direction)
    is SocialFundDraftData -> DocDto.SocialFund.Draft(data.direction)
    is SelfEmployedContributionDraftData -> DocDto.SelfEmployedContribution.Draft(data.direction)
    is VapzDraftData -> DocDto.Vapz.Draft(data.direction)
    is SalarySlipDraftData -> DocDto.SalarySlip.Draft(data.direction)
    is PayrollSummaryDraftData -> DocDto.PayrollSummary.Draft(data.direction)
    is EmploymentContractDraftData -> DocDto.EmploymentContract.Draft(data.direction)
    is DimonaDraftData -> DocDto.Dimona.Draft(data.direction)
    is C4DraftData -> DocDto.C4.Draft(data.direction)
    is HolidayPayDraftData -> DocDto.HolidayPay.Draft(data.direction)
    is ContractDraftData -> DocDto.Contract.Draft(data.direction)
    is LeaseDraftData -> DocDto.Lease.Draft(data.direction)
    is LoanDraftData -> DocDto.Loan.Draft(data.direction)
    is InsuranceDraftData -> DocDto.Insurance.Draft(data.direction)
    is DividendDraftData -> DocDto.Dividend.Draft(data.direction)
    is ShareholderRegisterDraftData -> DocDto.ShareholderRegister.Draft(data.direction)
    is CompanyExtractDraftData -> DocDto.CompanyExtract.Draft(data.direction)
    is AnnualAccountsDraftData -> DocDto.AnnualAccounts.Draft(data.direction)
    is BoardMinutesDraftData -> DocDto.BoardMinutes.Draft(data.direction)
    is SubsidyDraftData -> DocDto.Subsidy.Draft(data.direction)
    is FineDraftData -> DocDto.Fine.Draft(data.direction)
    is PermitDraftData -> DocDto.Permit.Draft(data.direction)
    is CustomsDeclarationDraftData -> DocDto.CustomsDeclaration.Draft(data.direction)
    is IntrastatDraftData -> DocDto.Intrastat.Draft(data.direction)
    is DepreciationScheduleDraftData -> DocDto.DepreciationSchedule.Draft(data.direction)
    is InventoryDraftData -> DocDto.Inventory.Draft(data.direction)
    is OtherDraftData -> DocDto.Other.Draft(data.direction)
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
// DocLineItem → FinancialLineItemDto (reverse)
// =============================================================================

fun FinancialLineItemDto.Companion.from(item: DocLineItem): FinancialLineItemDto =
    FinancialLineItemDto(
        description = item.description,
        quantity = item.quantity?.value?.toLong(),
        unitPrice = item.unitPrice?.minor,
        vatRate = item.vatRate?.basisPoints,
        netAmount = item.netAmount?.minor,
    )

// =============================================================================
// DocDto → DocumentDraftData (Draft variants only)
// =============================================================================

@Suppress("CyclomaticComplexMethod")
fun DocumentDraftData.Companion.from(dto: DocDto): DocumentDraftData = when (dto) {
    is DocDto.Invoice.Draft -> {
        val (seller, buyer) = splitCounterparty(dto.direction, dto.counterparty)
        InvoiceDraftData(
            direction = dto.direction,
            invoiceNumber = dto.invoiceNumber,
            issueDate = dto.issueDate,
            dueDate = dto.dueDate,
            currency = dto.currency,
            subtotalAmount = dto.subtotalAmount,
            vatAmount = dto.vatAmount,
            totalAmount = dto.totalAmount,
            lineItems = dto.lineItems.map { FinancialLineItemDto.from(it) },
            iban = dto.iban,
            notes = dto.notes,
            vatBreakdown = dto.vatBreakdown,
            payment = dto.payment,
            seller = seller,
            buyer = buyer,
        )
    }

    is DocDto.Invoice.Confirmed -> InvoiceDraftData(
        direction = dto.direction,
        invoiceNumber = dto.invoiceNumber,
        issueDate = dto.issueDate,
        dueDate = dto.dueDate,
        currency = dto.currency,
        subtotalAmount = dto.subtotalAmount,
        vatAmount = dto.vatAmount,
        totalAmount = dto.totalAmount,
        lineItems = dto.lineItems.map { FinancialLineItemDto.from(it) },
        iban = dto.iban,
        notes = dto.notes,
    )

    is DocDto.CreditNote.Draft -> {
        val (seller, buyer) = splitCounterparty(dto.direction, dto.counterparty)
        CreditNoteDraftData(
            direction = dto.direction,
            creditNoteNumber = dto.creditNoteNumber,
            issueDate = dto.issueDate,
            currency = dto.currency,
            subtotalAmount = dto.subtotalAmount,
            vatAmount = dto.vatAmount,
            totalAmount = dto.totalAmount,
            lineItems = dto.lineItems.map { FinancialLineItemDto.from(it) },
            reason = dto.reason,
            notes = dto.notes,
            vatBreakdown = dto.vatBreakdown,
            originalInvoiceNumber = dto.originalInvoiceNumber,
            seller = seller,
            buyer = buyer,
        )
    }

    is DocDto.CreditNote.Confirmed -> CreditNoteDraftData(
        direction = dto.direction,
        creditNoteNumber = dto.creditNoteNumber,
        issueDate = dto.issueDate,
        currency = dto.currency,
        subtotalAmount = dto.subtotalAmount,
        vatAmount = dto.vatAmount,
        totalAmount = dto.totalAmount,
        lineItems = dto.lineItems.map { FinancialLineItemDto.from(it) },
        reason = dto.reason,
        notes = dto.notes,
    )

    is DocDto.Receipt.Draft -> ReceiptDraftData(
        direction = dto.direction,
        merchantName = dto.merchantName,
        merchantVat = dto.merchantVat,
        date = dto.date,
        currency = dto.currency,
        totalAmount = dto.totalAmount,
        vatAmount = dto.vatAmount,
        lineItems = dto.lineItems.map { FinancialLineItemDto.from(it) },
        receiptNumber = dto.receiptNumber,
        notes = dto.notes,
        vatBreakdown = dto.vatBreakdown,
        paymentMethod = dto.paymentMethod,
    )

    is DocDto.Receipt.Confirmed -> ReceiptDraftData(
        direction = dto.direction,
        merchantName = dto.merchantName,
        merchantVat = dto.merchantVat,
        date = dto.date,
        currency = dto.currency,
        totalAmount = dto.totalAmount,
        vatAmount = dto.vatAmount,
        lineItems = dto.lineItems.map { FinancialLineItemDto.from(it) },
        receiptNumber = dto.receiptNumber,
        notes = dto.notes,
    )

    is DocDto.BankStatement.Draft -> BankStatementDraftData(
        direction = dto.direction,
        accountIban = dto.accountIban,
        openingBalance = dto.openingBalance,
        closingBalance = dto.closingBalance,
        periodStart = dto.periodStart,
        periodEnd = dto.periodEnd,
        notes = dto.notes,
        transactions = dto.transactions,
        institution = dto.institution,
    )

    is DocDto.BankStatement.Confirmed -> BankStatementDraftData(
        direction = dto.direction,
        accountIban = dto.accountIban,
        openingBalance = dto.openingBalance,
        closingBalance = dto.closingBalance,
        periodStart = dto.periodStart,
        periodEnd = dto.periodEnd,
        notes = dto.notes,
    )

    is DocDto.ProForma -> ProFormaDraftData(dto.direction)
    is DocDto.Quote -> QuoteDraftData(dto.direction)
    is DocDto.OrderConfirmation -> OrderConfirmationDraftData(dto.direction)
    is DocDto.DeliveryNote -> DeliveryNoteDraftData(dto.direction)
    is DocDto.Reminder -> ReminderDraftData(dto.direction)
    is DocDto.StatementOfAccount -> StatementOfAccountDraftData(dto.direction)
    is DocDto.PurchaseOrder -> PurchaseOrderDraftData(dto.direction)
    is DocDto.ExpenseClaim -> ExpenseClaimDraftData(dto.direction)
    is DocDto.BankFee -> BankFeeDraftData(dto.direction)
    is DocDto.InterestStatement -> InterestStatementDraftData(dto.direction)
    is DocDto.PaymentConfirmation -> PaymentConfirmationDraftData(dto.direction)
    is DocDto.VatReturn -> VatReturnDraftData(dto.direction)
    is DocDto.VatListing -> VatListingDraftData(dto.direction)
    is DocDto.VatAssessment -> VatAssessmentDraftData(dto.direction)
    is DocDto.IcListing -> IcListingDraftData(dto.direction)
    is DocDto.OssReturn -> OssReturnDraftData(dto.direction)
    is DocDto.CorporateTax -> CorporateTaxDraftData(dto.direction)
    is DocDto.CorporateTaxAdvance -> CorporateTaxAdvanceDraftData(dto.direction)
    is DocDto.TaxAssessment -> TaxAssessmentDraftData(dto.direction)
    is DocDto.PersonalTax -> PersonalTaxDraftData(dto.direction)
    is DocDto.WithholdingTax -> WithholdingTaxDraftData(dto.direction)
    is DocDto.SocialContribution -> SocialContributionDraftData(dto.direction)
    is DocDto.SocialFund -> SocialFundDraftData(dto.direction)
    is DocDto.SelfEmployedContribution -> SelfEmployedContributionDraftData(dto.direction)
    is DocDto.Vapz -> VapzDraftData(dto.direction)
    is DocDto.SalarySlip -> SalarySlipDraftData(dto.direction)
    is DocDto.PayrollSummary -> PayrollSummaryDraftData(dto.direction)
    is DocDto.EmploymentContract -> EmploymentContractDraftData(dto.direction)
    is DocDto.Dimona -> DimonaDraftData(dto.direction)
    is DocDto.C4 -> C4DraftData(dto.direction)
    is DocDto.HolidayPay -> HolidayPayDraftData(dto.direction)
    is DocDto.Contract -> ContractDraftData(dto.direction)
    is DocDto.Lease -> LeaseDraftData(dto.direction)
    is DocDto.Loan -> LoanDraftData(dto.direction)
    is DocDto.Insurance -> InsuranceDraftData(dto.direction)
    is DocDto.Dividend -> DividendDraftData(dto.direction)
    is DocDto.ShareholderRegister -> ShareholderRegisterDraftData(dto.direction)
    is DocDto.CompanyExtract -> CompanyExtractDraftData(dto.direction)
    is DocDto.AnnualAccounts -> AnnualAccountsDraftData(dto.direction)
    is DocDto.BoardMinutes -> BoardMinutesDraftData(dto.direction)
    is DocDto.Subsidy -> SubsidyDraftData(dto.direction)
    is DocDto.Fine -> FineDraftData(dto.direction)
    is DocDto.Permit -> PermitDraftData(dto.direction)
    is DocDto.CustomsDeclaration -> CustomsDeclarationDraftData(dto.direction)
    is DocDto.Intrastat -> IntrastatDraftData(dto.direction)
    is DocDto.DepreciationSchedule -> DepreciationScheduleDraftData(dto.direction)
    is DocDto.Inventory -> InventoryDraftData(dto.direction)
    is DocDto.Other -> OtherDraftData(dto.direction)
}
