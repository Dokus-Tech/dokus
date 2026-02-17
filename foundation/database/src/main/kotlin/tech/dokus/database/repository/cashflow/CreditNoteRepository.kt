package tech.dokus.database.repository.cashflow
import kotlin.uuid.Uuid

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
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CreditNoteStatus
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.SettlementIntent
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateCreditNoteRequest
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery

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
    ): Result<FinancialDocumentDto.CreditNoteDto> = runCatching {
        dbQuery {
            val creditNoteId = CreditNotesTable.insertAndGetId {
                it[CreditNotesTable.tenantId] = tenantId.value
                it[contactId] = request.contactId.value
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
                it[documentId] = request.documentId?.let { id -> id.value }
            }

            // Fetch and return the created credit note
            CreditNotesTable.selectAll().where {
                (CreditNotesTable.id eq creditNoteId.value) and
                    (CreditNotesTable.tenantId eq tenantId.value)
            }.single().let { row ->
                mapRowToDto(row)
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
    ): Result<FinancialDocumentDto.CreditNoteDto?> = runCatching {
        dbQuery {
            CreditNotesTable.selectAll().where {
                (CreditNotesTable.id eq creditNoteId.value) and
                    (CreditNotesTable.tenantId eq tenantId.value)
            }.singleOrNull()?.let { row ->
                mapRowToDto(row)
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
    ): Result<PaginatedResponse<FinancialDocumentDto.CreditNoteDto>> = runCatching {
        dbQuery {
            var query = CreditNotesTable.selectAll().where {
                CreditNotesTable.tenantId eq tenantId.value
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
                    CreditNotesTable.contactId eq contactId.value
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
                .map { row -> mapRowToDto(row) }
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
    ): Result<FinancialDocumentDto.CreditNoteDto> = runCatching {
        dbQuery {
            val updated = CreditNotesTable.update({
                (CreditNotesTable.id eq creditNoteId.value) and
                    (CreditNotesTable.tenantId eq tenantId.value)
            }) {
                it[contactId] = request.contactId.value
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
                it[documentId] = request.documentId?.let { id -> id.value }
            }

            if (updated == 0) {
                throw IllegalArgumentException("Credit note not found or access denied")
            }

            CreditNotesTable.selectAll().where {
                (CreditNotesTable.id eq creditNoteId.value) and
                    (CreditNotesTable.tenantId eq tenantId.value)
            }.single().let { row ->
                mapRowToDto(row)
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
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updatedRows = CreditNotesTable.update({
                (CreditNotesTable.id eq creditNoteId.value) and
                    (CreditNotesTable.tenantId eq tenantId.value)
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
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updatedRows = CreditNotesTable.update({
                (CreditNotesTable.id eq creditNoteId.value) and
                    (CreditNotesTable.tenantId eq tenantId.value)
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
    ): FinancialDocumentDto.CreditNoteDto? = dbQuery {
        CreditNotesTable.selectAll().where {
            (CreditNotesTable.tenantId eq tenantId.value) and
                (CreditNotesTable.documentId eq documentId.value)
        }.singleOrNull()?.let { row ->
            mapRowToDto(row)
        }
    }

    /**
     * Delete credit note.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deleteCreditNote(
        creditNoteId: CreditNoteId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val deletedRows = CreditNotesTable.deleteWhere {
                (CreditNotesTable.id eq creditNoteId.value) and
                    (CreditNotesTable.tenantId eq tenantId.value)
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
    ): Result<Boolean> = runCatching {
        dbQuery {
            CreditNotesTable.selectAll().where {
                (CreditNotesTable.id eq creditNoteId.value) and
                    (CreditNotesTable.tenantId eq tenantId.value)
            }.count() > 0
        }
    }

    private fun mapRowToDto(row: org.jetbrains.exposed.v1.core.ResultRow): FinancialDocumentDto.CreditNoteDto {
        return FinancialDocumentDto.CreditNoteDto(
            id = CreditNoteId(row[CreditNotesTable.id].value),
            tenantId = TenantId(row[CreditNotesTable.tenantId]),
            contactId = ContactId(row[CreditNotesTable.contactId]),
            creditNoteType = row[CreditNotesTable.creditNoteType],
            creditNoteNumber = row[CreditNotesTable.creditNoteNumber],
            issueDate = row[CreditNotesTable.issueDate],
            subtotalAmount = Money.fromDbDecimal(row[CreditNotesTable.subtotalAmount]),
            vatAmount = Money.fromDbDecimal(row[CreditNotesTable.vatAmount]),
            totalAmount = Money.fromDbDecimal(row[CreditNotesTable.totalAmount]),
            status = row[CreditNotesTable.status],
            settlementIntent = row[CreditNotesTable.settlementIntent],
            documentId = row[CreditNotesTable.documentId]?.let { DocumentId.parse(it.toString()) },
            reason = row[CreditNotesTable.reason],
            currency = row[CreditNotesTable.currency],
            notes = row[CreditNotesTable.notes],
            createdAt = row[CreditNotesTable.createdAt],
            updatedAt = row[CreditNotesTable.updatedAt]
        )
    }
}
