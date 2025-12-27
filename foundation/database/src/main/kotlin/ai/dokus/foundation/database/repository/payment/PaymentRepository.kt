package ai.dokus.foundation.database.repository.payment

import ai.dokus.foundation.database.tables.payment.PaymentsTable
import tech.dokus.domain.Money
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.PaymentId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.TransactionId
import ai.dokus.foundation.domain.model.PaymentDto
import tech.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * Repository for managing payment records.
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. Use NUMERIC for money to avoid rounding errors
 */
@OptIn(ExperimentalUuidApi::class)
class PaymentRepository {

    /**
     * Record a payment for an invoice.
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun createPayment(
        tenantId: TenantId,
        invoiceId: InvoiceId,
        amount: Money,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        transactionId: TransactionId?,
        notes: String?
    ): Result<PaymentDto> = runCatching {
        dbQuery {
            val id = PaymentsTable.insert {
                it[PaymentsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[PaymentsTable.invoiceId] = UUID.fromString(invoiceId.toString())
                it[PaymentsTable.amount] = java.math.BigDecimal(amount.value)
                it[PaymentsTable.paymentDate] = paymentDate
                it[PaymentsTable.paymentMethod] = paymentMethod
                it[PaymentsTable.transactionId] = transactionId?.value
                it[PaymentsTable.notes] = notes
            } get PaymentsTable.id

            PaymentsTable.selectAll().where {
                PaymentsTable.id eq id.value
            }.single().toPaymentDto()
        }
    }

    /**
     * Get a payment by ID.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getPayment(
        paymentId: PaymentId,
        tenantId: TenantId
    ): Result<PaymentDto?> = runCatching {
        dbQuery {
            PaymentsTable.selectAll().where {
                (PaymentsTable.id eq UUID.fromString(paymentId.toString())) and
                (PaymentsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull()?.toPaymentDto()
        }
    }

    /**
     * List payments for an invoice.
     */
    suspend fun listByInvoice(invoiceId: InvoiceId): Result<List<PaymentDto>> = runCatching {
        dbQuery {
            PaymentsTable.selectAll().where {
                PaymentsTable.invoiceId eq UUID.fromString(invoiceId.toString())
            }.orderBy(PaymentsTable.paymentDate, SortOrder.DESC)
                .map { it.toPaymentDto() }
        }
    }

    /**
     * List payments for a tenant with filters.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listByTenant(
        tenantId: TenantId,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        paymentMethod: PaymentMethod? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<PaymentDto>> = runCatching {
        dbQuery {
            var query = PaymentsTable.selectAll().where {
                PaymentsTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            fromDate?.let {
                query = query.andWhere { PaymentsTable.paymentDate greaterEq it }
            }
            toDate?.let {
                query = query.andWhere { PaymentsTable.paymentDate lessEq it }
            }
            paymentMethod?.let {
                query = query.andWhere { PaymentsTable.paymentMethod eq it }
            }

            query.orderBy(PaymentsTable.paymentDate, SortOrder.DESC)
                .limit(limit)
                .offset(offset.toLong())
                .map { it.toPaymentDto() }
        }
    }

    /**
     * Get total amount paid for an invoice.
     */
    suspend fun getTotalPaid(invoiceId: InvoiceId): Result<Money> = runCatching {
        dbQuery {
            val total = PaymentsTable.selectAll().where {
                PaymentsTable.invoiceId eq UUID.fromString(invoiceId.toString())
            }.sumOf { it[PaymentsTable.amount] }
            Money(total.toString())
        }
    }

    /**
     * Delete a payment.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deletePayment(
        paymentId: PaymentId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            PaymentsTable.deleteWhere {
                (PaymentsTable.id eq UUID.fromString(paymentId.toString())) and
                (PaymentsTable.tenantId eq UUID.fromString(tenantId.toString()))
            } > 0
        }
    }

    /**
     * Reconcile a payment with a bank transaction.
     */
    suspend fun reconcile(
        paymentId: PaymentId,
        tenantId: TenantId,
        transactionId: TransactionId
    ): Result<Boolean> = runCatching {
        dbQuery {
            PaymentsTable.update({
                (PaymentsTable.id eq UUID.fromString(paymentId.toString())) and
                (PaymentsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[PaymentsTable.transactionId] = transactionId.value
            } > 0
        }
    }

    private fun ResultRow.toPaymentDto(): PaymentDto {
        return PaymentDto(
            id = PaymentId.parse(this[PaymentsTable.id].value.toString()),
            tenantId = TenantId.parse(this[PaymentsTable.tenantId].toString()),
            invoiceId = InvoiceId.parse(this[PaymentsTable.invoiceId].toString()),
            amount = Money(this[PaymentsTable.amount].toString()),
            paymentDate = this[PaymentsTable.paymentDate],
            paymentMethod = this[PaymentsTable.paymentMethod],
            transactionId = this[PaymentsTable.transactionId]?.let { TransactionId(it) },
            notes = this[PaymentsTable.notes],
            createdAt = this[PaymentsTable.createdAt]
        )
    }
}
