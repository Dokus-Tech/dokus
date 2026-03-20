@file:Suppress("LongParameterList")

package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.CreditNoteStatus
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.SettlementIntent
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.CounterpartySnapshotDto

/**
 * Unified document content model.
 *
 * Each document type is a sealed interface with [Draft] and [Confirmed] variants
 * that share display fields. The frontend renders the same fields regardless of state;
 * Draft is editable, Confirmed is read-only.
 *
 * Core financial types (Invoice, CreditNote, Receipt, BankStatement) have structured
 * extraction fields. All other types extend [ClassifiedDoc] with just [direction].
 */
@Serializable
sealed interface DocDto {
    val direction: DocumentDirection

    // =========================================================================
    // Invoice
    // =========================================================================

    sealed interface Invoice : DocDto {
        val invoiceNumber: String?
        val issueDate: LocalDate?
        val dueDate: LocalDate?
        val currency: Currency
        val subtotalAmount: Money?
        val vatAmount: Money?
        val totalAmount: Money?
        val lineItems: List<DocLineItem>
        val notes: String?
        val iban: Iban?

        @Serializable
        @SerialName("DocDto.Invoice.Draft")
        data class Draft(
            override val direction: DocumentDirection = DocumentDirection.Unknown,
            override val invoiceNumber: String? = null,
            override val issueDate: LocalDate? = null,
            override val dueDate: LocalDate? = null,
            override val currency: Currency = Currency.default,
            override val subtotalAmount: Money? = null,
            override val vatAmount: Money? = null,
            override val totalAmount: Money? = null,
            override val lineItems: List<DocLineItem> = emptyList(),
            override val iban: Iban? = null,
            override val notes: String? = null,
            val vatBreakdown: List<VatBreakdownEntryDto> = emptyList(),
            val payment: CanonicalPaymentDto? = null,
            val counterparty: PartyDraftDto = PartyDraftDto(),
        ) : Invoice {
            companion object
        }

        @Serializable
        @SerialName("DocDto.Invoice.Confirmed")
        data class Confirmed(
            val id: InvoiceId,
            val tenantId: TenantId,
            val contactId: ContactId,
            override val direction: DocumentDirection = DocumentDirection.Outbound,
            override val invoiceNumber: String? = null,
            override val issueDate: LocalDate? = null,
            override val dueDate: LocalDate? = null,
            override val currency: Currency = Currency.Eur,
            override val subtotalAmount: Money? = null,
            override val vatAmount: Money? = null,
            override val totalAmount: Money? = null,
            val paidAmount: Money = Money.ZERO,
            override val lineItems: List<DocLineItem> = emptyList(),
            override val iban: Iban? = null,
            override val notes: String? = null,
            val status: InvoiceStatus,
            val structuredCommunication: StructuredCommunication? = null,
            val peppol: InvoicePeppolInfo? = null,
            val paymentLinkInfo: PaymentLinkInfo? = null,
            val paymentInfo: InvoicePaymentInfo? = null,
            val cashflowEntryId: CashflowEntryId? = null,
            val documentId: DocumentId? = null,
            val confirmedAt: LocalDateTime? = null,
            val confirmedBy: UserId? = null,
            val createdAt: LocalDateTime,
            val updatedAt: LocalDateTime,
        ) : Invoice {
            companion object
        }
    }

    // =========================================================================
    // CreditNote
    // =========================================================================

    sealed interface CreditNote : DocDto {
        val creditNoteNumber: String?
        val issueDate: LocalDate?
        val currency: Currency
        val subtotalAmount: Money?
        val vatAmount: Money?
        val totalAmount: Money?
        val lineItems: List<DocLineItem>
        val reason: String?
        val notes: String?

