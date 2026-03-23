package tech.dokus.backend.services.admin

import tech.dokus.database.entity.DraftSummaryEntity
import tech.dokus.database.entity.IngestionRunSummaryEntity
import tech.dokus.database.repository.auth.FirmRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentListPage
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentWithDraftAndIngestion
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.Firm
import tech.dokus.domain.model.auth.ConsoleClientSummary

/**
 * Service layer for console (firm-workspace) operations.
 * Wraps repository calls used by ConsoleRoutes.
 */
class ConsoleService(
    private val firmRepository: FirmRepository,
    private val tenantRepository: TenantRepository,
    private val documentRepository: DocumentRepository,
    private val ingestionRepository: DocumentIngestionRunRepository,
) {
    /**
     * Exposed for [tech.dokus.backend.security.requireFirmClientAccess] which requires
     * the repository directly. Will be removed once that security function is refactored.
     */
    val firmRepositoryForAccessCheck: FirmRepository get() = firmRepository

    suspend fun listClientSummaries(firmId: FirmId): List<ConsoleClientSummary> {
        val accessRows = firmRepository.listActiveAccessByFirm(firmId)
        if (accessRows.isEmpty()) return emptyList()

        val tenantsById = tenantRepository.findByIds(accessRows.map { it.tenantId })
            .associateBy { it.id }

        return accessRows
            .mapNotNull { access ->
                val tenant = tenantsById[access.tenantId] ?: return@mapNotNull null
                ConsoleClientSummary(
                    tenantId = tenant.id,
                    companyName = tenant.displayName,
                    vatNumber = tenant.vatNumber.takeIf { it.value.isNotBlank() }
                )
            }
            .sortedBy { it.companyName.value.lowercase() }
    }

    suspend fun listDocuments(
        tenantId: TenantId,
        filter: DocumentListFilter?,
        documentStatus: DocumentStatus?,
        documentType: DocumentType?,
        ingestionStatus: IngestionStatus?,
        page: Int,
        limit: Int,
    ): DocumentListPage<DocumentWithDraftAndIngestion> = documentRepository.listWithDraftsAndIngestion(
        tenantId = tenantId,
        filter = filter,
        documentStatus = documentStatus,
        documentType = documentType,
        ingestionStatus = ingestionStatus,
        page = page,
        limit = limit,
    )

    suspend fun getDocument(tenantId: TenantId, documentId: DocumentId): DocumentDto? =
        documentRepository.getById(tenantId, documentId)

    suspend fun getDraft(documentId: DocumentId, tenantId: TenantId): DraftSummaryEntity? =
        documentRepository.getDraftByDocumentId(documentId, tenantId)

    suspend fun getLatestIngestion(documentId: DocumentId, tenantId: TenantId): IngestionRunSummaryEntity? =
        ingestionRepository.getLatestForDocument(documentId, tenantId)

    suspend fun findFirmById(firmId: FirmId): Firm? = firmRepository.findById(firmId)

    suspend fun activateFirmAccess(firmId: FirmId, tenantId: TenantId, grantedByUserId: UserId): Boolean =
        firmRepository.activateAccess(firmId = firmId, tenantId = tenantId, grantedByUserId = grantedByUserId)
}
