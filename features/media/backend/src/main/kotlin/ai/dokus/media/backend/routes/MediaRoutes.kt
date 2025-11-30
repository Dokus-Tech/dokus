package ai.dokus.media.backend.routes

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import ai.dokus.media.backend.repository.MediaRepository
import ai.dokus.media.backend.repository.MediaRecord
import ai.dokus.media.backend.storage.MediaStorage
import ai.dokus.media.backend.storage.StoredMedia
import ai.dokus.foundation.messaging.core.MessagePublisher
import ai.dokus.foundation.messaging.messages.MediaProcessingRequestedMessage
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("MediaRoutes")

/**
 * Media API Routes
 * Base path: /api/v1/media
 *
 * All routes require JWT authentication.
 */
fun Route.mediaRoutes() {
    val repository by inject<MediaRepository>()
    val storage by inject<MediaStorage>()
    val processingPublisher by inject<MessagePublisher<MediaProcessingRequestedMessage>>()

    route("/api/v1/media") {
        // POST /api/v1/media - Upload media file (multipart form)
        authenticateJwt {
            post {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val multipart = call.receiveMultipart()

                var fileContent: ByteArray? = null
                var filename: String? = null
                var contentType: String? = null
                var entityType: EntityType? = null
                var entityId: String? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            filename = part.originalFileName ?: "unknown"
                            contentType = part.contentType?.toString() ?: "application/octet-stream"
                            // Read bytes from input stream
                            val channel = part.streamProvider()
                            fileContent = runBlocking {
                                val outputStream = java.io.ByteArrayOutputStream()
                                channel.copyTo(outputStream)
                                outputStream.toByteArray()
                            }
                        }
                        is PartData.FormItem -> {
                            when (part.name) {
                                "entityType" -> entityType = part.value.takeIf { it.isNotBlank() }?.let { EntityType.valueOf(it) }
                                "entityId" -> entityId = part.value.takeIf { it.isNotBlank() }
                            }
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (fileContent == null || filename == null || contentType == null) {
                    throw DokusException.BadRequest("Missing required fields: file, filename, or contentType")
                }

                // Validate file
                val validationError = storage.validate(fileContent!!, filename!!, contentType!!)
                if (validationError != null) {
                    logger.warn("File validation failed for tenant=$tenantId: $validationError")
                    throw DokusException.BadRequest(validationError)
                }

                val mediaId = MediaId.generate()

                // Store file
                val stored = storage.store(
                    tenantId = tenantId,
                    mediaId = mediaId,
                    filename = filename!!,
                    mimeType = contentType!!,
                    fileContent = fileContent!!
                ).getOrElse {
                    logger.error("Failed to store media for tenant=$tenantId", it)
                    throw DokusException.InternalError("Failed to store media file")
                }

                // Persist metadata
                val record = repository.create(
                    mediaId = mediaId,
                    tenantId = tenantId,
                    filename = filename!!,
                    mimeType = contentType!!,
                    sizeBytes = fileContent!!.size.toLong(),
                    status = MediaStatus.Pending,
                    storageKey = stored.storageKey,
                    storageBucket = stored.bucket,
                    processingSummary = "Queued for processing",
                    attachedEntityType = entityType,
                    attachedEntityId = entityId
                ).getOrElse {
                    logger.error("Failed to persist media metadata for tenant=$tenantId", it)
                    // Rollback stored file
                    storage.delete(stored.storageKey)
                    throw DokusException.InternalError("Failed to persist media metadata")
                }

                // Publish processing event
                publishProcessingRequested(record, stored, processingPublisher)

                // Return enriched DTO with download URL
                val enrichedDto = enrichRecord(record, storage)
                call.respond(HttpStatusCode.Created, enrichedDto)
            }
        }

            // GET /api/v1/media/{id} - Get media by ID
            get("/{id}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val mediaIdStr = call.parameters["id"]
                    ?: throw DokusException.BadRequest("Media ID is required")

                val mediaId = MediaId.parse(mediaIdStr)
                val record = repository.get(mediaId, tenantId).getOrElse {
                    logger.error("Failed to load media $mediaId for tenant=$tenantId", it)
                    throw DokusException.InternalError("Failed to load media")
                } ?: throw DokusException.NotFound("Media not found")

                val enrichedDto = enrichRecord(record, storage)
                call.respond(HttpStatusCode.OK, enrichedDto)
            }

            // GET /api/v1/media - List media with optional filters
            get {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                val status = call.parameters["status"]?.let { MediaStatus.valueOf(it) }
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0

                if (limit < 1 || limit > 100) {
                    throw DokusException.BadRequest("Limit must be between 1 and 100")
                }
                if (offset < 0) {
                    throw DokusException.BadRequest("Offset must be non-negative")
                }

                val records = repository.list(tenantId, status, limit, offset).getOrElse {
                    logger.error("Failed to list media for tenant=$tenantId", it)
                    throw DokusException.InternalError("Failed to list media")
                }

                val enrichedDtos = records.map { enrichRecord(it, storage) }
                call.respond(HttpStatusCode.OK, enrichedDtos)
            }

            // GET /api/v1/media/pending - List pending media (convenience endpoint)
            get("/pending") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0

                if (limit < 1 || limit > 100) {
                    throw DokusException.BadRequest("Limit must be between 1 and 100")
                }
                if (offset < 0) {
                    throw DokusException.BadRequest("Offset must be non-negative")
                }

                val pending = repository.list(tenantId, MediaStatus.Pending, limit, offset).getOrThrow()
                val processing = repository.list(tenantId, MediaStatus.Processing, limit, offset).getOrThrow()

                val combined = (pending + processing)
                    .sortedByDescending { it.dto.createdAt }
                    .map { enrichRecord(it, storage) }

                call.respond(HttpStatusCode.OK, combined)
            }

            // POST /api/v1/media/{id}/attach - Attach media to an entity
            post("/{id}/attach") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val mediaIdStr = call.parameters["id"]
                    ?: throw DokusException.BadRequest("Media ID is required")

                val mediaId = MediaId.parse(mediaIdStr)

                val params = call.receive<AttachMediaRequest>()

                if (params.entityType !in setOf(EntityType.Invoice, EntityType.Expense)) {
                    throw DokusException.BadRequest("Unsupported entity type: ${params.entityType}")
                }

                val updated = repository.attach(mediaId, tenantId, params.entityType, params.entityId).getOrElse {
                    logger.error("Failed to attach media $mediaId to ${params.entityType}:${params.entityId}", it)
                    throw DokusException.InternalError("Failed to attach media")
                } ?: throw DokusException.NotFound("Media not found")

                val enrichedDto = enrichRecord(updated, storage)
                call.respond(HttpStatusCode.OK, enrichedDto)
            }

            // PUT /api/v1/media/{id}/processing-result - Update processing result (for AI pipeline)
            put("/{id}/processing-result") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()
                val mediaIdStr = call.parameters["id"]
                    ?: throw DokusException.BadRequest("Media ID is required")

                val mediaId = MediaId.parse(mediaIdStr)
                val request = call.receive<MediaProcessingUpdateRequest>()

                if (request.mediaId != mediaId) {
                    throw DokusException.BadRequest("Media ID in path does not match request body")
                }

                if (request.status == MediaStatus.Pending) {
                    throw DokusException.BadRequest("Pending is not a valid target status for processing update")
                }

                val updated = repository.updateProcessing(
                    mediaId = request.mediaId,
                    tenantId = tenantId,
                    status = request.status,
                    processingSummary = request.summary,
                    extraction = request.extraction,
                    errorMessage = request.errorMessage,
                    attachedEntityType = request.attachedEntityType,
                    attachedEntityId = request.attachedEntityId
                ).getOrElse {
                    logger.error("Failed to update processing status for media ${request.mediaId}", it)
                    throw DokusException.InternalError("Failed to update processing result")
                } ?: throw DokusException.NotFound("Media not found")

                val enrichedDto = enrichRecord(updated, storage)
                call.respond(HttpStatusCode.OK, enrichedDto)
            }
        }
    }
}

/**
 * Request body for attaching media to an entity
 */
@kotlinx.serialization.Serializable
data class AttachMediaRequest(
    val entityType: EntityType,
    val entityId: String
)

/**
 * Enriches a media record with a download URL
 */
private fun enrichRecord(record: MediaRecord, storage: MediaStorage): MediaDto {
    val downloadUrl = storage.generateDownloadUrl(record.storageKey)
    return record.dto.copy(downloadUrl = downloadUrl)
}

/**
 * Publishes a media processing requested event
 */
private suspend fun publishProcessingRequested(
    record: MediaRecord,
    stored: StoredMedia,
    processingPublisher: MessagePublisher<MediaProcessingRequestedMessage>
) {
    val dto = record.dto
    val message = MediaProcessingRequestedMessage.create(
        mediaId = dto.id,
        tenantId = dto.tenantId,
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
