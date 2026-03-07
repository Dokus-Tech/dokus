package tech.dokus.backend.services.avatar

import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.model.User
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.foundation.backend.storage.AvatarStorageService

suspend fun AvatarStorageService.projectAvatarThumbnail(storageKey: String?): Thumbnail? {
    val resolvedStorageKey = storageKey?.takeIf { it.isNotBlank() } ?: return null
    return signAvatarUrls(resolvedStorageKey)
}

suspend fun UserRepository.projectUserAvatar(
    user: User,
    avatarStorageService: AvatarStorageService
): User {
    return user.copy(avatar = avatarStorageService.projectAvatarThumbnail(getAvatarStorageKey(user.id)))
}
