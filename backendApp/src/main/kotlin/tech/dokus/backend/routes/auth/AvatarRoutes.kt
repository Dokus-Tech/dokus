package tech.dokus.backend.routes.auth

import tech.dokus.backend.security.requireTenantId

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receiveMultipart
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.AvatarUploadResponse
import tech.dokus.domain.routes.Tenants
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.storage.AvatarStorageService
import tech.dokus.foundation.backend.utils.loggerFor

private val logger = loggerFor("AvatarRoutes")

/**
 * Avatar routes for company logo management.
 *
 * Endpoints:
 * - POST /api/v1/tenants/avatar - Upload avatar (multipart/form-data)
 * - GET /api/v1/tenants/avatar - Get current avatar URLs
 * - DELETE /api/v1/tenants/avatar - Remove avatar
 */
internal fun Route.avatarRoutes() {
    val tenantRepository by inject<TenantRepository>()
    val avatarStorageService by inject<AvatarStorageService>()
    val businessProfileService by inject<BusinessProfileService>()

    authenticateJwt {
        /**
         * POST /api/v1/tenants/avatar
         * Upload a company avatar image.
         * Expects multipart form data with a single "file" field.
         */
        post<Tenants.Avatar> {
            val tenantId = requireTenantId()

            logger.info("Avatar upload request for tenant: $tenantId")

            // Parse multipart data
            val multipart = call.receiveMultipart()
            var fileBytes: ByteArray? = null
            var contentType: String? = null
            var filename: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        if (part.name == "file") {
                            contentType = part.contentType?.toString()
                            filename = part.originalFileName
                            fileBytes = part.streamProvider().readBytes()
                        }
                    }

                    else -> {}
                }
                part.dispose()
            }

            if (fileBytes == null || contentType == null) {
                throw DokusException.BadRequest("No image file provided")
            }

            // Delete existing avatar if any
            val existingKey = tenantRepository.getAvatarStorageKey(tenantId)
            if (existingKey != null) {
                logger.info("Deleting existing avatar for tenant: $tenantId")
                try {
                    avatarStorageService.deleteAvatar(existingKey)
                } catch (e: Exception) {
                    logger.warn("Failed to delete existing avatar", e)
                }
            }

            // Upload new avatar
            val result = try {
                avatarStorageService.uploadAvatar(tenantId, fileBytes, contentType!!)
            } catch (e: IllegalArgumentException) {
                throw DokusException.BadRequest(e.message ?: "Invalid image")
            }

            // Save storage key to database
            tenantRepository.updateAvatarStorageKey(tenantId, result.storageKeyPrefix)
            businessProfileService.markTenantAvatarUploaded(tenantId, result.storageKeyPrefix)

            logger.info("Avatar uploaded successfully for tenant: $tenantId, key=${result.storageKeyPrefix}")

            val response = AvatarUploadResponse(
                avatar = businessProfileService.buildTenantAvatarThumbnail(),
                storageKey = result.storageKeyPrefix,
                tenantId = tenantId
            )
            call.respond(HttpStatusCode.Created, response)
        }

        /**
         * GET /api/v1/tenants/avatar
         * Get current avatar URLs for the tenant.
         * Returns 404 if no avatar is set.
         */
        get<Tenants.Avatar> {
            val tenantId = requireTenantId()

            val storageKey = tenantRepository.getAvatarStorageKey(tenantId)
            if (storageKey == null) {
                throw DokusException.NotFound("No avatar set")
            }
            if (avatarStorageService.getAvatarBytes(storageKey, "small") == null) {
                logger.debug(
                    "Missing tenant avatar object for tenant={}, size=small, reason=storage_missing",
                    tenantId
                )
                throw DokusException.NotFound("Avatar not found in storage")
            }

            val avatar = businessProfileService.buildTenantAvatarThumbnail()

            call.respond(HttpStatusCode.OK, avatar)
        }

        /**
         * GET /api/v1/tenants/avatar/{size}.webp
         * Stream a tenant avatar image from backend storage.
         */
        get<Tenants.AvatarImage> { route ->
            val tenantId = requireTenantId()
            val size = avatarStorageService.normalizeSize(route.size)
                ?: throw DokusException.BadRequest("Avatar size must be one of: small, medium, large")

            val storageKey = tenantRepository.getAvatarStorageKey(tenantId)
                ?: throw DokusException.NotFound("No avatar set")

            val imageBytes = avatarStorageService.getAvatarBytes(storageKey, size)
            if (imageBytes == null) {
                logger.debug(
                    "Missing tenant avatar object for tenant={}, size={}, reason=storage_missing",
                    tenantId,
                    size
                )
                throw DokusException.NotFound("Avatar not found in storage")
            }

            call.respondBytes(
                bytes = imageBytes,
                contentType = ContentType("image", "webp"),
                status = HttpStatusCode.OK
            )
        }

        /**
         * DELETE /api/v1/tenants/avatar
         * Remove the company avatar.
         */
        delete<Tenants.Avatar> {
            val tenantId = requireTenantId()

            val storageKey = tenantRepository.getAvatarStorageKey(tenantId)
            if (storageKey == null) {
                call.respond(HttpStatusCode.NoContent)
                return@delete
            }

            logger.info("Deleting avatar for tenant: $tenantId")

            // Delete from storage
            try {
                avatarStorageService.deleteAvatar(storageKey)
            } catch (e: Exception) {
                logger.warn("Failed to delete avatar from storage", e)
            }

            // Clear from database
            tenantRepository.updateAvatarStorageKey(tenantId, null)
            businessProfileService.markTenantAvatarDeleted(tenantId)

            call.respond(HttpStatusCode.NoContent)
        }
    }
}
