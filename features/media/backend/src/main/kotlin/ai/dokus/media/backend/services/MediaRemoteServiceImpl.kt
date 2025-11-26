package ai.dokus.media.backend.services

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest
import ai.dokus.foundation.domain.rpc.MediaRemoteService
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.requireAuthenticatedOrganizationId
import ai.dokus.foundation.messaging.core.MessagePublisher
import ai.dokus.foundation.messaging.messages.MediaProcessingRequestedMessage
import ai.dokus.media.backend.repository.MediaRepository
import ai.dokus.media.backend.repository.MediaRecord
import ai.dokus.media.backend.storage.MediaStorage
import ai.dokus.media.backend.storage.StoredMedia
import org.slf4j.LoggerFactory

class MediaRemoteServiceImpl(
    private val authInfoProvider: AuthInfoProvider,
    private val repository: MediaRepository,
    private val storage: MediaStorage,
    private val processingPublisher: MessagePublisher<MediaProcessingRequestedMessage>
) : MediaRemoteService {

    private val logger = LoggerFactory.getLogger(MediaRemoteServiceImpl::class.java)

    override suspend fun uploadMedia(request: MediaUploadRequest): MediaDto {
        return authInfoProvider.withAuthInfo {
            val organizationId = requireAuthenticatedOrganizationId()

            val validationError = storage.validate(request.fileContent, request.filename, request.contentType)
            if (validationError != null) {
                logger.warn("File validation failed for org=$organizationId: $validationError")
                throw IllegalArgumentException(validationError)
            }

            val mediaId = MediaId.generate()

            val stored = storage.store(
                organizationId = organizationId,
                mediaId = mediaId,
                filename = request.filename,
                mimeType = request.contentType,
                fileContent = request.fileContent
            ).getOrElse {
                logger.error("Failed to store media for org=$organizationId", it)
                throw it
            }

            val record = repository.create(
                mediaId = mediaId,
                organizationId = organizationId,
                filename = request.filename,
                mimeType = request.contentType,
                sizeBytes = request.fileContent.size.toLong(),
                status = MediaStatus.Pending,
                storageKey = stored.storageKey,
                storageBucket = stored.bucket,
                processingSummary = "Queued for processing",
                attachedEntityType = request.entityType,
                attachedEntityId = request.entityId
            ).getOrElse {
                logger.error("Failed to persist media metadata for org=$organizationId", it)
                // Rollback stored file
                storage.delete(stored.storageKey)
                throw it
            }

            publishProcessingRequested(record, stored)

            enrich(record)
        }
    }

    override suspend fun getMedia(mediaId: MediaId): MediaDto {
        return authInfoProvider.withAuthInfo {
            val organizationId = requireAuthenticatedOrganizationId()
            val record = repository.get(mediaId, organizationId).getOrElse {
                logger.error("Failed to load media $mediaId for org=$organizationId", it)
                throw it
            } ?: throw IllegalArgumentException("Media not found")

            enrich(record)
        }
    }

    override suspend fun listMedia(status: MediaStatus?, limit: Int, offset: Int): List<MediaDto> {
        return authInfoProvider.withAuthInfo {
            val organizationId = requireAuthenticatedOrganizationId()
            val records = repository.list(organizationId, status, limit, offset).getOrElse {
                logger.error("Failed to list media for org=$organizationId", it)
                throw it
            }
            records.map { enrich(it) }
        }
    }

    override suspend fun listPendingMedia(limit: Int, offset: Int): List<MediaDto> {
        return authInfoProvider.withAuthInfo {
            val organizationId = requireAuthenticatedOrganizationId()
            val pending = repository.list(organizationId, MediaStatus.Pending, limit, offset).getOrThrow()
            val processing = repository.list(organizationId, MediaStatus.Processing, limit, offset).getOrThrow()

            (pending + processing)
                .sortedByDescending { it.dto.createdAt }
                .map { enrich(it) }
        }
    }

    override suspend fun attachMedia(mediaId: MediaId, entityType: EntityType, entityId: String): MediaDto {
        return authInfoProvider.withAuthInfo {
            val organizationId = requireAuthenticatedOrganizationId()
            if (entityType !in setOf(EntityType.Invoice, EntityType.Expense)) {
                throw IllegalArgumentException("Unsupported entity type: $entityType")
            }

            val updated = repository.attach(mediaId, organizationId, entityType, entityId).getOrElse {
                logger.error("Failed to attach media $mediaId to $entityType:$entityId", it)
                throw it
            } ?: throw IllegalArgumentException("Media not found")

            enrich(updated)
        }
    }

    override suspend fun updateProcessingResult(request: MediaProcessingUpdateRequest): MediaDto {
        return authInfoProvider.withAuthInfo {
            val organizationId = requireAuthenticatedOrganizationId()

            if (request.status == MediaStatus.Pending) {
                throw IllegalArgumentException("Pending is not a valid target status for processing update")
            }

            val updated = repository.updateProcessing(
                mediaId = request.mediaId,
                organizationId = organizationId,
                status = request.status,
                processingSummary = request.summary,
                extraction = request.extraction,
                errorMessage = request.errorMessage,
                attachedEntityType = request.attachedEntityType,
                attachedEntityId = request.attachedEntityId
            ).getOrElse {
                logger.error("Failed to update processing status for media ${request.mediaId}", it)
                throw it
            } ?: throw IllegalArgumentException("Media not found")

            enrich(updated)
        }
    }

    private fun enrich(record: MediaRecord): MediaDto {
        val downloadUrl = storage.generateDownloadUrl(record.storageKey)
        return record.dto.copy(downloadUrl = downloadUrl)
    }

    private suspend fun publishProcessingRequested(record: MediaRecord, stored: StoredMedia) {
        val dto = record.dto
        val message = MediaProcessingRequestedMessage.create(
            mediaId = dto.id,
            organizationId = dto.organizationId,
            storageKey = stored.storageKey,
            storageBucket = stored.bucket,
            filename = dto.filename,
            mimeType = dto.mimeType,
            sizeBytes = dto.sizeBytes
        )

        processingPublisher.publish(message, MediaProcessingRequestedMessage.routingKey())
            .asResult
            .onFailure { logger.warn("Failed to publish processing event for media ${dto.id}", it) }
    }
}
