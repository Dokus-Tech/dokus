package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Email
import tech.dokus.domain.LegalName
import tech.dokus.domain.Money
import tech.dokus.domain.Name
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.enums.BankProvider
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.EntityType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.InvitationStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.AddressId
import tech.dokus.domain.ids.AttachmentId
import tech.dokus.domain.ids.BankConnectionId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.Bic
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PaymentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.TransactionId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.common.Thumbnail

@Serializable
data class Tenant(
    val id: TenantId,
    val type: TenantType,
    val legalName: LegalName,
    val displayName: DisplayName,
    val subscription: SubscriptionTier,
    val status: TenantStatus,
    val language: Language,
    val vatNumber: VatNumber,
    val trialEndsAt: LocalDateTime? = null,
    val subscriptionStartedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val avatar: Thumbnail? = null,
)

/**
 * Shared address model used for both tenant company addresses and contact addresses.
 * All fields nullable to support AI partial extraction for contact addresses.
 * Tenant company addresses enforce required fields at service layer.
 * Country stored as ISO 3166-1 alpha-2 string (e.g., "BE", "NL").
 */
@Serializable
data class Address(
    val id: AddressId,
    val tenantId: TenantId,
    val streetLine1: String? = null,
    val streetLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,  // ISO 3166-1 alpha-2
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class TenantSettings(
    val tenantId: TenantId,
    val invoicePrefix: String = "INV",
    val nextInvoiceNumber: Int = 1,
    val defaultPaymentTerms: Int = 30,
    val defaultVatRate: VatRate = VatRate.STANDARD_BE,
    val invoiceYearlyReset: Boolean = true,
    val invoicePadding: Int = 4,
    val invoiceIncludeYear: Boolean = true,
    val invoiceTimezone: String = "Europe/Brussels",
    val companyName: String? = null,
    val companyIban: Iban? = null,
    val companyBic: Bic? = null,
    val companyLogoUrl: String? = null,
    val emailInvoiceReminders: Boolean = true,
    val emailPaymentConfirmations: Boolean = true,
    val emailWeeklyReports: Boolean = false,
    val enableBankSync: Boolean = false,
    val enablePeppol: Boolean = false,
    val paymentTermsText: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * User identity - does not include tenant info.
 * Users can belong to multiple tenants via TenantMembership.
 */
@Serializable
data class User(
    val id: UserId,
    val email: Email,
    val firstName: Name? = null,
    val lastName: Name? = null,
    val emailVerified: Boolean = false,
    val isActive: Boolean = true,
    val lastLoginAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Represents a user's membership in a tenant with their role.
 */
@Serializable
data class TenantMembership(
    val userId: UserId,
    val tenantId: TenantId,
    val role: UserRole,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * User with specific tenant context - used when working within a tenant.
 */
@Serializable
data class UserInTenant(
    val user: User,
    val tenantId: TenantId,
    val role: UserRole,
    val membershipActive: Boolean = true
)

// ============================================================================
// TEAM MANAGEMENT
// ============================================================================

/**
 * Team member with display information for the team list.
 */
@Serializable
data class TeamMember(
    val userId: UserId,
    val email: Email,
    val firstName: Name?,
    val lastName: Name?,
    val role: UserRole,
    val joinedAt: LocalDateTime,
    val lastActiveAt: LocalDateTime?
) {
    val fullName: String
        get() = listOfNotNull(firstName?.value, lastName?.value)
            .joinToString(" ")
            .ifEmpty { email.value }
}

/**
 * Invitation to join a tenant workspace.
 */
@Serializable
data class TenantInvitation(
    val id: InvitationId,
    val tenantId: TenantId,
    val email: Email,
    val role: UserRole,
    val invitedByName: String,
    val status: InvitationStatus,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime
)

/**
 * Request DTO for creating a team invitation.
 */
@Serializable
data class CreateInvitationRequest(
    val email: Email,
    val role: UserRole
)

/**
 * Request DTO for updating a team member's role.
 */
@Serializable
data class UpdateMemberRoleRequest(
    val role: UserRole
)

/**
 * Request DTO for transferring workspace ownership.
 */
@Serializable
data class TransferOwnershipRequest(
    val newOwnerId: UserId
)

// NOTE: ClientDto has been replaced by ContactDto in Contact.kt
// Use ContactDto from Contact.kt for all customer/vendor operations

// NOTE: Invoice and Expense classes have been consolidated into FinancialDocumentDto
// Use InvoiceDto and ExpenseDto from FinancialDocument.kt

// ============================================================================
// PAYMENTS
// ============================================================================

@Serializable
data class PaymentDto(
    val id: PaymentId,
    val tenantId: TenantId,
    val invoiceId: InvoiceId,
    val amount: Money,
    val paymentDate: LocalDate,
    val paymentMethod: PaymentMethod,
    val transactionId: TransactionId? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime
)

// ============================================================================
// BANKING
// ============================================================================

@Serializable
data class BankConnectionDto(
    val id: BankConnectionId,
    val tenantId: TenantId,
    val provider: BankProvider,
    val institutionId: String,
    val institutionName: String,
    val accountId: String,
    val accountName: String? = null,
    val accountType: BankAccountType? = null,
    val currency: Currency = Currency.Eur,
    val lastSyncedAt: LocalDateTime? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class BankTransactionDto(
    val id: BankTransactionId,
    val bankConnectionId: BankConnectionId,
    val tenantId: TenantId,
    val externalId: String,
    val date: LocalDate,
    val amount: Money,
    val description: String,
    val merchantName: String? = null,
    val category: String? = null,
    val isPending: Boolean = false,
    val expenseId: ExpenseId? = null,
    val invoiceId: InvoiceId? = null,
    val isReconciled: Boolean = false,
    val createdAt: LocalDateTime
)

// ============================================================================
// AUDIT & ATTACHMENTS
// ============================================================================

@Serializable
data class AttachmentDto(
    val id: AttachmentId,
    val tenantId: TenantId,
    val entityType: EntityType,
    val entityId: String, // Generic entity ID as string since it could be any entity
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val s3Key: String,
    val s3Bucket: String,
    val uploadedAt: LocalDateTime
)

// ============================================================================
// REQUEST/RESPONSE MODELS
// ============================================================================

/**
 * Request DTO for creating a tenant.
 * Address is required for all tenant types.
 */
@Serializable
data class CreateTenantRequest(
    val type: TenantType,
    val legalName: LegalName,
    val displayName: DisplayName,
    val subscription: SubscriptionTier = SubscriptionTier.default,
    val language: Language = Language.En,
    val vatNumber: VatNumber,
    val address: UpsertTenantAddressRequest,
)

@Serializable
data class CreateInvoiceRequest(
    val contactId: ContactId,
    val direction: DocumentDirection = DocumentDirection.Outbound,
    val items: List<InvoiceItemDto>,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val notes: String? = null,
    val documentId: DocumentId? = null,
    val subtotalAmount: Money? = null,
    val vatAmount: Money? = null,
    val totalAmount: Money? = null
)

@Serializable
data class UpdateInvoiceStatusRequest(
    val invoiceId: InvoiceId,
    val status: InvoiceStatus
)

@Serializable
data class RecordPaymentRequest(
    val invoiceId: InvoiceId,
    val amount: Money,
    val paymentDate: LocalDate,
    val paymentMethod: PaymentMethod,
    val transactionId: TransactionId? = null,
    val notes: String? = null
)

@Serializable
data class CreateExpenseRequest(
    val date: LocalDate,
    val merchant: String,
    val amount: Money,
    val vatAmount: Money? = null,
    val vatRate: VatRate? = null,
    val category: ExpenseCategory,
    val description: String? = null,
    val documentId: DocumentId? = null,
    val contactId: ContactId? = null,
    val isDeductible: Boolean? = null,
    val deductiblePercentage: Percentage? = null,
    val isRecurring: Boolean? = null,
    val paymentMethod: PaymentMethod? = null,
    val notes: String? = null
)

@Serializable
data class InvoiceTotals(
    val subtotal: Money,
    val vatAmount: Money,
    val total: Money
)

@Serializable
data class VatCalculation(
    val salesVat: Money,
    val purchaseVat: Money,
    val netVat: Money
)

@Serializable
data class UploadInfo(
    val uploadUrl: String,
    val s3Key: String
)

@Serializable
data class QuarterInfo(
    val year: Int,
    val quarter: Int
)

// ============================================================================
// FROM-MEDIA CREATION MODELS
// ============================================================================

/**
 * Request to create an invoice from processed media extraction.
 */
@Serializable
data class CreateInvoiceFromMediaRequest(
    val contactId: ContactId,
    val corrections: InvoiceCorrections? = null
)

@Serializable
data class InvoiceCorrections(
    val invoiceNumber: String? = null,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val notes: String? = null
)

/**
 * Request to create an expense from processed media extraction.
 */
@Serializable
data class CreateExpenseFromMediaRequest(
    val corrections: ExpenseCorrections? = null
)

@Serializable
data class ExpenseCorrections(
    val merchant: String? = null,
    val date: LocalDate? = null,
    val amount: Money? = null,
    val vatAmount: Money? = null,
    val vatRate: VatRate? = null,
    val category: ExpenseCategory? = null,
    val isDeductible: Boolean? = null,
    val deductiblePercentage: Percentage? = null,
    val notes: String? = null
)

/**
 * Response when creating an entity from a document.
 */
@Serializable
data class CreatedFromDocumentResponse<T>(
    val entity: T,
    val documentId: DocumentId
)

/**
 * Response for invoice number preview.
 * Used to show the next expected invoice number before creation.
 */
@Serializable
data class InvoiceNumberPreviewResponse(
    val invoiceNumber: String
)

// NOTE: CashflowOverview, CashflowPeriod, CashInSummary, CashOutSummary
// are defined in Cashflow.kt

// ============================================================================
// CREDIT NOTE REQUEST/RESPONSE MODELS
// ============================================================================

/**
 * Request DTO for creating a credit note.
 */
@Serializable
data class CreateCreditNoteRequest(
    val contactId: ContactId,
    val creditNoteType: tech.dokus.domain.enums.CreditNoteType,
    val creditNoteNumber: String,
    val issueDate: LocalDate,
    val subtotalAmount: Money,
    val vatAmount: Money,
    val totalAmount: Money,
    val currency: Currency = Currency.Eur,
    val settlementIntent: tech.dokus.domain.enums.SettlementIntent =
        tech.dokus.domain.enums.SettlementIntent.Unknown,
    val reason: String? = null,
    val notes: String? = null,
    val documentId: DocumentId? = null
)

/**
 * Request DTO for recording a refund payment for a credit note.
 * Creates a cashflow entry and settles the refund claim.
 */
@Serializable
data class RecordRefundRequest(
    val amount: Money,
    val refundDate: LocalDate,
    val paymentMethod: PaymentMethod,
    val reference: String? = null,
    val notes: String? = null
)
