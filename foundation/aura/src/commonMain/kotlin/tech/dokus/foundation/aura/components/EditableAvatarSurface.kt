package tech.dokus.foundation.aura.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val EditChipSize = 32.dp
private val EditChipOffset = 4.dp
private val EditChipBorderWidth = 1.dp
private val EditIconSize = 16.dp
private val EditProgressSize = 16.dp
private val EditProgressStroke = 2.dp

@Composable
fun EditableAvatarSurface(
    onEditClick: () -> Unit,
    editContentDescription: String,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    enabled: Boolean = true,
    isBusy: Boolean = false,
    progress: Float? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val canEdit = enabled && !isBusy

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .then(
                    if (canEdit) {
                        Modifier.clickable(onClick = onEditClick)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
            content = content,
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = EditChipOffset, y = EditChipOffset)
                .size(EditChipSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(EditChipBorderWidth, MaterialTheme.colorScheme.surface, CircleShape)
                .then(
                    if (canEdit) {
                        Modifier.clickable(onClick = onEditClick)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isBusy) {
                val uploadProgress = progress?.coerceIn(0f, 1f)
                if (uploadProgress != null) {
                    CircularProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.size(EditProgressSize),
                        strokeWidth = EditProgressStroke,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(EditProgressSize),
                        strokeWidth = EditProgressStroke,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = editContentDescription,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(EditIconSize),
                )
            }
        }
    }
}

@Preview
@Composable
private fun EditableAvatarSurfacePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        EditableAvatarSurface(
            onEditClick = {},
            editContentDescription = "Edit avatar",
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            MonogramAvatar(
                initials = "JD",
                size = 88.dp,
                radius = 28.dp,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Preview
@Composable
private fun EditableAvatarSurfaceUploadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        EditableAvatarSurface(
            onEditClick = {},
            editContentDescription = "Edit avatar",
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(28.dp),
            enabled = false,
            isBusy = true,
            progress = 0.64f,
        ) {
            MonogramAvatar(
                initials = "JD",
                size = 88.dp,
                radius = 28.dp,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
