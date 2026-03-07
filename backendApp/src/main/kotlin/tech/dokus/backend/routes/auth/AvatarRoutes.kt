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
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
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

private const val AvatarPresenceProbeSize = "small"

internal fun Route.avatarRoutes() {
    val tenantRepository by inject<TenantRepository>()
    val userRepository by inject<UserRepository>()
    val avatarStorageService by inject<AvatarStorageService>()
    val businessProfileService by inject<BusinessProfileService>()

    authenticateJwt {
        post<Tenants.Id.Avatar> { route ->
            val principal = dokusPrincipal
            val tenantId = parseTenantId(route.parent.id)
            requireTenantMembership(userRepository, principal.userId, tenantId)

            logger.info("Avatar upload request for tenant: $tenantId")

            val upload = call.receiveAvatarUploadPayload()
            deleteExistingAvatarIfPresent(
                storageKey = tenantRepository.getAvatarStorageKey(tenantId),
                avatarStorageService = avatarStorageService,
                ownerLabel = "tenant: $tenantId"
            )

            val result = try {
                avatarStorageService.uploadAvatar(tenantId, upload.fileBytes, upload.contentType)
            } catch (error: IllegalArgumentException) {
                throw DokusException.BadRequest(error.message ?: "Invalid image")
            }

            tenantRepository.updateAvatarStorageKey(tenantId, result.storageKeyPrefix)
            businessProfileService.markTenantAvatarUploaded(tenantId, result.storageKeyPrefix)

            call.respond(HttpStatusCode.Created, businessProfileService.buildTenantAvatarThumbnail(tenantId))
        }

        get<Tenants.Id.Avatar> { route ->
            val principal = dokusPrincipal
            val tenantId = parseTenantId(route.parent.id)
            requireTenantMembership(userRepository, principal.userId, tenantId)

            val storageKey = ensureAvatarMetadataAvailable(
                storageKey = tenantRepository.getAvatarStorageKey(tenantId),
                avatarStorageService = avatarStorageService,
                ownerLabel = "tenant: $tenantId"
            )

            if (storageKey.isBlank()) {
                throw DokusException.NotFound("No avatar set")
            }

            call.respond(HttpStatusCode.OK, businessProfileService.buildTenantAvatarThumbnail(tenantId))
        }

        get<Tenants.AvatarImageById> { route ->
            val principal = dokusPrincipal
            val tenantId = parseTenantId(route.id)
            requireTenantMembership(userRepository, principal.userId, tenantId)

            val imageBytes = loadTenantAvatarImageBytes(
                tenantId = tenantId,
                size = route.size,
                tenantRepository = tenantRepository,
                avatarStorageService = avatarStorageService
            )

            call.respondBytes(
                bytes = imageBytes,
                contentType = ContentType("image", "webp"),
                status = HttpStatusCode.OK
            )
        }

        delete<Tenants.Id.Avatar> { route ->
            val principal = dokusPrincipal
            val tenantId = parseTenantId(route.parent.id)
            requireTenantMembership(userRepository, principal.userId, tenantId)

            val storageKey = tenantRepository.getAvatarStorageKey(tenantId)
            if (storageKey == null) {
                call.respond(HttpStatusCode.NoContent)
                return@delete
            }

            deleteExistingAvatarIfPresent(
                storageKey = storageKey,
                avatarStorageService = avatarStorageService,
                ownerLabel = "tenant: $tenantId"
            )

            tenantRepository.updateAvatarStorageKey(tenantId, null)
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
                storageKey = userRepository.getAvatarStorageKey(userId),
                avatarStorageService = avatarStorageService,
                ownerLabel = "user: $userId"
            )

            val result = try {
                avatarStorageService.uploadUserAvatar(userId, upload.fileBytes, upload.contentType)
            } catch (error: IllegalArgumentException) {
                throw DokusException.BadRequest(error.message ?: "Invalid image")
            }

            userRepository.updateAvatarStorageKey(userId, result.storageKeyPrefix)

            call.respond(HttpStatusCode.Created, buildUserAvatarThumbnail(userId))
        }

        get<Users.Id.Avatar> { route ->
            val userId = parseUserId(route.parent.id)
            ensureAvatarMetadataAvailable(
                storageKey = userRepository.getAvatarStorageKey(userId),
                avatarStorageService = avatarStorageService,
                ownerLabel = "user: $userId"
            )

            call.respond(HttpStatusCode.OK, buildUserAvatarThumbnail(userId))
        }

        get<Users.AvatarImageById> { route ->
            val userId = parseUserId(route.id)
            val imageBytes = loadUserAvatarImageBytes(
                userId = userId,
                size = route.size,
                userRepository = userRepository,
                avatarStorageService = avatarStorageService
            )

            call.respondBytes(
                bytes = imageBytes,
                contentType = ContentType("image", "webp"),
                status = HttpStatusCode.OK
            )
        }

        delete<Users.Id.Avatar> { route ->
            val principal = dokusPrincipal
            val userId = parseUserId(route.parent.id)
            requireSelf(principal.userId, userId)

            val storageKey = userRepository.getAvatarStorageKey(userId)
            if (storageKey == null) {
                call.respond(HttpStatusCode.NoContent)
                return@delete
            }

            deleteExistingAvatarIfPresent(
                storageKey = storageKey,
                avatarStorageService = avatarStorageService,
                ownerLabel = "user: $userId"
            )

            userRepository.updateAvatarStorageKey(userId, null)

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private suspend fun requireTenantMembership(
    userRepository: UserRepository,
    userId: UserId,
    tenantId: TenantId
) {
    val membership = userRepository.getMembership(userId, tenantId)
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

private data class AvatarUploadPayload(
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
    val resolvedStorageKey = storageKey ?: throw DokusException.NotFound("No avatar set")
    if (avatarStorageService.getAvatarBytes(resolvedStorageKey, AvatarPresenceProbeSize) == null) {
        logger.debug(
            "Missing avatar object for owner={}, size={}, reason=storage_missing",
            ownerLabel,
            AvatarPresenceProbeSize
        )
        throw DokusException.NotFound("Avatar not found in storage")
    }
    return resolvedStorageKey
}

private suspend fun loadTenantAvatarImageBytes(
    tenantId: TenantId,
    size: String,
    tenantRepository: TenantRepository,
    avatarStorageService: AvatarStorageService
): ByteArray {
    val normalizedSize = avatarStorageService.normalizeSize(size)
        ?: throw DokusException.BadRequest("Avatar size must be one of: small, medium, large")

    val storageKey = tenantRepository.getAvatarStorageKey(tenantId)
        ?: throw DokusException.NotFound("No avatar set")

    val imageBytes = avatarStorageService.getAvatarBytes(storageKey, normalizedSize)
    if (imageBytes == null) {
        logger.debug(
            "Missing tenant avatar object for tenant={}, size={}, reason=storage_missing",
            tenantId,
            normalizedSize
        )
        throw DokusException.NotFound("Avatar not found in storage")
    }
    return imageBytes
}

private suspend fun loadUserAvatarImageBytes(
    userId: UserId,
    size: String,
    userRepository: UserRepository,
    avatarStorageService: AvatarStorageService
): ByteArray {
    val normalizedSize = avatarStorageService.normalizeSize(size)
        ?: throw DokusException.BadRequest("Avatar size must be one of: small, medium, large")

    val storageKey = userRepository.getAvatarStorageKey(userId)
        ?: throw DokusException.NotFound("No avatar set")

    val imageBytes = avatarStorageService.getAvatarBytes(storageKey, normalizedSize)
    if (imageBytes == null) {
        logger.debug(
            "Missing user avatar object for user={}, size={}, reason=storage_missing",
            userId,
            normalizedSize
        )
        throw DokusException.NotFound("Avatar not found in storage")
    }
    return imageBytes
}
