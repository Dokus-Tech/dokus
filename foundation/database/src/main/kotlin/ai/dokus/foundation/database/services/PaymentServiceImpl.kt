package ai.dokus.foundation.database.services

import ai.dokus.foundation.database.mappers.PaymentMapper.toPayment
import ai.dokus.foundation.database.tables.InvoicesTable
import ai.dokus.foundation.database.tables.PaymentsTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.Payment
import ai.dokus.foundation.ktor.services.PaymentService
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
class PaymentServiceImpl : PaymentService {
    private val logger = LoggerFactory.getLogger(PaymentServiceImpl::class.java)

    override suspend fun recordPayment(
        tenantId: TenantId,
        invoiceId: InvoiceId,
        amount: Money,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        notes: String?
    ): Payment = dbQuery {
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
            it[PaymentsTable.amount] = BigDecimal(amount.amount)
            it[PaymentsTable.paymentDate] = kotlinx.datetime.toJavaLocalDate(paymentDate)
            it[PaymentsTable.paymentMethod] = paymentMethod
            it[PaymentsTable.transactionId] = transactionId
            it[PaymentsTable.notes] = notes
        }.value

        // Update invoice paid amount
        val currentPaid = invoice[InvoicesTable.paidAmount]
        val newPaid = currentPaid + BigDecimal(amount.amount)
        val total = invoice[InvoicesTable.totalAmount]

        InvoicesTable.update({ InvoicesTable.id eq invoiceId.value.toJavaUuid() }) {
            it[paidAmount] = newPaid
            if (newPaid >= total) {
                it[status] = InvoiceStatus.Paid
                it[paidAt] = kotlinx.datetime.toJavaInstant(kotlinx.datetime.Clock.System.now())
            }
        }

        logger.info("Recorded payment of $amount for invoice $invoiceId")

        PaymentsTable.selectAll()
            .where { PaymentsTable.id eq paymentId }
            .single()
            .toPayment()
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

        if (fromDate != null) query = query.andWhere { PaymentsTable.paymentDate greaterEq kotlinx.datetime.toJavaLocalDate(fromDate) }
        if (toDate != null) query = query.andWhere { PaymentsTable.paymentDate lessEq kotlinx.datetime.toJavaLocalDate(toDate) }
        if (paymentMethod != null) query = query.andWhere { PaymentsTable.paymentMethod eq paymentMethod }
        if (limit != null) query = query.limit(limit)
        if (offset != null) query = query.limit(limit ?: 100, offset.toLong())

        query.orderBy(PaymentsTable.paymentDate to SortOrder.DESC)
            .map { it.toPayment() }
    }

    override suspend fun delete(paymentId: PaymentId) = dbQuery {
        val payment = PaymentsTable.selectAll()
            .where { PaymentsTable.id eq paymentId.value.toJavaUuid() }
            .singleOrNull()
            ?.toPayment()
            ?: throw IllegalArgumentException("Payment not found: $paymentId")

        // Update invoice paid amount
        val invoice = InvoicesTable.selectAll()
            .where { InvoicesTable.id eq payment.invoiceId.value.toJavaUuid() }
            .single()

        val newPaid = invoice[InvoicesTable.paidAmount] - BigDecimal(payment.amount.amount)

        InvoicesTable.update({ InvoicesTable.id eq payment.invoiceId.value.toJavaUuid() }) {
            it[paidAmount] = newPaid
            if (newPaid < invoice[InvoicesTable.totalAmount]) {
                it[status] = InvoiceStatus.Sent
                it[paidAt] = null
            }
        }

        // Delete payment
        PaymentsTable.deleteWhere { PaymentsTable.id eq paymentId.value.toJavaUuid() }

        logger.info("Deleted payment $paymentId and updated invoice ${payment.invoiceId}")
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
    ): Map<String, Any> {
        // TODO: Implement statistics calculation
        return emptyMap()
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
