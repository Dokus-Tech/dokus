package ai.dokus.foundation.database.mappers

import ai.dokus.foundation.database.tables.*
import ai.dokus.foundation.domain.model.*
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object ExpenseMapper {

    fun ResultRow.toExpense(): Expense = Expense(
        id = this[ExpensesTable.id].value.toKotlinUuid(),
        tenantId = this[ExpensesTable.tenantId].value.toKotlinUuid(),
        date = this[ExpensesTable.date].toKotlinLocalDate(),
        merchant = this[ExpensesTable.merchant],
        amount = this[ExpensesTable.amount].toString(),
        vatAmount = this[ExpensesTable.vatAmount]?.toString(),
        vatRate = this[ExpensesTable.vatRate]?.toString(),
        category = this[ExpensesTable.category],
        description = this[ExpensesTable.description],
        receiptUrl = this[ExpensesTable.receiptUrl],
        receiptFilename = this[ExpensesTable.receiptFilename],
        isDeductible = this[ExpensesTable.isDeductible],
        deductiblePercentage = this[ExpensesTable.deductiblePercentage].toString(),
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
        id = this[PaymentsTable.id].value.toKotlinUuid(),
        tenantId = this[PaymentsTable.tenantId].value.toKotlinUuid(),
        invoiceId = this[PaymentsTable.invoiceId].value.toKotlinUuid(),
        amount = this[PaymentsTable.amount].toString(),
        paymentDate = this[PaymentsTable.paymentDate].toKotlinLocalDate(),
        paymentMethod = this[PaymentsTable.paymentMethod],
        transactionId = this[PaymentsTable.transactionId],
        notes = this[PaymentsTable.notes],
        createdAt = this[PaymentsTable.createdAt].toKotlinLocalDateTime()
    )
}

@OptIn(ExperimentalUuidApi::class)
object BankingMapper {

    fun ResultRow.toBankConnection(): BankConnection = BankConnection(
        id = this[BankConnectionsTable.id].value.toKotlinUuid(),
        tenantId = this[BankConnectionsTable.tenantId].value.toKotlinUuid(),
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
        id = this[BankTransactionsTable.id].value.toKotlinUuid(),
        bankConnectionId = this[BankTransactionsTable.bankConnectionId].value.toKotlinUuid(),
        tenantId = this[BankTransactionsTable.tenantId].value.toKotlinUuid(),
        externalId = this[BankTransactionsTable.externalId],
        date = this[BankTransactionsTable.date].toKotlinLocalDate(),
        amount = this[BankTransactionsTable.amount].toString(),
        description = this[BankTransactionsTable.description],
        merchantName = this[BankTransactionsTable.merchantName],
        category = this[BankTransactionsTable.category],
        isPending = this[BankTransactionsTable.isPending],
        expenseId = this[BankTransactionsTable.expenseId]?.value?.toKotlinUuid(),
        invoiceId = this[BankTransactionsTable.invoiceId]?.value?.toKotlinUuid(),
        isReconciled = this[BankTransactionsTable.isReconciled],
        createdAt = this[BankTransactionsTable.createdAt].toKotlinLocalDateTime()
    )
}

@OptIn(ExperimentalUuidApi::class)
object VatMapper {

    fun ResultRow.toVatReturn(): VatReturn = VatReturn(
        id = this[VatReturnsTable.id].value.toKotlinUuid(),
        tenantId = this[VatReturnsTable.tenantId].value.toKotlinUuid(),
        quarter = this[VatReturnsTable.quarter],
        year = this[VatReturnsTable.year],
        salesVat = this[VatReturnsTable.salesVat].toString(),
        purchaseVat = this[VatReturnsTable.purchaseVat].toString(),
        netVat = this[VatReturnsTable.netVat].toString(),
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
        id = this[AuditLogsTable.id].value.toKotlinUuid(),
        tenantId = this[AuditLogsTable.tenantId].value.toKotlinUuid(),
        userId = this[AuditLogsTable.userId]?.value?.toKotlinUuid(),
        action = this[AuditLogsTable.action],
        entityType = this[AuditLogsTable.entityType],
        entityId = this[AuditLogsTable.entityId].toKotlinUuid(),
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
        id = this[AttachmentsTable.id].value.toKotlinUuid(),
        tenantId = this[AttachmentsTable.tenantId].value.toKotlinUuid(),
        entityType = this[AttachmentsTable.entityType],
        entityId = this[AttachmentsTable.entityId].toKotlinUuid(),
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
        id = this[UsersTable.id].value.toKotlinUuid(),
        tenantId = this[UsersTable.tenantId].value.toKotlinUuid(),
        email = this[UsersTable.email],
        role = this[UsersTable.role],
        firstName = this[UsersTable.firstName],
        lastName = this[UsersTable.lastName],
        isActive = this[UsersTable.isActive],
        lastLoginAt = this[UsersTable.lastLoginAt]?.toKotlinLocalDateTime(),
        createdAt = this[UsersTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[UsersTable.updatedAt].toKotlinLocalDateTime()
    )
}