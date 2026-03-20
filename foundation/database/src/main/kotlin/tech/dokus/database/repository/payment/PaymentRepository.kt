package tech.dokus.database.repository.payment

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import tech.dokus.database.entity.PaymentEntity
import tech.dokus.database.mapper.from
import tech.dokus.database.tables.payment.PaymentsTable
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.util.UUID

/**
 * Repository for managing payment records.
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. Use NUMERIC for money to avoid rounding errors
 */
class PaymentRepository {

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
    ): Result<List<PaymentEntity>> = runSuspendCatching {
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
                .map { it.let { PaymentEntity.from(it) } }
        }
    }

}
