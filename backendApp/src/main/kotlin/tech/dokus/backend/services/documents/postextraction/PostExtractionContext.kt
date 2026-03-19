package tech.dokus.backend.services.documents.postextraction

import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.features.ai.models.DirectionResolutionSource
import tech.dokus.features.ai.models.FinancialExtractionResult

data class PostExtractionContext(
    val tenantId: TenantId,
    val documentId: DocumentId,
    val sourceId: DocumentSourceId?,
    val sourceChannel: DocumentSource,
    val documentType: DocumentType,
    val draftData: DocumentDraftData?,
    val confidence: Double,
    val auditPassed: Boolean,
    val directionSource: DirectionResolutionSource,
    val extraction: FinancialExtractionResult,
    val tenantVat: VatNumber?,
    val bankTransactionSource: BankTransactionSource? = null,
)
