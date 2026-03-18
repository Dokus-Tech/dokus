package tech.dokus.backend.routes.auth

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.avatar.buildUserAvatarThumbnail
import tech.dokus.backend.services.admin.TenantManagementService
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.routes.Tenants
import tech.dokus.domain.routes.Users
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.storage.AvatarStorageService
import tech.dokus.foundation.backend.utils.loggerFor

private val logger = loggerFor("AvatarRoutes")

internal fun Route.avatarRoutes() {
    val tenantManagementService by inject<TenantManagementService>()
    val avatarStorageService by inject<AvatarStorageService>()
    val businessProfileService by inject<BusinessProfileService>()

    get<Tenants.AvatarImageById> { route ->
        val tenantId = parseTenantId(route.id)
        val imageBytes = loadAvatarImageBytes(
            storageKey = tenantManagementService.getTenantAvatarStorageKey(tenantId),
            size = route.size,
            avatarStorageService = avatarStorageService,
            ownerLabel = "tenant: $tenantId"
        )

        call.respondBytes(
            bytes = imageBytes,
            contentType = ContentType("image", "webp"),
            status = HttpStatusCode.OK
        )
    }

    get<Users.AvatarImageById> { route ->
        val userId = parseUserId(route.id)
        val imageBytes = loadAvatarImageBytes(
            storageKey = tenantManagementService.getUserAvatarStorageKey(userId),
            size = route.size,
            avatarStorageService = avatarStorageService,
            ownerLabel = "user: $userId"
        )

        call.respondBytes(
            bytes = imageBytes,
            contentType = ContentType("image", "webp"),
            status = HttpStatusCode.OK
        )
    }

    authenticateJwt {
        post<Tenants.Id.Avatar> { route ->
            val principal = dokusPrincipal
            val tenantId = parseTenantId(route.parent.id)
            requireTenantMembership(tenantManagementService, principal.userId, tenantId)

            logger.info("Avatar upload request for tenant: $tenantId")

            val upload = call.receiveAvatarUploadPayload()
            deleteExistingAvatarIfPresent(
                storageKey = tenantManagementService.getTenantAvatarStorageKey(tenantId),
                avatarStorageService = avatarStorageService,
                ownerLabel = "tenant: $tenantId"
            )

            val result = try {
                avatarStorageService.uploadAvatar(tenantId, upload.fileBytes, upload.contentType)
            } catch (error: IllegalArgumentException) {
                throw DokusException.BadRequest(error.message ?: "Invalid image")
            }

            tenantManagementService.updateTenantAvatarStorageKey(tenantId, result.storageKeyPrefix)
            businessProfileService.markTenantAvatarUploaded(tenantId, result.storageKeyPrefix)

            call.respond(
                HttpStatusCode.Created,
                businessProfileService.buildTenantAvatarThumbnail(tenantId, result.storageKeyPrefix)
            )
        }

        get<Tenants.Id.Avatar> { route ->
            val principal = dokusPrincipal
            val tenantId = parseTenantId(route.parent.id)
            requireTenantMembership(tenantManagementService, principal.userId, tenantId)

            val storageKey = ensureAvatarMetadataAvailable(
                storageKey = tenantManagementService.getTenantAvatarStorageKey(tenantId),
                avatarStorageService = avatarStorageService,
                ownerLabel = "tenant: $tenantId"
            )

            call.respond(HttpStatusCode.OK, businessProfileService.buildTenantAvatarThumbnail(tenantId, storageKey))
        }

        delete<Tenants.Id.Avatar> { route ->
            val principal = dokusPrincipal
            val tenantId = parseTenantId(route.parent.id)
            requireTenantMembership(tenantManagementService, principal.userId, tenantId)

            val storageKey = tenantManagementService.getTenantAvatarStorageKey(tenantId)
            if (storageKey == null) {
                call.respond(HttpStatusCode.NoContent)
                return@delete
            }

            deleteExistingAvatarIfPresent(
                storageKey = storageKey,
                avatarStorageService = avatarStorageService,
                ownerLabel = "tenant: $tenantId"
            )

            tenantManagementService.updateTenantAvatarStorageKey(tenantId, null)
            businessProfileService.markTenantAvatarDeleted(tenantId)

            call.respond(HttpStatusCode.NoContent)
        }

        post<Users.Id.Avatar> { route ->
            val principal = dokusPrincipal
            val userId = parseUserId(route.parent.id)
            requireSelf(principal.userId, userId)

            logger.info("Avatar upload request for user: $userId")

            val upload = call.receiveAvatarUploadPayload()
            deleteExistingAvatarIfPresent(
                storageKey = tenantManagementService.getUserAvatarStorageKey(userId),
                avatarStorageService = avatarStorageService,
                ownerLabel = "user: $userId"
            )

            val result = try {
                avatarStorageService.uploadUserAvatar(userId, upload.fileBytes, upload.contentType)
            } catch (error: IllegalArgumentException) {
                throw DokusException.BadRequest(error.message ?: "Invalid image")
            }

            tenantManagementService.updateUserAvatarStorageKey(userId, result.storageKeyPrefix)

            call.respond(HttpStatusCode.Created, buildUserAvatarThumbnail(userId, result.storageKeyPrefix))
        }

        get<Users.Id.Avatar> { route ->
            val userId = parseUserId(route.parent.id)
            val storageKey = ensureAvatarMetadataAvailable(
                storageKey = tenantManagementService.getUserAvatarStorageKey(userId),
                avatarStorageService = avatarStorageService,
                ownerLabel = "user: $userId"
            )

            call.respond(HttpStatusCode.OK, buildUserAvatarThumbnail(userId, storageKey))
        }

        delete<Users.Id.Avatar> { route ->
            val principal = dokusPrincipal
            val userId = parseUserId(route.parent.id)
            requireSelf(principal.userId, userId)

            val storageKey = tenantManagementService.getUserAvatarStorageKey(userId)
            if (storageKey == null) {
                call.respond(HttpStatusCode.NoContent)
                return@delete
            }

            deleteExistingAvatarIfPresent(
                storageKey = storageKey,
                avatarStorageService = avatarStorageService,
                ownerLabel = "user: $userId"
            )

            tenantManagementService.updateUserAvatarStorageKey(userId, null)

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private suspend fun requireTenantMembership(
    tenantManagementService: TenantManagementService,
    userId: UserId,
    tenantId: TenantId
) {
    val membership = tenantManagementService.getMembership(userId, tenantId)
    if (membership == null || !membership.isActive) {
        throw DokusException.NotAuthorized("You do not have access to tenant $tenantId")
    }
}

private fun requireSelf(principalUserId: UserId, routeUserId: UserId) {
    if (principalUserId != routeUserId) {
        throw DokusException.NotAuthorized("You can only manage your own avatar")
    }
}

private fun parseTenantId(rawValue: String): TenantId =
    runCatching { TenantId.parse(rawValue) }
        .getOrElse { throw DokusException.BadRequest("Invalid tenant id in path") }

private fun parseUserId(rawValue: String): UserId =
    runCatching { UserId.parse(rawValue) }
        .getOrElse { throw DokusException.BadRequest("Invalid user id in path") }

private class AvatarUploadPayload(
    val fileBytes: ByteArray,
    val contentType: String
)

private suspend fun ApplicationCall.receiveAvatarUploadPayload(): AvatarUploadPayload {
    val multipart = receiveMultipart()
    var fileBytes: ByteArray? = null
    var contentType: String? = null

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                if (part.name == "file") {
                    contentType = part.contentType?.toString()
                    fileBytes = part.streamProvider().readBytes()
                }
            }

            else -> {}
        }
        part.dispose()
    }

    val resolvedBytes = fileBytes ?: throw DokusException.BadRequest("No image file provided")
    val resolvedContentType = contentType ?: throw DokusException.BadRequest("No image file provided")
    return AvatarUploadPayload(
        fileBytes = resolvedBytes,
        contentType = resolvedContentType
    )
}