        @Serializable
        @SerialName("DocDto.CreditNote.Draft")
        data class Draft(
            override val direction: DocumentDirection = DocumentDirection.Unknown,
            override val creditNoteNumber: String? = null,
            override val issueDate: LocalDate? = null,
            override val currency: Currency = Currency.default,
            override val subtotalAmount: Money? = null,
            override val vatAmount: Money? = null,
            override val totalAmount: Money? = null,
            override val lineItems: List<DocLineItem> = emptyList(),
            override val reason: String? = null,
            override val notes: String? = null,
            val vatBreakdown: List<VatBreakdownEntryDto> = emptyList(),
            val originalInvoiceNumber: String? = null,
            val counterparty: PartyDraftDto = PartyDraftDto(),
        ) : CreditNote {
            companion object
        }

        @Serializable
        @SerialName("DocDto.CreditNote.Confirmed")
        data class Confirmed(
            val id: CreditNoteId,
            val tenantId: TenantId,
            val contactId: ContactId,
            val creditNoteType: CreditNoteType,
            override val direction: DocumentDirection,
            override val creditNoteNumber: String? = null,
            override val issueDate: LocalDate? = null,
            override val currency: Currency = Currency.Eur,
            override val subtotalAmount: Money? = null,
            override val vatAmount: Money? = null,
            override val totalAmount: Money? = null,
            override val lineItems: List<DocLineItem> = emptyList(),
            val status: CreditNoteStatus,
            val settlementIntent: SettlementIntent,
            override val reason: String? = null,
            override val notes: String? = null,
            val documentId: DocumentId? = null,
            val confirmedAt: LocalDateTime? = null,
            val confirmedBy: UserId? = null,
            val createdAt: LocalDateTime,
            val updatedAt: LocalDateTime,
        ) : CreditNote {
            companion object
        }
    }

    // =========================================================================
    // Receipt
    // =========================================================================

    sealed interface Receipt : DocDto {
        val merchantName: String?
        val merchantVat: VatNumber?
        val date: LocalDate?
        val currency: Currency
        val totalAmount: Money?
        val vatAmount: Money?
        val lineItems: List<DocLineItem>
        val receiptNumber: String?
        val notes: String?

        @Serializable
        @SerialName("DocDto.Receipt.Draft")
        data class Draft(
            override val direction: DocumentDirection = DocumentDirection.Inbound,
            override val merchantName: String? = null,
            override val merchantVat: VatNumber? = null,
            override val date: LocalDate? = null,
            override val currency: Currency = Currency.default,
            override val totalAmount: Money? = null,
            override val vatAmount: Money? = null,
            override val lineItems: List<DocLineItem> = emptyList(),
            override val receiptNumber: String? = null,
            override val notes: String? = null,
            val vatBreakdown: List<VatBreakdownEntryDto> = emptyList(),
            val paymentMethod: PaymentMethod? = null,
        ) : Receipt {
            companion object
        }

        @Serializable
        @SerialName("DocDto.Receipt.Confirmed")
        data class Confirmed(
            val id: ExpenseId,
            val tenantId: TenantId,
            override val direction: DocumentDirection = DocumentDirection.Inbound,
            override val merchantName: String? = null,
            override val merchantVat: VatNumber? = null,
            override val date: LocalDate? = null,
            override val currency: Currency = Currency.Eur,
            override val totalAmount: Money? = null,
            override val vatAmount: Money? = null,
            override val lineItems: List<DocLineItem> = emptyList(),
            override val receiptNumber: String? = null,
            override val notes: String? = null,
            val vatRate: VatRate? = null,
            val category: ExpenseCategory,
            val isDeductible: Boolean = true,
            val deductiblePercentage: Percentage = Percentage.FULL,
            val paymentMethod: PaymentMethod? = null,
            val contactId: ContactId? = null,
            val documentId: DocumentId? = null,
            val confirmedAt: LocalDateTime? = null,
            val confirmedBy: UserId? = null,
            val createdAt: LocalDateTime,
            val updatedAt: LocalDateTime,
        ) : Receipt {
            companion object
        }
    }

    // =========================================================================
    // BankStatement
    // =========================================================================

