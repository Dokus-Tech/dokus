package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.Percentage
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.BankAccountType
import ai.dokus.foundation.domain.enums.BankProvider
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.enums.TenantStatus
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.enums.VatReturnStatus
import ai.dokus.foundation.domain.ids.AttachmentId
import ai.dokus.foundation.domain.ids.AuditLogId
import ai.dokus.foundation.domain.ids.BankConnectionId
import ai.dokus.foundation.domain.ids.BankTransactionId
import ai.dokus.foundation.domain.ids.Bic
import ai.dokus.foundation.domain.ids.BusinessUserId
import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.Iban
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.PaymentId
import ai.dokus.foundation.domain.ids.TransactionId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.ids.VatReturnId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

// ============================================================================
// TENANT & USER MANAGEMENT
// ============================================================================

@Serializable
data class Organization(
    val id: OrganizationId,
    val name: String,
    val email: String,
    val plan: OrganizationPlan,
    val status: TenantStatus,
    val country: String,
    val language: Language,
    val vatNumber: VatNumber? = null,
    val trialEndsAt: LocalDateTime? = null,
    val subscriptionStartedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class OrganizationSettings(
    val organizationId: OrganizationId,
    val invoicePrefix: String = "INV",
    val nextInvoiceNumber: Int = 1,
    val defaultPaymentTerms: Int = 30,
    val defaultVatRate: VatRate = VatRate.STANDARD_BE,
    val companyName: String? = null,
    val companyAddress: String? = null,
    val companyVatNumber: VatNumber? = null,
    val companyIban: Iban? = null,
    val companyBic: Bic? = null,
    val companyLogoUrl: String? = null,
    val emailInvoiceReminders: Boolean = true,
    val emailPaymentConfirmations: Boolean = true,
    val emailWeeklyReports: Boolean = false,
    val enableBankSync: Boolean = false,
    val enablePeppol: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * User identity - does not include organization info.
 * Users can belong to multiple organizations via OrganizationMembership.
 */
@Serializable
data class User(
    val id: UserId,
    val email: Email,
    val firstName: String? = null,
    val lastName: String? = null,
    val emailVerified: Boolean = false,
    val isActive: Boolean = true,
    val lastLoginAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Represents a user's membership in an organization with their role.
 */
@Serializable
data class OrganizationMembership(
    val userId: UserId,
    val organizationId: OrganizationId,
    val role: UserRole,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * User with specific organization context - used when working within an organization.
 */
@Serializable
data class UserInOrganization(
    val user: User,
    val organizationId: OrganizationId,
    val role: UserRole,
    val membershipActive: Boolean = true
)

// ============================================================================
// CLIENTS
// ============================================================================

@Serializable
data class ClientDto(
    val id: ClientId,
    val organizationId: OrganizationId,
    val name: String,
    val email: Email? = null,
    val vatNumber: VatNumber? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val contactPerson: String? = null,
    val phone: String? = null,
    val companyNumber: String? = null,
    val defaultPaymentTerms: Int = 30,
    val defaultVatRate: VatRate? = null,
    val peppolId: String? = null, // Peppol participant ID for e-invoicing (Belgium 2026)
    val peppolEnabled: Boolean = false,
    val tags: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

// NOTE: Invoice and Expense classes have been consolidated into FinancialDocumentDto
// Use InvoiceDto and ExpenseDto from FinancialDocument.kt

// ============================================================================
// PAYMENTS
// ============================================================================

@Serializable
data class PaymentDto(
    val id: PaymentId,
    val organizationId: OrganizationId,
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
    val organizationId: OrganizationId,
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
    val organizationId: OrganizationId,
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
// VAT RETURNS
// ============================================================================

@Serializable
data class VatReturnDto(
    val id: VatReturnId,
    val organizationId: OrganizationId,
    val quarter: Int,
    val year: Int,
    val salesVat: Money,
    val purchaseVat: Money,
    val netVat: Money,
    val status: VatReturnStatus,
    val filedAt: LocalDateTime? = null,
    val paidAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

// ============================================================================
// AUDIT & ATTACHMENTS
// ============================================================================

@Serializable
data class AuditLogDto(
    val id: AuditLogId,
    val organizationId: OrganizationId,
    val userId: BusinessUserId? = null,
    val action: AuditAction,
    val entityType: EntityType,
    val entityId: String, // Generic entity ID as string since it could be any entity
    val oldValues: Map<String, String>? = null,
    val newValues: Map<String, String>? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val createdAt: LocalDateTime
)

@Serializable
data class AttachmentDto(
    val id: AttachmentId,
    val organizationId: OrganizationId,
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

@Serializable
data class CreateInvoiceRequest(
    val clientId: ClientId,
    val items: List<InvoiceItemDto>,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val notes: String? = null
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
    val receiptUrl: String? = null,
    val receiptFilename: String? = null,
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