@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.dokus.foundation.database.services

import ai.dokus.foundation.database.mappers.PaymentMapper.toPayment
import ai.dokus.foundation.database.tables.InvoicesTable
import ai.dokus.foundation.database.tables.PaymentsTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.database.utils.toJavaLocalDate
import ai.dokus.foundation.database.utils.toJavaInstant
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.Payment
import ai.dokus.foundation.ktor.services.PaymentService
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class PaymentServiceImpl(
    private val auditService: AuditServiceImpl
) : PaymentService {
    private val logger = LoggerFactory.getLogger(PaymentServiceImpl::class.java)

    override suspend fun recordPayment(
        tenantId: TenantId,
        invoiceId: InvoiceId,
        amount: Money,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        notes: String?
    ): Payment {
        val payment = dbQuery {
            // Verify invoice exists and belongs to tenant
            val invoice = InvoicesTable.selectAll()
                .where { InvoicesTable.id eq invoiceId.value.toJavaUuid() }
                .andWhere { InvoicesTable.tenantId eq tenantId.value.toJavaUuid() }
                .singleOrNull()
                ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

            // Create payment record
            val paymentId = PaymentsTable.insertAndGetId {
                it[PaymentsTable.tenantId] = tenantId.value.toJavaUuid()
                it[PaymentsTable.invoiceId] = invoiceId.value.toJavaUuid()
                it[PaymentsTable.amount] = BigDecimal(amount.value)
                it[PaymentsTable.paymentDate] = paymentDate
                it[PaymentsTable.paymentMethod] = paymentMethod
                it[PaymentsTable.transactionId] = transactionId
                it[PaymentsTable.notes] = notes
            }.value

            // Update invoice paid amount
            val currentPaid = invoice[InvoicesTable.paidAmount]
            val newPaid = currentPaid + BigDecimal(amount.value)
            val total = invoice[InvoicesTable.totalAmount]

            InvoicesTable.update({ InvoicesTable.id eq invoiceId.value.toJavaUuid() }) {
                it[paidAmount] = newPaid
                if (newPaid >= total) {
                    it[status] = InvoiceStatus.Paid
                    it[paidAt] = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
                }
            }

            logger.info("Recorded payment of $amount for invoice $invoiceId")

            PaymentsTable.selectAll()
                .where { PaymentsTable.id eq paymentId }
                .single()
                .toPayment()
        }

        // Audit log
        auditService.logAction(
            tenantId = tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.PaymentRecorded,
            entityType = EntityType.Payment,
            entityId = payment.id.value,
            oldValues = null,
            newValues = mapOf(
                "invoiceId" to invoiceId.value.toString(),
                "amount" to amount.value,
                "paymentMethod" to paymentMethod.name,
                "paymentDate" to paymentDate.toString()
            )
        )

        return payment
    }

    override suspend fun findById(id: PaymentId): Payment? = dbQuery {
        PaymentsTable.selectAll()
            .where { PaymentsTable.id eq id.value.toJavaUuid() }
            .singleOrNull()
            ?.toPayment()
    }

    override suspend fun listByInvoice(invoiceId: InvoiceId): List<Payment> = dbQuery {
        PaymentsTable.selectAll()
            .where { PaymentsTable.invoiceId eq invoiceId.value.toJavaUuid() }
            .orderBy(PaymentsTable.paymentDate to SortOrder.DESC)
            .map { it.toPayment() }
    }

    override suspend fun listByTenant(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        paymentMethod: PaymentMethod?,
        limit: Int?,
        offset: Int?
    ): List<Payment> = dbQuery {
        var query = PaymentsTable.selectAll().where { PaymentsTable.tenantId eq tenantId.value.toJavaUuid() }

        if (fromDate != null) query = query.andWhere { PaymentsTable.paymentDate greaterEq fromDate }
        if (toDate != null) query = query.andWhere { PaymentsTable.paymentDate lessEq toDate }
        if (paymentMethod != null) query = query.andWhere { PaymentsTable.paymentMethod eq paymentMethod }
        if (limit != null) query = query.limit(limit)
        if (offset != null) query = query.offset(offset.toLong())

        query.orderBy(PaymentsTable.paymentDate to SortOrder.DESC)
            .map { it.toPayment() }
    }

    override suspend fun delete(paymentId: PaymentId) {
        data class DeletedPaymentInfo(
            val tenantId: TenantId,
            val invoiceId: InvoiceId,
            val amount: Money,
            val paymentMethod: PaymentMethod
        )

        val paymentInfo = dbQuery {
            val payment = PaymentsTable.selectAll()
                .where { PaymentsTable.id eq paymentId.value.toJavaUuid() }
                .singleOrNull()
                ?.toPayment()
                ?: throw IllegalArgumentException("Payment not found: $paymentId")

            // Update invoice paid amount
            val invoice = InvoicesTable.selectAll()
                .where { InvoicesTable.id eq payment.invoiceId.value.toJavaUuid() }
                .single()

            val newPaid = invoice[InvoicesTable.paidAmount] - BigDecimal(payment.amount.value)

            InvoicesTable.update({ InvoicesTable.id eq payment.invoiceId.value.toJavaUuid() }) {
                it[paidAmount] = newPaid
                if (newPaid < invoice[InvoicesTable.totalAmount]) {
                    it[status] = InvoiceStatus.Sent
                    it[paidAt] = null
                }
            }

            // Delete payment
            PaymentsTable.deleteWhere { id eq paymentId.value.toJavaUuid() }

            logger.info("Deleted payment $paymentId and updated invoice ${payment.invoiceId}")

            DeletedPaymentInfo(payment.tenantId, payment.invoiceId, payment.amount, payment.paymentMethod)
        }

        // Audit log
        auditService.logAction(
            tenantId = paymentInfo.tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.PaymentDeleted,
            entityType = EntityType.Payment,
            entityId = paymentId.value,
            oldValues = mapOf(
                "invoiceId" to paymentInfo.invoiceId.value.toString(),
                "amount" to paymentInfo.amount.value,
                "paymentMethod" to paymentInfo.paymentMethod.name
            ),
            newValues = null
        )
    }

    override suspend fun reconcile(paymentId: PaymentId, transactionId: String) = dbQuery {
        val updated = PaymentsTable.update({ PaymentsTable.id eq paymentId.value.toJavaUuid() }) {
            it[PaymentsTable.transactionId] = transactionId
        }

        if (updated == 0) {
            throw IllegalArgumentException("Payment not found: $paymentId")
        }

        logger.info("Reconciled payment $paymentId with transaction $transactionId")
    }

    override suspend fun getStatistics(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Map<String, Any> = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        var query = PaymentsTable.selectAll().where { PaymentsTable.tenantId eq javaUuid }

        // Apply date filters if provided
        if (fromDate != null) query = query.andWhere { PaymentsTable.paymentDate greaterEq fromDate }
        if (toDate != null) query = query.andWhere { PaymentsTable.paymentDate lessEq toDate }

        val payments = query.toList()

        // Calculate statistics
        var totalPayments = BigDecimal.ZERO
        var paymentCount = 0
        val paymentsByMethod = mutableMapOf<String, BigDecimal>()
        val paymentCountByMethod = mutableMapOf<String, Int>()

        payments.forEach { row ->
            val amount = row[PaymentsTable.amount]
            val paymentMethod = row[PaymentsTable.paymentMethod]

            totalPayments += amount
            paymentCount++

            // Group by payment method
            val methodName = paymentMethod.name
            paymentsByMethod[methodName] =
                paymentsByMethod.getOrDefault(methodName, BigDecimal.ZERO) + amount
            paymentCountByMethod[methodName] =
                paymentCountByMethod.getOrDefault(methodName, 0) + 1
        }

        // Calculate average payment amount
        val averagePayment = if (paymentCount > 0) {
            totalPayments / BigDecimal(paymentCount)
        } else {
            BigDecimal.ZERO
        }

        // Convert payment method maps to Money values
        val methodStats = paymentsByMethod.mapValues { Money(it.value.toString()) }

        mapOf(
            "totalPayments" to Money(totalPayments.toString()),
            "paymentCount" to paymentCount,
            "averagePayment" to Money(averagePayment.toString()),
            "paymentsByMethod" to methodStats,
            "paymentCountByMethod" to paymentCountByMethod
        )
    }

    override suspend fun getTotalPaid(invoiceId: InvoiceId): Money = dbQuery {
        val invoice = InvoicesTable.selectAll()
            .where { InvoicesTable.id eq invoiceId.value.toJavaUuid() }
            .singleOrNull()
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

        Money(invoice[InvoicesTable.paidAmount].toString())
    }

    override suspend fun isFullyPaid(invoiceId: InvoiceId): Boolean = dbQuery {
        val invoice = InvoicesTable.selectAll()
            .where { InvoicesTable.id eq invoiceId.value.toJavaUuid() }
            .singleOrNull()
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

        invoice[InvoicesTable.paidAmount] >= invoice[InvoicesTable.totalAmount]
    }

    override suspend fun getRemainingBalance(invoiceId: InvoiceId): Money = dbQuery {
        val invoice = InvoicesTable.selectAll()
            .where { InvoicesTable.id eq invoiceId.value.toJavaUuid() }
            .singleOrNull()
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")

        val remaining = invoice[InvoicesTable.totalAmount] - invoice[InvoicesTable.paidAmount]
        Money(remaining.toString())
    }
}
