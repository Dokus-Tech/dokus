package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_change
import tech.dokus.aura.resources.action_upload
import tech.dokus.aura.resources.auth_tier_core
import tech.dokus.aura.resources.auth_tier_owner
import tech.dokus.aura.resources.state_uploading
import tech.dokus.aura.resources.user_avatar_content_description
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.User
import tech.dokus.features.auth.mvi.AvatarState
import tech.dokus.foundation.app.network.rememberAuthenticatedImageLoader
import tech.dokus.foundation.app.network.rememberResolvedApiUrl
import tech.dokus.foundation.aura.components.EditableAvatarSurface
import tech.dokus.foundation.aura.components.UserAvatarImage
import tech.dokus.foundation.aura.components.badges.TierBadge
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val EditableProfileAvatarSize = 88.dp
private val EditableProfileAvatarRadius = 28.dp

/**
 * Profile hero: centered avatar, name, email, tier badges.
 */
@Composable
internal fun ProfileHero(
    user: User,
    avatarState: AvatarState,
    onUploadAvatar: () -> Unit,
    onResetAvatarState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = userDisplayName(user)
    val initials = userInitials(user)
    val avatarUrl = rememberResolvedApiUrl(user.avatar?.medium)
    val imageLoader = rememberAuthenticatedImageLoader()
    val isAvatarBusy = avatarState is AvatarState.Uploading
    val uploadProgress = (avatarState as? AvatarState.Uploading)?.progress
    val editDescription = stringResource(
        if (user.avatar != null) Res.string.action_change else Res.string.action_upload
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(bottom = 14.dp),
    ) {
        EditableAvatarSurface(
            onEditClick = onUploadAvatar,
            editContentDescription = editDescription,
            modifier = Modifier.size(EditableProfileAvatarSize),
            shape = RoundedCornerShape(EditableProfileAvatarRadius),
            enabled = !isAvatarBusy,
            isBusy = isAvatarBusy,
            progress = uploadProgress,
        ) {
            UserAvatarImage(
                avatarUrl = avatarUrl,
                initials = initials,
                size = EditableProfileAvatarSize,
                radius = EditableProfileAvatarRadius,
                imageLoader = imageLoader,
                contentDescription = stringResource(Res.string.user_avatar_content_description),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.02).em,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = user.email.value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TierBadge(label = stringResource(Res.string.auth_tier_core))
            TierBadge(label = stringResource(Res.string.auth_tier_owner))
        }
        ProfileAvatarStateIndicator(
            avatarState = avatarState,
            onResetState = onResetAvatarState,
        )
    }
}

@Composable
private fun ProfileAvatarStateIndicator(
    avatarState: AvatarState,
    onResetState: () -> Unit,
) {
    when (avatarState) {
        is AvatarState.Uploading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                CircularProgressIndicator(
                    progress = { avatarState.progress },
                    modifier = Modifier.height(16.dp).width(16.dp)
                )
                Text(
                    text = stringResource(Res.string.state_uploading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        is AvatarState.Error -> {
            Text(
                text = avatarState.error.localized,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        AvatarState.Success -> {
            LaunchedEffect(Unit) {
                onResetState()
            }
        }

        AvatarState.Idle -> Unit
    }
}

internal fun userDisplayName(user: User): String {
    return buildString {
        user.firstName?.value?.let { append(it) }
        if (user.firstName != null && user.lastName != null) append(" ")
        user.lastName?.value?.let { append(it) }
    }.ifEmpty { user.email.value }
}

internal fun userInitials(user: User): String {
    val first = user.firstName?.value?.firstOrNull()
    val last = user.lastName?.value?.firstOrNull()
    return when {
        first != null && last != null -> "$first$last"
        first != null -> "${first}${user.firstName?.value?.getOrNull(1) ?: ""}"
        last != null -> "${last}${user.lastName?.value?.getOrNull(1) ?: ""}"
        else -> user.email.value.take(2)
    }.uppercase()
}

@OptIn(ExperimentalUuidApi::class)
internal val previewUser = User(
    id = UserId(Uuid.parse("00000000-0000-0000-0000-000000000001")),
    email = Email("john@dokus.tech"),
    firstName = Name("John"),
    lastName = Name("Doe"),
    emailVerified = true,
    isActive = true,
    createdAt = LocalDateTime(2025, 1, 1, 0, 0),
    updatedAt = LocalDateTime(2025, 1, 1, 0, 0),
)

@Preview
@Composable
private fun ProfileHeroPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ProfileHero(
            user = previewUser,
            avatarState = AvatarState.Idle,
            onUploadAvatar = {},
            onResetAvatarState = {},
        )
    }
}
