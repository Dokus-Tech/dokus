package tech.dokus.foundation.aura.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage

@Composable
fun UserAvatarImage(
    avatarUrl: String?,
    initials: String,
    size: Dp,
    radius: Dp,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    contentDescription: String? = null,
) {
    if (avatarUrl == null) {
        MonogramAvatar(
            initials = initials,
            size = size,
            radius = radius,
            modifier = modifier,
            fontSize = fontSize,
            contentDescription = contentDescription,
        )
        return
    }

    val imageModifier = modifier
        .size(size)
        .clip(RoundedCornerShape(radius))

    val fallback: @Composable () -> Unit = {
        MonogramAvatar(
            initials = initials,
            size = size,
            radius = radius,
            fontSize = fontSize,
            contentDescription = contentDescription,
        )
    }

    if (imageLoader != null) {
        SubcomposeAsyncImage(
            model = avatarUrl,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = imageModifier,
            loading = { fallback() },
            error = { fallback() }
        )
    } else {
        SubcomposeAsyncImage(
            model = avatarUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = imageModifier,
            loading = { fallback() },
            error = { fallback() }
        )
    }
}
