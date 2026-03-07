package tech.dokus.backend.services.avatar

import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.User
import tech.dokus.domain.model.common.Thumbnail

private const val AvatarSizeSmall = "small"
private const val AvatarSizeMedium = "medium"
private const val AvatarSizeLarge = "large"

fun buildUserAvatarThumbnail(userId: UserId): Thumbnail = Thumbnail(
    small = "/api/v1/users/$userId/avatar/$AvatarSizeSmall.webp",
    medium = "/api/v1/users/$userId/avatar/$AvatarSizeMedium.webp",
    large = "/api/v1/users/$userId/avatar/$AvatarSizeLarge.webp"
)

suspend fun UserRepository.projectUserAvatar(user: User): User {
    val storageKey = getAvatarStorageKey(user.id) ?: return user
    if (storageKey.isBlank()) return user
    return user.copy(avatar = buildUserAvatarThumbnail(user.id))
}
