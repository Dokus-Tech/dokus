package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

// ============================================================================
// TENANT & USER MANAGEMENT
// ============================================================================

@Serializable
data class Tenant(
    val id: TenantId,
    val name: String,
    val email: String,
    val plan: TenantPlan,
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
data class TenantSettings(
    val tenantId: TenantId,
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

@Serializable
data class BusinessUser(
    val id: BusinessUserId,
    val tenantId: TenantId,
    val email: Email,
    val role: UserRole,
    val firstName: String? = null,
    val lastName: String? = null,
    val isActive: Boolean = true,
    val lastLoginAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

// ============================================================================
// INVOICING
// ============================================================================

@Serializable
data class Client(
    val id: ClientId,
    val tenantId: TenantId,
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
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class Invoice(
    val id: InvoiceId,
    val tenantId: TenantId,
    val clientId: ClientId,
    val invoiceNumber: InvoiceNumber,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val subtotalAmount: Money,
    val vatAmount: Money,
    val totalAmount: Money,
    val paidAmount: Money = Money.ZERO,
    val status: InvoiceStatus,
    val currency: Currency = Currency.Eur,
    val notes: String? = null,
    val termsAndConditions: String? = null,
    val items: List<InvoiceItem> = emptyList(),
    val peppolId: PeppolId? = null,
    val peppolSentAt: LocalDateTime? = null,
    val peppolStatus: PeppolStatus? = null,
    val paymentLink: String? = null,
    val paymentLinkExpiresAt: LocalDateTime? = null,
    val paidAt: LocalDateTime? = null,
    val paymentMethod: PaymentMethod? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class InvoiceItem(
    val id: InvoiceItemId? = null,
    val invoiceId: InvoiceId? = null,
    val description: String,
    val quantity: Quantity,
    val unitPrice: Money,
    val vatRate: VatRate,
    val lineTotal: Money,
    val vatAmount: Money,
    val sortOrder: Int = 0
)

// ============================================================================
// EXPENSES
// ============================================================================

@Serializable
data class Expense(
    val id: ExpenseId,
    val tenantId: TenantId,
    val date: LocalDate,
    val merchant: String,
    val amount: Money,
    val vatAmount: Money? = null,
    val vatRate: VatRate? = null,
    val category: ExpenseCategory,
    val description: String? = null,
    val receiptUrl: String? = null,
    val receiptFilename: String? = null,
    val isDeductible: Boolean = true,
    val deductiblePercentage: Percentage = Percentage.FULL,
    val paymentMethod: PaymentMethod? = null,
    val isRecurring: Boolean = false,
    val notes: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

// ============================================================================
// PAYMENTS
// ============================================================================

@Serializable
data class Payment(
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
data class BankConnection(
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
data class BankTransaction(
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
// VAT RETURNS
// ============================================================================

@Serializable
data class VatReturn(
    val id: VatReturnId,
    val tenantId: TenantId,
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
data class AuditLog(
    val id: AuditLogId,
    val tenantId: TenantId,
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
data class Attachment(
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

@Serializable
data class CreateInvoiceRequest(
    val clientId: ClientId,
    val items: List<InvoiceItem>,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val notes: String? = null
)

@Serializable
data class UpdateInvoiceStatusRequest(
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
    val paymentMethod: PaymentMethod? = null,
    val notes: String? = null
)