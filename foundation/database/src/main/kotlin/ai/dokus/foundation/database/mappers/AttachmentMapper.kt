package ai.dokus.foundation.database.mappers

import ai.dokus.foundation.database.tables.AttachmentsTable
import ai.dokus.foundation.domain.AttachmentId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.model.Attachment
import org.jetbrains.exposed.sql.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object AttachmentMapper {
    fun ResultRow.toAttachment(): Attachment {
        return Attachment(
            id = AttachmentId(this[AttachmentsTable.id].value.toKotlinUuid()),
            tenantId = TenantId(this[AttachmentsTable.tenantId].value.toKotlinUuid()),
            entityType = this[AttachmentsTable.entityType],
            entityId = this[AttachmentsTable.entityId].toString(),
            filename = this[AttachmentsTable.filename],
            mimeType = this[AttachmentsTable.mimeType],
            sizeBytes = this[AttachmentsTable.sizeBytes],
            s3Key = this[AttachmentsTable.s3Key],
            s3Bucket = this[AttachmentsTable.s3Bucket],
            uploadedAt = this[AttachmentsTable.uploadedAt]
        )
    }
}
