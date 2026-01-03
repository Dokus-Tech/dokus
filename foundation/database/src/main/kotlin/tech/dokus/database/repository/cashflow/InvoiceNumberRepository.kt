package tech.dokus.database.repository.cashflow

import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.cashflow.InvoiceNumberSequencesTable
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.database.dbQuery
import java.sql.SQLException
import java.util.UUID

/**
 * Repository for managing invoice number sequences with atomic increment.
 *
 * Implements gap-less sequential invoice numbering as required by Belgian tax law.
 * Uses database-level locking (SELECT...FOR UPDATE) to ensure atomicity during
 * concurrent invoice creation.
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return sequences from different tenants
 * 3. All operations must be tenant-isolated
 */
class InvoiceNumberRepository {

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 50L
    }

    /**
     * Get the next invoice sequence number and atomically increment the counter.
     *
     * Uses SELECT...FOR UPDATE to acquire a row-level lock, ensuring that concurrent
     * requests are serialized. If the sequence row doesn't exist for the given tenant
     * and year, it creates one starting from 1.
     *
     * CRITICAL: MUST filter by tenant_id for multi-tenancy security
     *
     * @param tenantId The tenant requesting the sequence number
     * @param year The year for the invoice number (supports yearly reset)
     * @return Result containing the next sequence number (1-based), or failure on error
     */
    suspend fun getAndIncrementSequence(
        tenantId: TenantId,
        year: Int
    ): Result<Int> {
        return retryOnDeadlock {
            getAndIncrementSequenceInternal(tenantId, year)
        }
    }

    /**
     * Internal implementation of getAndIncrementSequence.
     * Separated for retry logic clarity.
     */
    private suspend fun getAndIncrementSequenceInternal(
        tenantId: TenantId,
        year: Int
    ): Result<Int> = runCatching {
        dbQuery {
            val tenantUuid = UUID.fromString(tenantId.toString())

            // Try to select existing row with FOR UPDATE lock
            val existingRow = InvoiceNumberSequencesTable.selectAll()
                .where {
                    (InvoiceNumberSequencesTable.tenantId eq tenantUuid) and
                        (InvoiceNumberSequencesTable.year eq year)
                }
                .forUpdate()
                .singleOrNull()

            if (existingRow != null) {
                // Row exists - increment and return
                val currentNumber = existingRow[InvoiceNumberSequencesTable.currentNumber]
                val nextNumber = currentNumber + 1

                InvoiceNumberSequencesTable.update({
                    (InvoiceNumberSequencesTable.tenantId eq tenantUuid) and
                        (InvoiceNumberSequencesTable.year eq year)
                }) {
                    it[InvoiceNumberSequencesTable.currentNumber] = nextNumber
                    it[updatedAt] = CurrentDateTime
                }

                nextNumber
            } else {
                // Row doesn't exist - create new sequence starting at 1
                InvoiceNumberSequencesTable.insert {
                    it[InvoiceNumberSequencesTable.tenantId] = tenantUuid
                    it[InvoiceNumberSequencesTable.year] = year
                    it[currentNumber] = 1
                }

                1
            }
        }
    }

    /**
     * Get the current sequence number without incrementing.
     *
     * Useful for preview/display purposes without consuming a number.
     *
     * CRITICAL: MUST filter by tenant_id for multi-tenancy security
     *
     * @param tenantId The tenant to query
     * @param year The year for the invoice number
     * @return Result containing the current sequence number (0 if not started), or failure on error
     */
    suspend fun getCurrentSequence(
        tenantId: TenantId,
        year: Int
    ): Result<Int> = runCatching {
        dbQuery {
            val tenantUuid = UUID.fromString(tenantId.toString())

            val row = InvoiceNumberSequencesTable.selectAll()
                .where {
                    (InvoiceNumberSequencesTable.tenantId eq tenantUuid) and
                        (InvoiceNumberSequencesTable.year eq year)
                }
                .singleOrNull()

            row?.get(InvoiceNumberSequencesTable.currentNumber) ?: 0
        }
    }

    /**
     * Initialize a sequence for a tenant/year if it doesn't exist.
     *
     * This is useful for pre-creating sequences (e.g., at start of year)
     * without consuming a number.
     *
     * CRITICAL: MUST filter by tenant_id for multi-tenancy security
     *
     * @param tenantId The tenant to initialize
     * @param year The year for the invoice number
     * @param startingNumber The starting number (default 0, first invoice will be 1)
     * @return Result indicating success or failure
     */
    suspend fun initializeSequence(
        tenantId: TenantId,
        year: Int,
        startingNumber: Int = 0
    ): Result<Unit> = runCatching {
        dbQuery {
            val tenantUuid = UUID.fromString(tenantId.toString())

            // Check if row already exists
            val exists = InvoiceNumberSequencesTable.selectAll()
                .where {
                    (InvoiceNumberSequencesTable.tenantId eq tenantUuid) and
                        (InvoiceNumberSequencesTable.year eq year)
                }
                .count() > 0

            if (!exists) {
                InvoiceNumberSequencesTable.insert {
                    it[InvoiceNumberSequencesTable.tenantId] = tenantUuid
                    it[InvoiceNumberSequencesTable.year] = year
                    it[currentNumber] = startingNumber
                }
            }
        }
    }

    /**
     * Retry logic with exponential backoff for handling database deadlocks.
     *
     * When multiple concurrent requests try to acquire the same row lock,
     * deadlocks can occur. This method implements retry with exponential
     * backoff to gracefully handle such situations.
     *
     * @param block The database operation to retry
     * @return Result of the operation, or failure after max retries
     */
    private suspend fun <T> retryOnDeadlock(block: suspend () -> Result<T>): Result<T> {
        var lastException: Throwable? = null
        var backoffMs = INITIAL_BACKOFF_MS

        repeat(MAX_RETRIES) { attempt ->
            val result = block()

            if (result.isSuccess) {
                return result
            }

            val exception = result.exceptionOrNull()
            lastException = exception

            // Check if this is a deadlock or lock contention error
            if (exception is SQLException && isDeadlockException(exception)) {
                // Wait with exponential backoff before retrying
                delay(backoffMs)
                backoffMs *= 2
            } else {
                // Not a deadlock - don't retry
                return result
            }
        }

        return Result.failure(
            lastException ?: IllegalStateException("Unexpected retry failure without exception")
        )
    }

    /**
     * Determine if an SQLException represents a deadlock or lock contention error.
     *
     * Different databases use different error codes:
     * - PostgreSQL: 40001 (serialization_failure), 40P01 (deadlock_detected)
     * - MySQL: 1213 (deadlock), 1205 (lock wait timeout)
     */
    private fun isDeadlockException(exception: SQLException): Boolean {
        val sqlState = exception.sqlState
        val errorCode = exception.errorCode

        // PostgreSQL deadlock/serialization failures
        if (sqlState == "40001" || sqlState == "40P01") {
            return true
        }

        // MySQL deadlock
        if (errorCode == 1213 || errorCode == 1205) {
            return true
        }

        // Check the exception chain for nested deadlock causes
        var cause = exception.cause
        while (cause != null) {
            if (cause is SQLException && isDeadlockException(cause)) {
                return true
            }
            cause = cause.cause
        }

        return false
    }
}
