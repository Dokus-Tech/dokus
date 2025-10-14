package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.mappers.PaymentMapper.toPayment
import ai.dokus.foundation.database.tables.PaymentsTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.Payment
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class PaymentRepository {
    private val logger = LoggerFactory.getLogger(PaymentRepository::class.java)

    suspend fun create(
        tenantId: TenantId,
        invoiceId: InvoiceId,
        amount: Money,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        transactionId: TransactionId? = null,
        notes: String? = null
    ): PaymentId = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()
        val javaInvoiceId = invoiceId.value.toJavaUuid()

        val paymentId = PaymentsTable.insertAndGetId {
            it[PaymentsTable.tenantId] = javaTenantId
            it[PaymentsTable.invoiceId] = javaInvoiceId
            it[PaymentsTable.amount] = amount.value
            it[PaymentsTable.paymentDate] = paymentDate
            it[PaymentsTable.paymentMethod] = paymentMethod
            it[PaymentsTable.transactionId] = transactionId?.value
            it[PaymentsTable.notes] = notes
        }.value

        logger.info("Created payment: $paymentId for invoice: $invoiceId, tenant: $tenantId")
        PaymentId(paymentId.toKotlinUuid())
    }

    suspend fun findById(id: PaymentId, tenantId: TenantId): Payment? = dbQuery {
        val javaPaymentId = id.value.toJavaUuid()
        val javaTenantId = tenantId.value.toJavaUuid()

        PaymentsTable
            .selectAll()
            .where {
                (PaymentsTable.id eq javaPaymentId) and
                (PaymentsTable.tenantId eq javaTenantId)
            }
            .singleOrNull()
            ?.toPayment()
    }

    suspend fun findByTenant(
        tenantId: TenantId,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        paymentMethod: PaymentMethod? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<Payment> = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        val query = PaymentsTable
            .selectAll()
            .where { PaymentsTable.tenantId eq javaTenantId }

        // Apply date range filter
        fromDate?.let { from ->
            query.andWhere { PaymentsTable.paymentDate greaterEq from }
        }
        toDate?.let { to ->
            query.andWhere { PaymentsTable.paymentDate lessEq to }
        }

        // Apply payment method filter
        paymentMethod?.let { method ->
            query.andWhere { PaymentsTable.paymentMethod eq method }
        }

        query
            .orderBy(PaymentsTable.paymentDate to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { it.toPayment() }
    }

    /**
     * Find all payments for a specific invoice
     * Critical for invoice payment tracking and status updates
     */
    suspend fun findByInvoice(invoiceId: InvoiceId, tenantId: TenantId): List<Payment> = dbQuery {
        val javaInvoiceId = invoiceId.value.toJavaUuid()
        val javaTenantId = tenantId.value.toJavaUuid()

        PaymentsTable
            .selectAll()
            .where {
                (PaymentsTable.invoiceId eq javaInvoiceId) and
                (PaymentsTable.tenantId eq javaTenantId)
            }
            .orderBy(PaymentsTable.paymentDate to SortOrder.ASC)
            .map { it.toPayment() }
    }

    /**
     * Calculate total amount paid for an invoice
     * Used to determine invoice payment status (Paid, PartiallyPaid, etc.)
     */
    suspend fun getTotalPaidForInvoice(invoiceId: InvoiceId, tenantId: TenantId): Money = dbQuery {
        val javaInvoiceId = invoiceId.value.toJavaUuid()
        val javaTenantId = tenantId.value.toJavaUuid()

        val sum = PaymentsTable
            .select(PaymentsTable.amount.sum())
            .where {
                (PaymentsTable.invoiceId eq javaInvoiceId) and
                (PaymentsTable.tenantId eq javaTenantId)
            }
            .singleOrNull()
            ?.get(PaymentsTable.amount.sum())

        Money(sum ?: BigDecimal.ZERO)
    }

    suspend fun update(
        id: PaymentId,
        tenantId: TenantId,
        amount: Money? = null,
        paymentDate: LocalDate? = null,
        paymentMethod: PaymentMethod? = null,
        transactionId: TransactionId? = null,
        notes: String? = null
    ): Boolean = dbQuery {
        val javaPaymentId = id.value.toJavaUuid()
        val javaTenantId = tenantId.value.toJavaUuid()

        val updated = PaymentsTable.update({
            (PaymentsTable.id eq javaPaymentId) and
            (PaymentsTable.tenantId eq javaTenantId)
        }) {
            amount?.let { value -> it[PaymentsTable.amount] = value.value }
            paymentDate?.let { value -> it[PaymentsTable.paymentDate] = value }
            paymentMethod?.let { value -> it[PaymentsTable.paymentMethod] = value }
            transactionId?.let { value -> it[PaymentsTable.transactionId] = value.value }
            notes?.let { value -> it[PaymentsTable.notes] = value }
        }

        if (updated > 0) {
            logger.info("Updated payment: $id for tenant: $tenantId")
        }

        updated > 0
    }

    suspend fun delete(id: PaymentId, tenantId: TenantId): Boolean = dbQuery {
        val javaPaymentId = id.value.toJavaUuid()
        val javaTenantId = tenantId.value.toJavaUuid()

        val deleted = PaymentsTable.deleteWhere {
            (PaymentsTable.id eq javaPaymentId) and
            (PaymentsTable.tenantId eq javaTenantId)
        }

        if (deleted > 0) {
            logger.info("Deleted payment: $id for tenant: $tenantId")
        }

        deleted > 0
    }

    /**
     * Find payments by date range for reporting
     */
    suspend fun findByDateRange(
        tenantId: TenantId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<Payment> = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        PaymentsTable
            .selectAll()
            .where {
                (PaymentsTable.tenantId eq javaTenantId) and
                (PaymentsTable.paymentDate greaterEq fromDate) and
                (PaymentsTable.paymentDate lessEq toDate)
            }
            .orderBy(PaymentsTable.paymentDate to SortOrder.DESC)
            .map { it.toPayment() }
    }

    /**
     * Find payments by transaction ID (for webhook reconciliation)
     */
    suspend fun findByTransactionId(
        transactionId: TransactionId,
        tenantId: TenantId
    ): Payment? = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        PaymentsTable
            .selectAll()
            .where {
                (PaymentsTable.transactionId eq transactionId.value) and
                (PaymentsTable.tenantId eq javaTenantId)
            }
            .singleOrNull()
            ?.toPayment()
    }

    /**
     * Count payments by tenant
     */
    suspend fun countByTenant(tenantId: TenantId): Long = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        PaymentsTable
            .select(PaymentsTable.id.count())
            .where { PaymentsTable.tenantId eq javaTenantId }
            .single()[PaymentsTable.id.count()]
    }

    /**
     * Calculate total revenue received in a date range
     * For financial reporting and dashboard
     */
    suspend fun sumByDateRange(
        tenantId: TenantId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Money = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        val sum = PaymentsTable
            .select(PaymentsTable.amount.sum())
            .where {
                (PaymentsTable.tenantId eq javaTenantId) and
                (PaymentsTable.paymentDate greaterEq fromDate) and
                (PaymentsTable.paymentDate lessEq toDate)
            }
            .singleOrNull()
            ?.get(PaymentsTable.amount.sum())

        Money(sum ?: BigDecimal.ZERO)
    }
}