private suspend fun deleteExistingAvatarIfPresent(
    storageKey: String?,
    avatarStorageService: AvatarStorageService,
    ownerLabel: String
) {
    if (storageKey == null) return

    logger.info("Deleting existing avatar for $ownerLabel")
    try {
        avatarStorageService.deleteAvatar(storageKey)
    } catch (error: Exception) {
        logger.warn("Failed to delete existing avatar for $ownerLabel", error)
    }
}

private suspend fun ensureAvatarMetadataAvailable(
    storageKey: String?,
    avatarStorageService: AvatarStorageService,
    ownerLabel: String
): String {
    val resolvedStorageKey = storageKey
        ?.takeIf { it.isNotBlank() }
        ?: throw DokusException.NotFound("No avatar set")
    if (!avatarStorageService.avatarExists(resolvedStorageKey)) {
        logger.debug(
            "Missing avatar object for owner={}, reason=storage_missing",
            ownerLabel,
        )
        throw DokusException.NotFound("Avatar not found in storage")
    }
    return resolvedStorageKey
}

private suspend fun loadAvatarImageBytes(
    storageKey: String?,
    size: String,
    avatarStorageService: AvatarStorageService,
    ownerLabel: String
): ByteArray {
    val normalizedSize = avatarStorageService.normalizeSize(size)
        ?: throw DokusException.BadRequest("Avatar size must be one of: small, medium, large")

    val resolvedKey = storageKey
        ?: throw DokusException.NotFound("No avatar set")

    val imageBytes = avatarStorageService.getAvatarBytes(resolvedKey, normalizedSize)
    if (imageBytes == null) {
        logger.debug(
            "Missing avatar object for owner={}, size={}, reason=storage_missing",
            ownerLabel,
            normalizedSize
        )
        throw DokusException.NotFound("Avatar not found in storage")
    }
    return imageBytes
}
