package ai.dokus.foundation.database.mappers

import ai.dokus.foundation.database.tables.*
import ai.dokus.foundation.database.utils.toKotlinLocalDate
import ai.dokus.foundation.database.utils.toKotlinLocalDateTime
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.model.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object ExpenseMapper {

    fun ResultRow.toExpense(): Expense = Expense(
        id = ExpenseId(this[ExpensesTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[ExpensesTable.tenantId].value.toKotlinUuid()),
        date = this[ExpensesTable.date].toKotlinLocalDate(),
        merchant = this[ExpensesTable.merchant],
        amount = Money(this[ExpensesTable.amount].toString()),
        vatAmount = this[ExpensesTable.vatAmount]?.toString()?.let { Money(it) },
        vatRate = this[ExpensesTable.vatRate]?.toString()?.let { VatRate(it) },
        category = this[ExpensesTable.category],
        description = this[ExpensesTable.description],
        receiptUrl = this[ExpensesTable.receiptUrl],
        receiptFilename = this[ExpensesTable.receiptFilename],
        isDeductible = this[ExpensesTable.isDeductible],
        deductiblePercentage = Percentage(this[ExpensesTable.deductiblePercentage].toString()),
        paymentMethod = this[ExpensesTable.paymentMethod],
        isRecurring = this[ExpensesTable.isRecurring],
        notes = this[ExpensesTable.notes],
        createdAt = this[ExpensesTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[ExpensesTable.updatedAt].toKotlinLocalDateTime()
    )
}

@OptIn(ExperimentalUuidApi::class)
object PaymentMapper {

    fun ResultRow.toPayment(): Payment = Payment(
        id = PaymentId(this[PaymentsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[PaymentsTable.tenantId].value.toKotlinUuid()),
        invoiceId = InvoiceId(this[PaymentsTable.invoiceId].value.toKotlinUuid()),
        amount = Money(this[PaymentsTable.amount].toString()),
        paymentDate = this[PaymentsTable.paymentDate].toKotlinLocalDate(),
        paymentMethod = this[PaymentsTable.paymentMethod],
        transactionId = this[PaymentsTable.transactionId]?.let { TransactionId(it) },
        notes = this[PaymentsTable.notes],
        createdAt = this[PaymentsTable.createdAt].toKotlinLocalDateTime()
    )
}

@OptIn(ExperimentalUuidApi::class)
object BankingMapper {

    fun ResultRow.toBankConnection(): BankConnection = BankConnection(
        id = BankConnectionId(this[BankConnectionsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[BankConnectionsTable.tenantId].value.toKotlinUuid()),
        provider = this[BankConnectionsTable.provider],
        institutionId = this[BankConnectionsTable.institutionId],
        institutionName = this[BankConnectionsTable.institutionName],
        accountId = this[BankConnectionsTable.accountId],
        accountName = this[BankConnectionsTable.accountName],
        accountType = this[BankConnectionsTable.accountType],
        currency = this[BankConnectionsTable.currency],
        lastSyncedAt = this[BankConnectionsTable.lastSyncedAt]?.toKotlinLocalDateTime(),
        isActive = this[BankConnectionsTable.isActive],
        createdAt = this[BankConnectionsTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[BankConnectionsTable.updatedAt].toKotlinLocalDateTime()
    )

    fun ResultRow.toBankTransaction(): BankTransaction = BankTransaction(
        id = BankTransactionId(this[BankTransactionsTable.id].value.toKotlinUuid()),
        bankConnectionId = BankConnectionId(this[BankTransactionsTable.bankConnectionId].value.toKotlinUuid()),
        tenantId = TenantId(this[BankTransactionsTable.tenantId].value.toKotlinUuid()),
        externalId = this[BankTransactionsTable.externalId],
        date = this[BankTransactionsTable.date].toKotlinLocalDate(),
        amount = Money(this[BankTransactionsTable.amount].toString()),
        description = this[BankTransactionsTable.description],
        merchantName = this[BankTransactionsTable.merchantName],
        category = this[BankTransactionsTable.category],
        isPending = this[BankTransactionsTable.isPending],
        expenseId = this[BankTransactionsTable.expenseId]?.value?.toKotlinUuid()?.let { ExpenseId(it) },
        invoiceId = this[BankTransactionsTable.invoiceId]?.value?.toKotlinUuid()?.let { InvoiceId(it) },
        isReconciled = this[BankTransactionsTable.isReconciled],
        createdAt = this[BankTransactionsTable.createdAt].toKotlinLocalDateTime()
    )
}

@OptIn(ExperimentalUuidApi::class)
object VatMapper {

    fun ResultRow.toVatReturn(): VatReturn = VatReturn(
        id = VatReturnId(this[VatReturnsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[VatReturnsTable.tenantId].value.toKotlinUuid()),
        quarter = this[VatReturnsTable.quarter],
        year = this[VatReturnsTable.year],
        salesVat = Money(this[VatReturnsTable.salesVat].toString()),
        purchaseVat = Money(this[VatReturnsTable.purchaseVat].toString()),
        netVat = Money(this[VatReturnsTable.netVat].toString()),
        status = this[VatReturnsTable.status],
        filedAt = this[VatReturnsTable.filedAt]?.toKotlinLocalDateTime(),
        paidAt = this[VatReturnsTable.paidAt]?.toKotlinLocalDateTime(),
        createdAt = this[VatReturnsTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[VatReturnsTable.updatedAt].toKotlinLocalDateTime()
    )
}

@OptIn(ExperimentalUuidApi::class)
object AuditMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun ResultRow.toAuditLog(): AuditLog = AuditLog(
        id = AuditLogId(this[AuditLogsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[AuditLogsTable.tenantId].value.toKotlinUuid()),
        userId = this[AuditLogsTable.userId]?.value?.toKotlinUuid()?.let { BusinessUserId(it) },
        action = this[AuditLogsTable.action],
        entityType = this[AuditLogsTable.entityType],
        entityId = this[AuditLogsTable.entityId].toString(), // Generic entity ID as string
        oldValues = this[AuditLogsTable.oldValues]?.let {
            json.decodeFromString<Map<String, String>>(it)
        },
        newValues = this[AuditLogsTable.newValues]?.let {
            json.decodeFromString<Map<String, String>>(it)
        },
        ipAddress = this[AuditLogsTable.ipAddress],
        userAgent = this[AuditLogsTable.userAgent],
        createdAt = this[AuditLogsTable.createdAt].toKotlinLocalDateTime()
    )
}

@OptIn(ExperimentalUuidApi::class)
object AttachmentMapper {

    fun ResultRow.toAttachment(): Attachment = Attachment(
        id = AttachmentId(this[AttachmentsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[AttachmentsTable.tenantId].value.toKotlinUuid()),
        entityType = this[AttachmentsTable.entityType],
        entityId = this[AttachmentsTable.entityId].toString(), // Generic entity ID as string
        filename = this[AttachmentsTable.filename],
        mimeType = this[AttachmentsTable.mimeType],
        sizeBytes = this[AttachmentsTable.sizeBytes],
        s3Key = this[AttachmentsTable.s3Key],
        s3Bucket = this[AttachmentsTable.s3Bucket],
        uploadedAt = this[AttachmentsTable.uploadedAt].toKotlinLocalDateTime()
    )
}

@OptIn(ExperimentalUuidApi::class)
object UserMapper {

    fun ResultRow.toBusinessUser(): BusinessUser = BusinessUser(
        id = BusinessUserId(this[UsersTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[UsersTable.tenantId].value.toKotlinUuid()),
        email = Email(this[UsersTable.email]),
        role = this[UsersTable.role],
        firstName = this[UsersTable.firstName],
        lastName = this[UsersTable.lastName],
        isActive = this[UsersTable.isActive],
        lastLoginAt = this[UsersTable.lastLoginAt]?.toKotlinLocalDateTime(),
        createdAt = this[UsersTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[UsersTable.updatedAt].toKotlinLocalDateTime()
    )
}