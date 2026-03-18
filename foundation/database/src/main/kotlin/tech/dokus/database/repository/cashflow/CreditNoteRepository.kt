package tech.dokus.database.repository.cashflow

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.cashflow.CreditNotesTable
import tech.dokus.domain.enums.CreditNoteStatus
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.SettlementIntent
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.database.entity.CreditNoteEntity
import tech.dokus.database.mapper.from
import tech.dokus.domain.model.CreateCreditNoteRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID
import tech.dokus.foundation.backend.utils.runSuspendCatching

/**
 * Repository for managing credit notes.
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return credit notes from different tenants
 * 3. All operations must be tenant-isolated
 */
class CreditNoteRepository {

    /**
     * Create a new credit note.
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun createCreditNote(
        tenantId: TenantId,
        request: CreateCreditNoteRequest
    ): Result<CreditNoteEntity> = runSuspendCatching {
        dbQuery {
            val creditNoteId = CreditNotesTable.insertAndGetId {
                it[CreditNotesTable.tenantId] = UUID.fromString(tenantId.toString())
                it[contactId] = UUID.fromString(request.contactId.toString())
                it[creditNoteType] = request.creditNoteType
                it[creditNoteNumber] = request.creditNoteNumber
                it[issueDate] = request.issueDate
                it[subtotalAmount] = request.subtotalAmount.toDbDecimal()
                it[vatAmount] = request.vatAmount.toDbDecimal()
                it[totalAmount] = request.totalAmount.toDbDecimal()
                it[currency] = request.currency
                it[settlementIntent] = request.settlementIntent
                it[status] = CreditNoteStatus.Draft
                it[reason] = request.reason
                it[notes] = request.notes
                it[documentId] = request.documentId?.let { id -> UUID.fromString(id.toString()) }
            }

            // Fetch and return the created credit note
            CreditNotesTable.selectAll().where {
                (CreditNotesTable.id eq creditNoteId.value) and
                    (CreditNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                CreditNoteEntity.from(row)
            }
        }
    }

    /**
     * Get a single credit note by ID.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getCreditNote(
        creditNoteId: CreditNoteId,
        tenantId: TenantId
    ): Result<CreditNoteEntity?> = runSuspendCatching {
        dbQuery {
            CreditNotesTable.selectAll().where {
                (CreditNotesTable.id eq UUID.fromString(creditNoteId.toString())) and
                    (CreditNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull()?.let { row ->
                CreditNoteEntity.from(row)
            }
        }
    }

    /**
     * List credit notes for a tenant with optional filters.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listCreditNotes(
        tenantId: TenantId,
        status: CreditNoteStatus? = null,
        creditNoteType: CreditNoteType? = null,
        contactId: ContactId? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<CreditNoteEntity>> = runSuspendCatching {
        dbQuery {
            var query = CreditNotesTable.selectAll().where {
                CreditNotesTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            // Apply filters
            if (status != null) {
                query = query.andWhere { CreditNotesTable.status eq status }
            }
            if (creditNoteType != null) {
                query = query.andWhere { CreditNotesTable.creditNoteType eq creditNoteType }
            }
            if (contactId != null) {
                query = query.andWhere {
                    CreditNotesTable.contactId eq UUID.fromString(contactId.toString())
                }
            }
            if (fromDate != null) {
                query = query.andWhere { CreditNotesTable.issueDate greaterEq fromDate }
            }
            if (toDate != null) {
                query = query.andWhere { CreditNotesTable.issueDate lessEq toDate }
            }

            val total = query.count()

            val items = query.orderBy(CreditNotesTable.issueDate to SortOrder.DESC)
                .limit(limit + offset)
                .map { row -> CreditNoteEntity.from(row) }
                .drop(offset)

            PaginatedResponse(
                items = items,
                total = total,
                limit = limit,
                offset = offset
            )
        }
    }

    /**
     * Update credit note details.
     * CRITICAL: MUST filter by tenant_id.
     *
     * Does NOT update status; status transitions are handled separately.
     */
    suspend fun updateCreditNote(
        creditNoteId: CreditNoteId,
        tenantId: TenantId,
        request: CreateCreditNoteRequest
    ): Result<CreditNoteEntity> = runSuspendCatching {
        dbQuery {
            val updated = CreditNotesTable.update({
                (CreditNotesTable.id eq UUID.fromString(creditNoteId.toString())) and
                    (CreditNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[contactId] = UUID.fromString(request.contactId.toString())
                it[creditNoteType] = request.creditNoteType
                it[creditNoteNumber] = request.creditNoteNumber
                it[issueDate] = request.issueDate
                it[subtotalAmount] = request.subtotalAmount.toDbDecimal()
                it[vatAmount] = request.vatAmount.toDbDecimal()
                it[totalAmount] = request.totalAmount.toDbDecimal()
                it[currency] = request.currency
                it[settlementIntent] = request.settlementIntent
                it[reason] = request.reason
                it[notes] = request.notes
                it[documentId] = request.documentId?.let { id -> UUID.fromString(id.toString()) }
            }

            if (updated == 0) {
                throw IllegalArgumentException("Credit note not found or access denied")
            }

            CreditNotesTable.selectAll().where {
                (CreditNotesTable.id eq UUID.fromString(creditNoteId.toString())) and
                    (CreditNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                CreditNoteEntity.from(row)
            }
        }
    }

    /**
     * Update credit note status.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateStatus(
        creditNoteId: CreditNoteId,
        tenantId: TenantId,
        status: CreditNoteStatus
    ): Result<Boolean> = runSuspendCatching {
        dbQuery {
            val updatedRows = CreditNotesTable.update({
                (CreditNotesTable.id eq UUID.fromString(creditNoteId.toString())) and
                    (CreditNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[CreditNotesTable.status] = status
            }
            updatedRows > 0
        }
    }

    /**
     * Update credit note settlement intent.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateSettlementIntent(
        creditNoteId: CreditNoteId,
        tenantId: TenantId,
        settlementIntent: SettlementIntent
    ): Result<Boolean> = runSuspendCatching {
        dbQuery {
            val updatedRows = CreditNotesTable.update({
                (CreditNotesTable.id eq UUID.fromString(creditNoteId.toString())) and
                    (CreditNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[CreditNotesTable.settlementIntent] = settlementIntent
            }
            updatedRows > 0
        }
    }

    /**
     * Find credit note by document ID.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun findByDocumentId(
        tenantId: TenantId,
        documentId: DocumentId
    ): CreditNoteEntity? = dbQuery {
        CreditNotesTable.selectAll().where {
            (CreditNotesTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (CreditNotesTable.documentId eq UUID.fromString(documentId.toString()))
        }.singleOrNull()?.let { row ->
            CreditNoteEntity.from(row)
        }
    }

    /**
     * Delete credit note.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deleteCreditNote(
        creditNoteId: CreditNoteId,
        tenantId: TenantId
    ): Result<Boolean> = runSuspendCatching {
        dbQuery {
            val deletedRows = CreditNotesTable.deleteWhere {
                (CreditNotesTable.id eq UUID.fromString(creditNoteId.toString())) and
                    (CreditNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
            deletedRows > 0
        }
    }

    /**
     * Check if a credit note exists and belongs to the tenant.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun exists(
        creditNoteId: CreditNoteId,
        tenantId: TenantId
    ): Result<Boolean> = runSuspendCatching {
        dbQuery {
            CreditNotesTable.selectAll().where {
                (CreditNotesTable.id eq UUID.fromString(creditNoteId.toString())) and
                    (CreditNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0
        }
    }

}
