package tech.dokus.backend.services.avatar

import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.User
import tech.dokus.domain.model.common.Thumbnail
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun buildVersionedAvatarThumbnail(basePath: String, storageKey: String): Thumbnail {
    val normalizedStorageKey = storageKey.trim()
    val version = normalizedStorageKey.substringAfterLast('/').ifBlank { normalizedStorageKey }
    val encodedVersion = URLEncoder.encode(version, StandardCharsets.UTF_8)
    return Thumbnail(
        small = "$basePath/small.webp?v=$encodedVersion",
        medium = "$basePath/medium.webp?v=$encodedVersion",
        large = "$basePath/large.webp?v=$encodedVersion"
    )
}

fun buildUserAvatarThumbnail(userId: UserId, storageKey: String): Thumbnail =
    buildVersionedAvatarThumbnail("/api/v1/users/$userId/avatar", storageKey)

suspend fun UserRepository.projectUserAvatar(user: User): User {
    val storageKey = getAvatarStorageKey(user.id)?.takeIf { it.isNotBlank() } ?: return user
    return user.copy(avatar = buildUserAvatarThumbnail(user.id, storageKey))
}
