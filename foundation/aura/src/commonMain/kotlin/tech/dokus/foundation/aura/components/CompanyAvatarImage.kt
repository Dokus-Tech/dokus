@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for constants (Kotlin convention)

package tech.dokus.foundation.aura.components

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
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.company_avatar_content_description
import tech.dokus.foundation.aura.constrains.Constraints
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlin.math.abs

// Avatar font sizes for fallback display
private val FontSizeExtraSmall = 10.sp
private val FontSizeSmall = 14.sp
private val FontSizeMedium = 24.sp
private val FontSizeLarge = 48.sp
private val FontSizeExtraLarge = 96.sp

// Avatar color generation constants
private const val HueModulus = 360
private const val AvatarColorSaturation = 0.5f
private const val AvatarColorLightness = 0.7f

/**
 * Size variants for company avatars.
 * References centralized values from [Constraints.AvatarSize].
 */
enum class AvatarSize(val value: Dp) {
    ExtraSmall(Constraints.AvatarSize.extraSmall),
    Small(Constraints.AvatarSize.small),
    Medium(Constraints.AvatarSize.medium),
    Large(Constraints.AvatarSize.large),
    ExtraLarge(Constraints.AvatarSize.extraLarge)
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
 * @param imageLoader Optional image loader (useful for authenticated image URLs)
 * @param modifier Modifier to be applied to the avatar container
 * @param onClick Optional click handler for the avatar
 */
@Composable
fun CompanyAvatarImage(
    avatarUrl: String?,
    initial: String,
    size: AvatarSize = AvatarSize.Medium,
    shape: AvatarShape = AvatarShape.RoundedSquare,
    sizeOverride: Dp? = null,
    imageLoader: ImageLoader? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val resolvedSize = sizeOverride ?: size.value
    val clipShape = avatarClipShape(size = resolvedSize, shape = shape)

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(resolvedSize)
            .clip(clipShape)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl != null) {
            if (imageLoader != null) {
                SubcomposeAsyncImage(
                    model = avatarUrl,
                    imageLoader = imageLoader,
                    contentDescription = stringResource(Res.string.company_avatar_content_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(resolvedSize)
                        .clip(clipShape),
                    loading = {
                        AvatarFallback(
                            initial = initial,
                            size = resolvedSize,
                            shape = shape
                        )
                    },
                    error = {
                        AvatarFallback(
                            initial = initial,
                            size = resolvedSize,
                            shape = shape
                        )
                    }
                )
            } else {
                SubcomposeAsyncImage(
                    model = avatarUrl,
                    contentDescription = stringResource(Res.string.company_avatar_content_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(resolvedSize)
                        .clip(clipShape),
                    loading = {
                        AvatarFallback(
                            initial = initial,
                            size = resolvedSize,
                            shape = shape
                        )
                    },
                    error = {
                        AvatarFallback(
                            initial = initial,
                            size = resolvedSize,
                            shape = shape
                        )
                    }
                )
            }
        } else {
            AvatarFallback(
                initial = initial,
                size = resolvedSize,
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
    size: Dp,
    shape: AvatarShape
) {
    val clipShape = avatarClipShape(size = size, shape = shape)

    val backgroundColor = MaterialTheme.colorScheme.primaryContainer
    val textColor = MaterialTheme.colorScheme.primary

    val fontSize = avatarFontSize(size)

    Box(
        modifier = Modifier
            .size(size)
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

@Composable
private fun avatarClipShape(
    size: Dp,
    shape: AvatarShape
) = when (shape) {
    AvatarShape.Circle -> CircleShape
    AvatarShape.RoundedSquare -> when {
        size <= AvatarSize.ExtraSmall.value -> MaterialTheme.shapes.extraSmall
        size <= AvatarSize.Small.value -> MaterialTheme.shapes.small
        else -> MaterialTheme.shapes.medium
    }
}

private fun avatarFontSize(size: Dp) = when {
    size <= AvatarSize.ExtraSmall.value -> FontSizeExtraSmall
    size <= AvatarSize.Small.value -> FontSizeSmall
    size <= AvatarSize.Medium.value -> FontSizeMedium
    size <= AvatarSize.Large.value -> FontSizeLarge
    else -> (size.value * 0.375f).sp
}

/**
 * Generate a consistent color for an avatar based on a seed string.
 * This ensures the same string always produces the same color.
 */
@Composable
fun rememberAvatarColor(seed: String): Color {
    val hue = (abs(seed.hashCode()) % HueModulus).toFloat()
    return Color.hsl(hue, AvatarColorSaturation, AvatarColorLightness)
}

@Preview
@Composable
private fun CompanyAvatarImagePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CompanyAvatarImage(avatarUrl = null, initial = "D", size = AvatarSize.Medium)
    }
}
