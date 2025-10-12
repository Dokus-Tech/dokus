package ai.dokus.foundation.domain.model

import ai.dokus.foundation.database.enums.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

// ============================================================================
// TENANT & USER MANAGEMENT
// ============================================================================

@Serializable
data class Tenant(
    val id: String,
    val name: String,
    val email: String,
    val plan: TenantPlan,
    val status: TenantStatus,
    val country: String,
    val language: Language,
    val vatNumber: String? = null,
    val trialEndsAt: LocalDateTime? = null,
    val subscriptionStartedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class TenantSettings(
    val tenantId: String,
    val invoicePrefix: String = "INV",
    val nextInvoiceNumber: Int = 1,
    val defaultPaymentTerms: Int = 30,
    val defaultVatRate: String = "21.00",
    val companyName: String? = null,
    val companyAddress: String? = null,
    val companyVatNumber: String? = null,
    val companyIban: String? = null,
    val companyBic: String? = null,
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
    val id: String,
    val tenantId: String,
    val email: String,
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
    val id: String,
    val tenantId: String,
    val name: String,
    val email: String? = null,
    val vatNumber: String? = null,
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
    val id: String,
    val tenantId: String,
    val clientId: String,
    val invoiceNumber: String,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val subtotalAmount: String,
    val vatAmount: String,
    val totalAmount: String,
    val paidAmount: String = "0.00",
    val status: InvoiceStatus,
    val currency: Currency = Currency.EUR,
    val notes: String? = null,
    val termsAndConditions: String? = null,
    val items: List<InvoiceItem> = emptyList(),
    val peppolId: String? = null,
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
    val id: String? = null,
    val invoiceId: String? = null,
    val description: String,
    val quantity: String,
    val unitPrice: String,
    val vatRate: String,
    val lineTotal: String,
    val vatAmount: String,
    val sortOrder: Int = 0
)

// ============================================================================
// EXPENSES
// ============================================================================

@Serializable
data class Expense(
    val id: String,
    val tenantId: String,
    val date: LocalDate,
    val merchant: String,
    val amount: String,
    val vatAmount: String? = null,
    val vatRate: String? = null,
    val category: ExpenseCategory,
    val description: String? = null,
    val receiptUrl: String? = null,
    val receiptFilename: String? = null,
    val isDeductible: Boolean = true,
    val deductiblePercentage: String = "100.00",
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
    val id: String,
    val tenantId: String,
    val invoiceId: String,
    val amount: String,
    val paymentDate: LocalDate,
    val paymentMethod: PaymentMethod,
    val transactionId: String? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime
)

// ============================================================================
// BANKING
// ============================================================================

@Serializable
data class BankConnection(
    val id: String,
    val tenantId: String,
    val provider: BankProvider,
    val institutionId: String,
    val institutionName: String,
    val accountId: String,
    val accountName: String? = null,
    val accountType: BankAccountType? = null,
    val currency: Currency = Currency.EUR,
    val lastSyncedAt: LocalDateTime? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class BankTransaction(
    val id: String,
    val bankConnectionId: String,
    val tenantId: String,
    val externalId: String,
    val date: LocalDate,
    val amount: String,
    val description: String,
    val merchantName: String? = null,
    val category: String? = null,
    val isPending: Boolean = false,
    val expenseId: String? = null,
    val invoiceId: String? = null,
    val isReconciled: Boolean = false,
    val createdAt: LocalDateTime
)

// ============================================================================
// VAT RETURNS
// ============================================================================

@Serializable
data class VatReturn(
    val id: String,
    val tenantId: String,
    val quarter: Int,
    val year: Int,
    val salesVat: String,
    val purchaseVat: String,
    val netVat: String,
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
    val id: String,
    val tenantId: String,
    val userId: String? = null,
    val action: AuditAction,
    val entityType: EntityType,
    val entityId: String,
    val oldValues: Map<String, String>? = null,
    val newValues: Map<String, String>? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val createdAt: LocalDateTime
)

@Serializable
data class Attachment(
    val id: String,
    val tenantId: String,
    val entityType: EntityType,
    val entityId: String,
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
    val clientId: String,
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
    val invoiceId: String,
    val amount: String,
    val paymentDate: LocalDate,
    val paymentMethod: PaymentMethod,
    val transactionId: String? = null,
    val notes: String? = null
)

@Serializable
data class CreateExpenseRequest(
    val date: LocalDate,
    val merchant: String,
    val amount: String,
    val vatAmount: String? = null,
    val vatRate: String? = null,
    val category: ExpenseCategory,
    val description: String? = null,
    val receiptUrl: String? = null,
    val receiptFilename: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val notes: String? = null
)