package tech.dokus.backend.routes.auth

import ai.dokus.foundation.database.repository.auth.TenantRepository
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.AvatarUploadResponse
import tech.dokus.domain.routes.Tenants
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receiveMultipart
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
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

    authenticateJwt {
        /**
         * POST /api/v1/tenants/avatar
         * Upload a company avatar image.
         * Expects multipart form data with a single "file" field.
         */
        post<Tenants.Avatar> {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()

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

            logger.info("Avatar uploaded successfully for tenant: $tenantId, key=${result.storageKeyPrefix}")

            val response = AvatarUploadResponse(
                avatar = result.avatar,
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
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()

            val storageKey = tenantRepository.getAvatarStorageKey(tenantId)
            if (storageKey == null) {
                throw DokusException.NotFound("No avatar set")
            }

            val avatar = avatarStorageService.getAvatarUrls(storageKey)
                ?: throw DokusException.NotFound("Avatar not found in storage")

            call.respond(HttpStatusCode.OK, avatar)
        }

        /**
         * DELETE /api/v1/tenants/avatar
         * Remove the company avatar.
         */
        delete<Tenants.Avatar> {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()

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

            call.respond(HttpStatusCode.NoContent)
        }
    }
}
