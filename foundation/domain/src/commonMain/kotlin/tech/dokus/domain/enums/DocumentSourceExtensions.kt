package tech.dokus.domain.enums

import tech.dokus.domain.model.DocumentSourceDto

/** Source trust priority for determining the authoritative origin of a document. */
val DocumentSource.originPriority: Int get() = when (this) {
    DocumentSource.Peppol -> 3
    DocumentSource.Email -> 2
    DocumentSource.Upload -> 1
    DocumentSource.Manual -> 0
}

/** Highest-priority source channel, or [DocumentSource.Upload] if no sources exist. */
val List<DocumentSourceDto>.effectiveOriginOrDefault: DocumentSource
    get() = maxByOrNull { it.sourceChannel.originPriority }?.sourceChannel ?: DocumentSource.Upload