    sealed interface BankStatement : DocDto {
        val accountIban: Iban?
        val openingBalance: Money?
        val closingBalance: Money?
        val periodStart: LocalDate?
        val periodEnd: LocalDate?
        val notes: String?

        @Serializable
        @SerialName("DocDto.BankStatement.Draft")
        data class Draft(
            override val direction: DocumentDirection = DocumentDirection.Neutral,
            override val accountIban: Iban? = null,
            override val openingBalance: Money? = null,
            override val closingBalance: Money? = null,
            override val periodStart: LocalDate? = null,
            override val periodEnd: LocalDate? = null,
            override val notes: String? = null,
            val transactions: List<BankStatementTransactionDraftRowDto> = emptyList(),
            val institution: PartyDraftDto = PartyDraftDto(),
        ) : BankStatement {
            companion object
        }

        @Serializable
        @SerialName("DocDto.BankStatement.Confirmed")
        data class Confirmed(
            override val direction: DocumentDirection = DocumentDirection.Neutral,
            override val accountIban: Iban? = null,
            override val openingBalance: Money? = null,
            override val closingBalance: Money? = null,
            override val periodStart: LocalDate? = null,
            override val periodEnd: LocalDate? = null,
            override val notes: String? = null,
            val transactionCount: Int = 0,
        ) : BankStatement {
            companion object
        }
    }

    // =========================================================================
    // ClassifiedDoc — shared marker for all 46 classified-only types
    // =========================================================================

    sealed interface ClassifiedDoc : DocDto {
        override val direction: DocumentDirection
    }

    // =========================================================================
    // Classified-only types — Draft/Confirmed pairs with just direction
    // =========================================================================

