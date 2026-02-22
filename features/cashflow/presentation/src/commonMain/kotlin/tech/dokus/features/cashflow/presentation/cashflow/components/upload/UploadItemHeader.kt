package tech.dokus.features.cashflow.presentation.cashflow.components.upload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.upload_status_deleting
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val UploadRowHeight = Constraints.IconSize.xxLarge - Constraints.Spacing.xSmall
private val UploadRowPadding = Constraints.Spacing.small + Constraints.Spacing.xxSmall

/**
 * File icon with a customizable overlay indicator.
 *
 * Used to display a document icon with status indicators such as:
 * - Progress spinner for pending/uploading states
 * - Check mark for uploaded state
 * - Error icon for failed state
 *
 * @param overlay The overlay composable to render on top of the file icon
 */
@Composable
fun FileIconWithOverlay(
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.size(Constraints.CropGuide.cornerLength),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(Constraints.AvatarSize.small),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        overlay()
    }
}

/**
 * Pending state overlay showing a subtle spinning indicator.
 */
@Composable
fun PendingOverlay() {
    CircularProgressIndicator(
        modifier = Modifier.size(Constraints.CropGuide.cornerLength),
        strokeWidth = Constraints.Spacing.xxSmall,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

/**
 * Uploading state overlay showing an active spinning indicator.
 */
@Composable
fun UploadingOverlay() {
    CircularProgressIndicator(
        modifier = Modifier.size(Constraints.CropGuide.cornerLength),
        strokeWidth = Constraints.Spacing.xxSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Status badge overlay for completed states (uploaded, failed).
 *
 * Renders a small icon badge at the bottom-end corner of the file icon.
 *
 * @param icon The icon to display in the badge
 * @param tint The color tint for the icon
 */
@Composable
fun androidx.compose.foundation.layout.BoxScope.StatusBadgeOverlay(
    icon: ImageVector,
    tint: Color
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(Constraints.IconSize.xSmall)
            .background(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.shapes.extraSmall
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Constraints.IconSize.xSmall),
            tint = tint
        )
    }
}

/**
 * Uploaded state overlay showing a success checkmark badge.
 */
@Composable
fun androidx.compose.foundation.layout.BoxScope.UploadedOverlay() {
    StatusBadgeOverlay(
        icon = Icons.Default.CheckCircle,
        tint = MaterialTheme.colorScheme.primary
    )
}

/**
 * Failed state overlay showing an error badge.
 */
@Composable
fun androidx.compose.foundation.layout.BoxScope.FailedOverlay() {
    StatusBadgeOverlay(
        icon = Icons.Default.Error,
        tint = MaterialTheme.colorScheme.error
    )
}

/**
 * Dimmed file icon used for items being deleted.
 *
 * @param modifier Modifier to apply to the icon container
 */
@Composable
fun DeletingFileIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(Constraints.CropGuide.cornerLength),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(Constraints.AvatarSize.small),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        )
    }
}

/**
 * Standard row layout for upload item content.
 *
 * Provides consistent spacing and alignment for:
 * - Icon on the left
 * - File info (name + subtitle) in the center
 * - Action buttons on the right
 *
 * @param fileName The name of the file to display
 * @param subtitle Optional subtitle text (e.g., file size, status message)
 * @param subtitleColor Color for the subtitle text
 * @param icon Composable slot for the file icon
 * @param actions Composable slot for action buttons
 * @param modifier Modifier to apply to the row
 * @param subtitleContent Optional custom composable for subtitle area (overrides subtitle text)
 */
@Composable
fun UploadItemRow(
    fileName: String,
    subtitle: String?,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitleContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(UploadRowPadding)
            .heightIn(min = UploadRowHeight, max = UploadRowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium)
    ) {
        icon()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)
        ) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitleContent != null) {
                subtitleContent()
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        actions()
    }
}

/**
 * Deleting state file info showing dimmed text with "Deleting..." status.
 *
 * @param fileName The name of the file being deleted
 * @param modifier Modifier to apply to the column
 */
@Composable
fun DeletingFileInfo(
    fileName: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(Res.string.upload_status_deleting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun UploadItemRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        UploadItemRow(
            fileName = "invoice_2024_001.pdf",
            subtitle = "1.2 MB",
            icon = { FileIconWithOverlay { UploadingOverlay() } },
            actions = { CancelUploadAction(onClick = {}) }
        )
    }
}

@Preview
@Composable
private fun DeletingFileInfoPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DeletingFileInfo(fileName = "invoice_2024_001.pdf")
    }
}
