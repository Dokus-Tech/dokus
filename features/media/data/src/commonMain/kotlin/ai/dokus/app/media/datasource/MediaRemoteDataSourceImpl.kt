package ai.dokus.app.media.datasource

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaProcessingUpdateRequest
import ai.dokus.foundation.domain.model.MediaUploadRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

/**
 * HTTP-based implementation of MediaRemoteDataSource
 * Uses Ktor HttpClient to communicate with the media management API
 */
internal class MediaRemoteDataSourceImpl(
    private val httpClient: HttpClient
) : MediaRemoteDataSource {

    override suspend fun uploadMedia(request: MediaUploadRequest): Result<MediaDto> {
        return runCatching {
            httpClient.post("/api/v1/media") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            // Add file content
                            append(
                                key = "file",
                                value = request.fileContent,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, request.contentType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"${request.filename}\"")
                                }
                            )

                            // Add optional entity type and ID
                            request.entityType?.let { append("entityType", it.name) }
                            request.entityId?.let { append("entityId", it) }
                        }
                    )
                )
            }.body()
        }
    }

    override suspend fun getMedia(mediaId: MediaId): Result<MediaDto> {
        return runCatching {
            httpClient.get("/api/v1/media/$mediaId").body()
        }
    }

    override suspend fun listMedia(
        status: MediaStatus?,
        limit: Int,
        offset: Int
    ): Result<List<MediaDto>> {
        return runCatching {
            httpClient.get("/api/v1/media") {
                status?.let { parameter("status", it.name) }
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
        }
    }

    override suspend fun listPendingMedia(
        limit: Int,
        offset: Int
    ): Result<List<MediaDto>> {
        return runCatching {
            httpClient.get("/api/v1/media/pending") {
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
        }
    }

    override suspend fun attachMedia(
        mediaId: MediaId,
        entityType: EntityType,
        entityId: String
    ): Result<MediaDto> {
        return runCatching {
            httpClient.post("/api/v1/media/$mediaId/attach") {
                contentType(ContentType.Application.Json)
                setBody(AttachMediaRequest(entityType, entityId))
            }.body()
        }
    }

    override suspend fun updateProcessingResult(request: MediaProcessingUpdateRequest): Result<MediaDto> {
        return runCatching {
            httpClient.put("/api/v1/media/${request.mediaId}/processing-result") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }
}

/**
 * Request body for attaching media to an entity
 */
@kotlinx.serialization.Serializable
private data class AttachMediaRequest(
    val entityType: EntityType,
    val entityId: String
)
