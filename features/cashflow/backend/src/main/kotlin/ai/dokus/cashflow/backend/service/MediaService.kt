package ai.dokus.cashflow.backend.service

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.MediaDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/**
 * Interface for media service operations.
 *
 * This allows for easier testing and follows dependency inversion principle.
 */
interface IMediaService {
    suspend fun getMedia(mediaId: MediaId, tenantId: TenantId): MediaDto?
    suspend fun attachMedia(mediaId: MediaId, tenantId: TenantId, entityType: EntityType, entityId: String): Boolean
}

/**
 * Service for communicating with the Media module.
 *
 * This service fetches media data and attaches media to entities.
 * In production, this communicates via HTTP with the media-service.
 *
 * Tenant context is passed via X-Tenant-Id header for multi-tenant security.
 */
class MediaService(
    private val httpClient: HttpClient,
    private val mediaServiceBaseUrl: String
) : IMediaService {
    private val logger = LoggerFactory.getLogger(MediaService::class.java)

    companion object {
        private const val TENANT_HEADER = "X-Tenant-Id"
    }

    /**
     * Fetches media data by ID for a specific tenant.
     * Returns null if media not found or access denied.
     */
    override suspend fun getMedia(mediaId: MediaId, tenantId: TenantId): MediaDto? {
        return try {
            val response: HttpResponse = httpClient.get("$mediaServiceBaseUrl/api/v1/media/$mediaId") {
                header(TENANT_HEADER, tenantId.toString())
            }

            when (response.status) {
                HttpStatusCode.OK -> response.body<MediaDto>()
                HttpStatusCode.NotFound, HttpStatusCode.Forbidden -> {
                    logger.debug("Media $mediaId not found or access denied for tenant $tenantId")
                    null
                }
                else -> {
                    logger.warn("Failed to fetch media $mediaId: ${response.status}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching media $mediaId", e)
            null
        }
    }

    /**
     * Attaches media to an entity (invoice, expense, bill).
     * Returns true if successful, false otherwise.
     */
    override suspend fun attachMedia(
        mediaId: MediaId,
        tenantId: TenantId,
        entityType: EntityType,
        entityId: String
    ): Boolean {
        return try {
            val response: HttpResponse = httpClient.post("$mediaServiceBaseUrl/api/v1/media/$mediaId/attach") {
                header(TENANT_HEADER, tenantId.toString())
                contentType(ContentType.Application.Json)
                setBody(AttachMediaRequest(entityType, entityId))
            }

            if (response.status == HttpStatusCode.OK) {
                logger.info("Attached media $mediaId to $entityType:$entityId")
                true
            } else {
                logger.warn("Failed to attach media $mediaId to $entityType:$entityId: ${response.status}")
                false
            }
        } catch (e: Exception) {
            logger.error("Error attaching media $mediaId to $entityType:$entityId", e)
            false
        }
    }
}

@Serializable
private data class AttachMediaRequest(
    val entityType: EntityType,
    val entityId: String
)
