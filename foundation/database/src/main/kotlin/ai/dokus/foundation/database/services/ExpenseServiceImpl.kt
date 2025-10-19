package ai.dokus.foundation.database.services

import ai.dokus.foundation.database.mappers.ExpenseMapper.toExpense
import ai.dokus.foundation.database.tables.ExpensesTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.database.utils.toJavaLocalDate
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.Expense
import ai.dokus.foundation.ktor.services.ExpenseService
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class ExpenseServiceImpl(
    private val auditService: AuditServiceImpl
) : ExpenseService {
    private val logger = LoggerFactory.getLogger(ExpenseServiceImpl::class.java)

    override suspend fun create(request: CreateExpenseRequest): Expense {
        val expense = dbQuery {
            val expenseId = ExpensesTable.insertAndGetId {
                it[tenantId] = request.tenantId.value.toJavaUuid()
                it[date] = request.date
                it[merchant] = request.merchant
                it[amount] = BigDecimal(request.amount.value)
                it[vatAmount] = request.vatAmount?.let { BigDecimal(it.value) }
                it[vatRate] = request.vatRate?.let { BigDecimal(it.value) }
                it[category] = request.category
                it[description] = request.description
                it[paymentMethod] = request.paymentMethod
                it[isDeductible] = request.isDeductible ?: true
                it[deductiblePercentage] = request.deductiblePercentage?.let { BigDecimal(it.value) } ?: BigDecimal("100.00")
                it[isRecurring] = request.isRecurring ?: false
                it[notes] = request.notes
            }.value

            logger.info("Created expense $expenseId for tenant ${request.tenantId}")

            ExpensesTable.selectAll()
                .where { ExpensesTable.id eq expenseId }
                .single()
                .toExpense()
        }

        // Audit log
        auditService.logAction(
            tenantId = request.tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.ExpenseCreated,
            entityType = EntityType.Expense,
            entityId = expense.id.value,
            oldValues = null,
            newValues = mapOf(
                "merchant" to request.merchant,
                "amount" to request.amount.value,
                "category" to request.category.name,
                "date" to request.date.toString()
            )
        )

        return expense
    }

    override suspend fun update(
        expenseId: ExpenseId,
        date: LocalDate?,
        merchant: String?,
        amount: Money?,
        vatAmount: Money?,
        vatRate: VatRate?,
        category: ExpenseCategory?,
        description: String?,
        paymentMethod: PaymentMethod?,
        isDeductible: Boolean?,
        deductiblePercentage: Double?,
        isRecurring: Boolean?,
        notes: String?
    ) {
        val (tenantId, oldValues, newValues) = dbQuery {
            val javaUuid = expenseId.value.toJavaUuid()

            // Get existing expense data
            val expense = ExpensesTable.selectAll().where { ExpensesTable.id eq javaUuid }.singleOrNull()
                ?: throw IllegalArgumentException("Expense not found: $expenseId")

            // Capture old/new values
            val oldVals = mutableMapOf<String, Any?>()
            val newVals = mutableMapOf<String, Any?>()

            if (merchant != null) {
                oldVals["merchant"] = expense[ExpensesTable.merchant]
                newVals["merchant"] = merchant
            }
            if (amount != null) {
                oldVals["amount"] = expense[ExpensesTable.amount].toString()
                newVals["amount"] = amount.value
            }
            if (category != null) {
                oldVals["category"] = expense[ExpensesTable.category].name
                newVals["category"] = category.name
            }
            if (date != null) {
                oldVals["date"] = expense[ExpensesTable.date].toString()
                newVals["date"] = date.toString()
            }

            val updated = ExpensesTable.update({ ExpensesTable.id eq javaUuid }) {
                if (date != null) it[ExpensesTable.date] = date
                if (merchant != null) it[ExpensesTable.merchant] = merchant
                if (amount != null) it[ExpensesTable.amount] = BigDecimal(amount.value)
                if (vatAmount != null) it[ExpensesTable.vatAmount] = BigDecimal(vatAmount.value)
                if (vatRate != null) it[ExpensesTable.vatRate] = BigDecimal(vatRate.value)
                if (category != null) it[ExpensesTable.category] = category
                if (description != null) it[ExpensesTable.description] = description
                if (paymentMethod != null) it[ExpensesTable.paymentMethod] = paymentMethod
                if (isDeductible != null) it[ExpensesTable.isDeductible] = isDeductible
                if (deductiblePercentage != null) it[ExpensesTable.deductiblePercentage] = BigDecimal(deductiblePercentage.toString())
                if (isRecurring != null) it[ExpensesTable.isRecurring] = isRecurring
                if (notes != null) it[ExpensesTable.notes] = notes
            }

            if (updated == 0) {
                throw IllegalArgumentException("Expense not found: $expenseId")
            }

            Triple(TenantId(expense[ExpensesTable.tenantId].value.toKotlinUuid()), oldVals, newVals)
        }

        logger.info("Updated expense $expenseId")

        // Audit log
        auditService.logAction(
            tenantId = tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.ExpenseUpdated,
            entityType = EntityType.Expense,
            entityId = expenseId.value,
            oldValues = oldValues,
            newValues = newValues
        )
    }

    override suspend fun delete(expenseId: ExpenseId) {
        val (tenantId, merchant, amount) = dbQuery {
            val javaUuid = expenseId.value.toJavaUuid()

            // Get expense info before deletion
            val expense = ExpensesTable.selectAll().where { ExpensesTable.id eq javaUuid }.singleOrNull()
                ?: throw IllegalArgumentException("Expense not found: $expenseId")

            val deleted = ExpensesTable.deleteWhere { id eq javaUuid }

            if (deleted == 0) {
                throw IllegalArgumentException("Expense not found: $expenseId")
            }

            Triple(
                TenantId(expense[ExpensesTable.tenantId].value.toKotlinUuid()),
                expense[ExpensesTable.merchant],
                expense[ExpensesTable.amount].toString()
            )
        }

        logger.info("Deleted expense $expenseId")

        // Audit log
        auditService.logAction(
            tenantId = tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.ExpenseDeleted,
            entityType = EntityType.Expense,
            entityId = expenseId.value,
            oldValues = mapOf("merchant" to merchant, "amount" to amount),
            newValues = null
        )
    }

    override suspend fun findById(id: ExpenseId): Expense? = dbQuery {
        val javaUuid = id.value.toJavaUuid()
        ExpensesTable.selectAll()
            .where { ExpensesTable.id eq javaUuid }
            .singleOrNull()
            ?.toExpense()
    }

    override suspend fun listByTenant(
        tenantId: TenantId,
        category: ExpenseCategory?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        merchant: String?,
        limit: Int?,
        offset: Int?
    ): List<Expense> = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        var query = ExpensesTable.selectAll().where { ExpensesTable.tenantId eq javaUuid }

        if (category != null) query = query.andWhere { ExpensesTable.category eq category }
        if (fromDate != null) query = query.andWhere { ExpensesTable.date greaterEq fromDate }
        if (toDate != null) query = query.andWhere { ExpensesTable.date lessEq toDate }
        if (merchant != null) query = query.andWhere { ExpensesTable.merchant.lowerCase() like "%${merchant.lowercase()}%" }
        if (limit != null) query = query.limit(limit)
        if (offset != null) query = query.offset(offset.toLong())

        query.orderBy(ExpensesTable.date to SortOrder.DESC)
            .map { it.toExpense() }
    }

    override suspend fun uploadReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): String {
        val (tenantId, receiptUrl) = dbQuery {
            logger.info("Uploading receipt for expense $expenseId: $filename")

            // Verify expense exists
            val expense = ExpensesTable.selectAll()
                .where { ExpensesTable.id eq expenseId.value.toJavaUuid() }
                .singleOrNull()
                ?: throw IllegalArgumentException("Expense not found: $expenseId")

            val tid = expense[ExpensesTable.tenantId]

            // Generate unique file path
            val sanitizedFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fileExtension = sanitizedFilename.substringAfterLast(".", "")
            val fileKey = "receipts/$tid/${expenseId.value}/${System.currentTimeMillis()}_$sanitizedFilename"

            // TODO: Integrate with S3/MinIO object storage
            // Example S3 integration:
            // val s3Client = S3Client.builder()
            //     .region(Region.EU_WEST_1)
            //     .build()
            //
            // val putObjectRequest = PutObjectRequest.builder()
            //     .bucket("dokus-receipts")
            //     .key(fileKey)
            //     .contentType(contentType)
            //     .build()
            //
            // s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent))
            //
            // val receiptUrl = "https://dokus-receipts.s3.eu-west-1.amazonaws.com/$fileKey"

            // For now, generate a placeholder URL
            val url = "https://storage.dokus.ai/$fileKey"

            // Update expense record with receipt URL
            ExpensesTable.update({ ExpensesTable.id eq expenseId.value.toJavaUuid() }) {
                it[ExpensesTable.receiptUrl] = url
                it[ExpensesTable.receiptFilename] = sanitizedFilename
            }

            logger.info("Receipt uploaded for expense $expenseId: $url (${fileContent.size} bytes)")
            logger.warn("S3/MinIO integration not yet implemented - file not actually stored")

            Pair(TenantId(tid.value.toKotlinUuid()), url)
        }

        // Audit log
        auditService.logAction(
            tenantId = tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.ExpenseUpdated,
            entityType = EntityType.Expense,
            entityId = expenseId.value,
            oldValues = mapOf("receiptUrl" to null),
            newValues = mapOf("receiptUrl" to receiptUrl, "filename" to filename)
        )

        return receiptUrl
    }

    override suspend fun downloadReceipt(expenseId: ExpenseId): ByteArray? = dbQuery {
        logger.info("Downloading receipt for expense $expenseId")

        // Get expense with receipt URL
        val expense = ExpensesTable.selectAll()
            .where { ExpensesTable.id eq expenseId.value.toJavaUuid() }
            .singleOrNull()
            ?: throw IllegalArgumentException("Expense not found: $expenseId")

        val receiptUrl = expense[ExpensesTable.receiptUrl]
            ?: return@dbQuery null

        // Extract file key from URL
        val fileKey = receiptUrl.substringAfter("storage.dokus.ai/")

        // TODO: Integrate with S3/MinIO object storage
        // Example S3 integration:
        // val s3Client = S3Client.builder()
        //     .region(Region.EU_WEST_1)
        //     .build()
        //
        // val getObjectRequest = GetObjectRequest.builder()
        //     .bucket("dokus-receipts")
        //     .key(fileKey)
        //     .build()
        //
        // val responseBytes = s3Client.getObject(getObjectRequest)
        //     .use { response -> response.readBytes() }
        //
        // return responseBytes

        logger.warn("S3/MinIO integration not yet implemented - cannot download actual file")

        // Return placeholder data
        "Receipt file content would be here for expense $expenseId".toByteArray(Charsets.UTF_8)
    }

    override suspend fun deleteReceipt(expenseId: ExpenseId) {
        val (tenantId, oldReceiptUrl) = dbQuery {
            val javaUuid = expenseId.value.toJavaUuid()

            // Get expense info before deletion
            val expense = ExpensesTable.selectAll().where { ExpensesTable.id eq javaUuid }.singleOrNull()
                ?: throw IllegalArgumentException("Expense not found: $expenseId")

            val oldUrl = expense[ExpensesTable.receiptUrl]

            ExpensesTable.update({ ExpensesTable.id eq javaUuid }) {
                it[receiptUrl] = null
                it[receiptFilename] = null
            }

            Pair(TenantId(expense[ExpensesTable.tenantId].value.toKotlinUuid()), oldUrl)
        }

        logger.info("Deleted receipt for expense $expenseId")

        // Audit log
        auditService.logAction(
            tenantId = tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.ExpenseUpdated,
            entityType = EntityType.Expense,
            entityId = expenseId.value,
            oldValues = mapOf("receiptUrl" to oldReceiptUrl),
            newValues = mapOf("receiptUrl" to null)
        )
    }

    override suspend fun categorize(merchant: String, description: String?): ExpenseCategory {
        return when {
            merchant.contains("aws", ignoreCase = true) || merchant.contains("cloud", ignoreCase = true) -> ExpenseCategory.Software
            merchant.contains("uber", ignoreCase = true) || merchant.contains("taxi", ignoreCase = true) -> ExpenseCategory.Travel
            merchant.contains("hotel", ignoreCase = true) -> ExpenseCategory.Travel
            merchant.contains("restaurant", ignoreCase = true) || merchant.contains("coffee", ignoreCase = true) -> ExpenseCategory.Meals
            merchant.contains("office", ignoreCase = true) -> ExpenseCategory.OfficeSupplies
            else -> ExpenseCategory.Other
        }
    }

    override suspend fun listRecurring(tenantId: TenantId): List<Expense> = dbQuery {
        ExpensesTable.selectAll()
            .where { ExpensesTable.tenantId eq tenantId.value.toJavaUuid() }
            .andWhere { ExpensesTable.isRecurring eq true }
            .map { it.toExpense() }
    }

    override fun watchExpenses(tenantId: TenantId): Flow<Expense> {
        return kotlinx.coroutines.flow.flow {
            var lastSeenExpenses = emptyMap<ExpenseId, Expense>()

            while (true) {
                // Poll for expense changes every 5 seconds
                kotlinx.coroutines.delay(5000)

                try {
                    val currentExpenses = listByTenant(
                        tenantId = tenantId,
                        category = null,
                        fromDate = null,
                        toDate = null,
                        merchant = null,
                        limit = null,
                        offset = null
                    )
                    val currentMap = currentExpenses.associateBy { it.id }

                    // Emit new or updated expenses
                    currentExpenses.forEach { expense ->
                        val previous = lastSeenExpenses[expense.id]
                        if (previous == null || previous != expense) {
                            // New or updated expense
                            emit(expense)
                        }
                    }

                    lastSeenExpenses = currentMap
                } catch (e: Exception) {
                    // Log error but continue polling
                    logger.error("Error polling expenses for tenant $tenantId", e)
                }
            }
        }
    }

    override suspend fun getStatistics(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Map<String, Any> = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        var query = ExpensesTable.selectAll().where { ExpensesTable.tenantId eq javaUuid }

        // Apply date filters if provided
        if (fromDate != null) query = query.andWhere { ExpensesTable.date greaterEq fromDate }
        if (toDate != null) query = query.andWhere { ExpensesTable.date lessEq toDate }

        val expenses = query.toList()

        // Calculate statistics
        var totalExpenses = BigDecimal.ZERO
        var totalDeductible = BigDecimal.ZERO
        var totalVat = BigDecimal.ZERO
        val expensesByCategory = mutableMapOf<String, BigDecimal>()
        var expenseCount = 0

        expenses.forEach { row ->
            val amount = row[ExpensesTable.amount]
            val vatAmount = row[ExpensesTable.vatAmount] ?: BigDecimal.ZERO
            val deductiblePercentage = row[ExpensesTable.deductiblePercentage]
            val category = row[ExpensesTable.category]

            totalExpenses += amount
            totalVat += vatAmount

            // Calculate deductible amount
            val deductibleAmount = amount * deductiblePercentage / BigDecimal("100")
            totalDeductible += deductibleAmount

            // Group by category
            expensesByCategory[category.name] =
                expensesByCategory.getOrDefault(category.name, BigDecimal.ZERO) + amount

            expenseCount++
        }

        // Convert category map to Money values
        val categoryStats = expensesByCategory.mapValues { Money(it.value.toString()) }

        mapOf(
            "totalExpenses" to Money(totalExpenses.toString()),
            "totalDeductible" to Money(totalDeductible.toString()),
            "totalVat" to Money(totalVat.toString()),
            "expenseCount" to expenseCount,
            "expensesByCategory" to categoryStats
        )
    }

    override suspend fun calculateDeductible(amount: Money, deductiblePercentage: Double): Money {
        val deductible = BigDecimal(amount.value) * BigDecimal(deductiblePercentage.toString()) / BigDecimal("100")
        return Money(deductible.toString())
    }
}
