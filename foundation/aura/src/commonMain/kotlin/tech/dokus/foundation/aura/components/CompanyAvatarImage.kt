package tech.dokus.foundation.aura.components

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.company_avatar_content_description
import tech.dokus.foundation.aura.constrains.Constrains
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource

/**
 * Size variants for company avatars.
 * References centralized values from [Constrains.AvatarSize].
 */
enum class AvatarSize(val value: Dp) {
    ExtraSmall(Constrains.AvatarSize.extraSmall),
    Small(Constrains.AvatarSize.small),
    Medium(Constrains.AvatarSize.medium),
    Large(Constrains.AvatarSize.large),
    ExtraLarge(Constrains.AvatarSize.extraLarge)
}

/**
 * Shape variants for company avatars.
 */
enum class AvatarShape {
    Circle,
    RoundedSquare
}

/**
 * A composable that displays a company avatar image.
 * Falls back to showing the company initial letter on a colored background
 * when no avatar URL is provided or the image fails to load.
 *
 * @param avatarUrl The URL of the avatar image, or null for fallback
 * @param initial The initial letter to display as fallback (typically first letter of company name)
 * @param size The size of the avatar (Small, Medium, Large, ExtraLarge)
 * @param shape The shape of the avatar (Circle or RoundedSquare)
 * @param modifier Modifier to be applied to the avatar container
 * @param onClick Optional click handler for the avatar
 */
@Composable
fun CompanyAvatarImage(
    avatarUrl: String?,
    initial: String,
    size: AvatarSize = AvatarSize.Medium,
    shape: AvatarShape = AvatarShape.RoundedSquare,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clipShape = when (shape) {
        AvatarShape.Circle -> CircleShape
        AvatarShape.RoundedSquare -> when (size) {
            AvatarSize.ExtraSmall -> MaterialTheme.shapes.extraSmall
            AvatarSize.Small -> MaterialTheme.shapes.small
            else -> MaterialTheme.shapes.medium
        }
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size.value)
            .clip(clipShape)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl != null) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = stringResource(Res.string.company_avatar_content_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size.value)
                    .clip(clipShape),
                loading = {
                    AvatarFallback(
                        initial = initial,
                        size = size,
                        shape = shape
                    )
                },
                error = {
                    AvatarFallback(
                        initial = initial,
                        size = size,
                        shape = shape
                    )
                }
            )
        } else {
            AvatarFallback(
                initial = initial,
                size = size,
                shape = shape
            )
        }
    }
}

/**
 * Fallback component that displays an initial letter on a colored background.
 */
@Composable
private fun AvatarFallback(
    initial: String,
    size: AvatarSize,
    shape: AvatarShape
) {
    val clipShape = when (shape) {
        AvatarShape.Circle -> CircleShape
        AvatarShape.RoundedSquare -> when (size) {
            AvatarSize.ExtraSmall -> MaterialTheme.shapes.extraSmall
            AvatarSize.Small -> MaterialTheme.shapes.small
            else -> MaterialTheme.shapes.medium
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.primaryContainer
    val textColor = MaterialTheme.colorScheme.primary

    val fontSize = when (size) {
        AvatarSize.ExtraSmall -> 10.sp
        AvatarSize.Small -> 14.sp
        AvatarSize.Medium -> 24.sp
        AvatarSize.Large -> 48.sp
        AvatarSize.ExtraLarge -> 96.sp
    }

    Box(
        modifier = Modifier
            .size(size.value)
            .clip(clipShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.take(1).uppercase(),
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Generate a consistent color for an avatar based on a seed string.
 * This ensures the same string always produces the same color.
 */
@Composable
fun rememberAvatarColor(seed: String): Color {
    val hue = (abs(seed.hashCode()) % 360).toFloat()
    return Color.hsl(hue, 0.5f, 0.7f)
}