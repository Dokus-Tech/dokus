package tech.dokus.backend.mappers

import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.domain.Name
import tech.dokus.domain.model.Tenant
import tech.dokus.features.ai.graph.AcceptDocumentInput

fun AcceptDocumentInput.Companion.from(
    entity: IngestionItemEntity,
    tenant: Tenant,
    associatedPersonNames: List<Name>
): AcceptDocumentInput {
    return when (entity) {
        is IngestionItemEntity.Peppol -> from(entity, tenant, associatedPersonNames)
        is IngestionItemEntity.Upload -> from(entity, tenant, associatedPersonNames)
    }
}

fun AcceptDocumentInput.Companion.from(
    entity: IngestionItemEntity.Peppol,
    tenant: Tenant,
    associatedPersonNames: List<Name>
): AcceptDocumentInput.Peppol {
    return AcceptDocumentInput.Peppol(
        documentId = entity.documentId,
        tenant = tenant,
        associatedPersonNames = associatedPersonNames,
        userFeedback = entity.userFeedback,
        maxPagesOverride = entity.overrideMaxPages,
        dpiOverride = entity.overrideDpi,
        peppolStructuredSnapshotJson = entity.peppolStructuredSnapshotJson,
        peppolSnapshotVersion = entity.peppolSnapshotVersion
    )
}

fun AcceptDocumentInput.Companion.from(
    entity: IngestionItemEntity.Upload,
    tenant: Tenant,
    associatedPersonNames: List<Name>
): AcceptDocumentInput.Upload {
    return AcceptDocumentInput.Upload(
        documentId = entity.documentId,
        tenant = tenant,
        associatedPersonNames = associatedPersonNames,
        userFeedback = entity.userFeedback,
        maxPagesOverride = entity.overrideMaxPages,
        dpiOverride = entity.overrideDpi,
    )
}