    sealed interface ProForma : ClassifiedDoc {
        @Serializable @SerialName("DocDto.ProForma.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : ProForma
        @Serializable @SerialName("DocDto.ProForma.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : ProForma
    }

    sealed interface Quote : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Quote.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Quote
        @Serializable @SerialName("DocDto.Quote.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Quote
    }

    sealed interface OrderConfirmation : ClassifiedDoc {
        @Serializable @SerialName("DocDto.OrderConfirmation.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : OrderConfirmation
        @Serializable @SerialName("DocDto.OrderConfirmation.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : OrderConfirmation
    }

    sealed interface DeliveryNote : ClassifiedDoc {
        @Serializable @SerialName("DocDto.DeliveryNote.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : DeliveryNote
        @Serializable @SerialName("DocDto.DeliveryNote.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : DeliveryNote
    }

    sealed interface Reminder : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Reminder.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Reminder
        @Serializable @SerialName("DocDto.Reminder.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Reminder
    }

    sealed interface StatementOfAccount : ClassifiedDoc {
        @Serializable @SerialName("DocDto.StatementOfAccount.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : StatementOfAccount
        @Serializable @SerialName("DocDto.StatementOfAccount.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : StatementOfAccount
    }

    sealed interface PurchaseOrder : ClassifiedDoc {
        @Serializable @SerialName("DocDto.PurchaseOrder.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : PurchaseOrder
        @Serializable @SerialName("DocDto.PurchaseOrder.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : PurchaseOrder
    }

    sealed interface ExpenseClaim : ClassifiedDoc {
        @Serializable @SerialName("DocDto.ExpenseClaim.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : ExpenseClaim
        @Serializable @SerialName("DocDto.ExpenseClaim.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : ExpenseClaim
    }

    sealed interface BankFee : ClassifiedDoc {
        @Serializable @SerialName("DocDto.BankFee.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : BankFee
        @Serializable @SerialName("DocDto.BankFee.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : BankFee
    }

    sealed interface InterestStatement : ClassifiedDoc {
        @Serializable @SerialName("DocDto.InterestStatement.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : InterestStatement
        @Serializable @SerialName("DocDto.InterestStatement.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : InterestStatement
    }

    sealed interface PaymentConfirmation : ClassifiedDoc {
        @Serializable @SerialName("DocDto.PaymentConfirmation.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : PaymentConfirmation
        @Serializable @SerialName("DocDto.PaymentConfirmation.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : PaymentConfirmation
    }

    sealed interface VatReturn : ClassifiedDoc {
        @Serializable @SerialName("DocDto.VatReturn.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : VatReturn
        @Serializable @SerialName("DocDto.VatReturn.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : VatReturn
    }

    sealed interface VatListing : ClassifiedDoc {
        @Serializable @SerialName("DocDto.VatListing.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : VatListing
        @Serializable @SerialName("DocDto.VatListing.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : VatListing
    }

    sealed interface VatAssessment : ClassifiedDoc {
        @Serializable @SerialName("DocDto.VatAssessment.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : VatAssessment
        @Serializable @SerialName("DocDto.VatAssessment.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : VatAssessment
    }

    sealed interface IcListing : ClassifiedDoc {
        @Serializable @SerialName("DocDto.IcListing.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : IcListing
        @Serializable @SerialName("DocDto.IcListing.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : IcListing
    }

    sealed interface OssReturn : ClassifiedDoc {
        @Serializable @SerialName("DocDto.OssReturn.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : OssReturn
        @Serializable @SerialName("DocDto.OssReturn.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : OssReturn
    }

    sealed interface CorporateTax : ClassifiedDoc {
        @Serializable @SerialName("DocDto.CorporateTax.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : CorporateTax
        @Serializable @SerialName("DocDto.CorporateTax.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : CorporateTax
    }

    sealed interface CorporateTaxAdvance : ClassifiedDoc {
        @Serializable @SerialName("DocDto.CorporateTaxAdvance.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : CorporateTaxAdvance
        @Serializable @SerialName("DocDto.CorporateTaxAdvance.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : CorporateTaxAdvance
    }

    sealed interface TaxAssessment : ClassifiedDoc {
        @Serializable @SerialName("DocDto.TaxAssessment.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : TaxAssessment
        @Serializable @SerialName("DocDto.TaxAssessment.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : TaxAssessment
    }

    sealed interface PersonalTax : ClassifiedDoc {
        @Serializable @SerialName("DocDto.PersonalTax.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : PersonalTax
        @Serializable @SerialName("DocDto.PersonalTax.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : PersonalTax
    }

    sealed interface WithholdingTax : ClassifiedDoc {
        @Serializable @SerialName("DocDto.WithholdingTax.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : WithholdingTax
        @Serializable @SerialName("DocDto.WithholdingTax.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : WithholdingTax
    }

    sealed interface SocialContribution : ClassifiedDoc {
        @Serializable @SerialName("DocDto.SocialContribution.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : SocialContribution
        @Serializable @SerialName("DocDto.SocialContribution.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : SocialContribution
    }

    sealed interface SocialFund : ClassifiedDoc {
        @Serializable @SerialName("DocDto.SocialFund.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : SocialFund
        @Serializable @SerialName("DocDto.SocialFund.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : SocialFund
    }

    sealed interface SelfEmployedContribution : ClassifiedDoc {
        @Serializable @SerialName("DocDto.SelfEmployedContribution.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : SelfEmployedContribution
        @Serializable @SerialName("DocDto.SelfEmployedContribution.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : SelfEmployedContribution
    }

    sealed interface Vapz : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Vapz.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Vapz
        @Serializable @SerialName("DocDto.Vapz.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Vapz
    }

    sealed interface SalarySlip : ClassifiedDoc {
        @Serializable @SerialName("DocDto.SalarySlip.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : SalarySlip
        @Serializable @SerialName("DocDto.SalarySlip.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : SalarySlip
    }

    sealed interface PayrollSummary : ClassifiedDoc {
        @Serializable @SerialName("DocDto.PayrollSummary.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : PayrollSummary
        @Serializable @SerialName("DocDto.PayrollSummary.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : PayrollSummary
    }

    sealed interface EmploymentContract : ClassifiedDoc {
        @Serializable @SerialName("DocDto.EmploymentContract.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : EmploymentContract
        @Serializable @SerialName("DocDto.EmploymentContract.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : EmploymentContract
    }

    sealed interface Dimona : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Dimona.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Dimona
        @Serializable @SerialName("DocDto.Dimona.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Dimona
    }

    sealed interface C4 : ClassifiedDoc {
        @Serializable @SerialName("DocDto.C4.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : C4
        @Serializable @SerialName("DocDto.C4.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : C4
    }

    sealed interface HolidayPay : ClassifiedDoc {
        @Serializable @SerialName("DocDto.HolidayPay.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : HolidayPay
        @Serializable @SerialName("DocDto.HolidayPay.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : HolidayPay
    }

    sealed interface Contract : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Contract.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Contract
        @Serializable @SerialName("DocDto.Contract.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Contract
    }

    sealed interface Lease : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Lease.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Lease
        @Serializable @SerialName("DocDto.Lease.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Lease
    }

    sealed interface Loan : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Loan.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Loan
        @Serializable @SerialName("DocDto.Loan.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Loan
    }

    sealed interface Insurance : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Insurance.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Insurance
        @Serializable @SerialName("DocDto.Insurance.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Insurance
    }

    sealed interface Dividend : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Dividend.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Dividend
        @Serializable @SerialName("DocDto.Dividend.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Dividend
    }

    sealed interface ShareholderRegister : ClassifiedDoc {
        @Serializable @SerialName("DocDto.ShareholderRegister.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : ShareholderRegister
        @Serializable @SerialName("DocDto.ShareholderRegister.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : ShareholderRegister
    }

    sealed interface CompanyExtract : ClassifiedDoc {
        @Serializable @SerialName("DocDto.CompanyExtract.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : CompanyExtract
        @Serializable @SerialName("DocDto.CompanyExtract.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : CompanyExtract
    }

    sealed interface AnnualAccounts : ClassifiedDoc {
        @Serializable @SerialName("DocDto.AnnualAccounts.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : AnnualAccounts
        @Serializable @SerialName("DocDto.AnnualAccounts.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : AnnualAccounts
    }

    sealed interface BoardMinutes : ClassifiedDoc {
        @Serializable @SerialName("DocDto.BoardMinutes.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : BoardMinutes
        @Serializable @SerialName("DocDto.BoardMinutes.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : BoardMinutes
    }

    sealed interface Subsidy : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Subsidy.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Subsidy
        @Serializable @SerialName("DocDto.Subsidy.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Subsidy
    }

    sealed interface Fine : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Fine.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Fine
        @Serializable @SerialName("DocDto.Fine.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Fine
    }

    sealed interface Permit : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Permit.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Permit
        @Serializable @SerialName("DocDto.Permit.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Permit
    }

    sealed interface CustomsDeclaration : ClassifiedDoc {
        @Serializable @SerialName("DocDto.CustomsDeclaration.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : CustomsDeclaration
        @Serializable @SerialName("DocDto.CustomsDeclaration.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : CustomsDeclaration
    }

    sealed interface Intrastat : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Intrastat.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Intrastat
        @Serializable @SerialName("DocDto.Intrastat.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Intrastat
    }

    sealed interface DepreciationSchedule : ClassifiedDoc {
        @Serializable @SerialName("DocDto.DepreciationSchedule.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : DepreciationSchedule
        @Serializable @SerialName("DocDto.DepreciationSchedule.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : DepreciationSchedule
    }

    sealed interface Inventory : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Inventory.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Inventory
        @Serializable @SerialName("DocDto.Inventory.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Inventory
    }

    sealed interface Other : ClassifiedDoc {
        @Serializable @SerialName("DocDto.Other.Draft")
        data class Draft(override val direction: DocumentDirection = DocumentDirection.Unknown) : Other
        @Serializable @SerialName("DocDto.Other.Confirmed")
        data class Confirmed(override val direction: DocumentDirection = DocumentDirection.Unknown) : Other
    }
}
