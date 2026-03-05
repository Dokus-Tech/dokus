package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.coming_soon
import tech.dokus.aura.resources.documents_add_sheet_import_photos_subtitle
import tech.dokus.aura.resources.documents_add_sheet_import_photos_title
import tech.dokus.aura.resources.documents_add_sheet_scan_subtitle
import tech.dokus.aura.resources.documents_add_sheet_scan_title
import tech.dokus.aura.resources.documents_add_sheet_title
import tech.dokus.aura.resources.documents_add_sheet_upload_subtitle
import tech.dokus.aura.resources.documents_add_sheet_upload_title
import tech.dokus.aura.resources.upload_action_cancel

private val DragHandleWidth = 42.dp
private val DragHandleHeight = 4.dp
private val ContentPadding = 20.dp
private const val DisabledAlpha = 0.66f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocumentsAddDocumentSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onUploadFile: () -> Unit,
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(ContentPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(Res.string.documents_add_sheet_title),
                style = MaterialTheme.typography.titleLarge
            )

            SheetOptionRow(
                title = stringResource(Res.string.documents_add_sheet_scan_title),
                subtitle = stringResource(Res.string.documents_add_sheet_scan_subtitle),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null
                    )
                },
                enabled = false,
                badgeText = stringResource(Res.string.coming_soon),
                onClick = {}
            )

            SheetOptionRow(
                title = stringResource(Res.string.documents_add_sheet_upload_title),
                subtitle = stringResource(Res.string.documents_add_sheet_upload_subtitle),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null
                    )
                },
                enabled = true,
                badgeText = null,
                onClick = onUploadFile
            )

            SheetOptionRow(
                title = stringResource(Res.string.documents_add_sheet_import_photos_title),
                subtitle = stringResource(Res.string.documents_add_sheet_import_photos_subtitle),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null
                    )
                },
                enabled = false,
                badgeText = stringResource(Res.string.coming_soon),
                onClick = {}
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(Res.string.upload_action_cancel))
            }
        }
    }
}

@Composable
private fun BottomSheetDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(DragHandleWidth)
                .height(DragHandleHeight),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(DragHandleHeight / 2)
        ) {}
    }
}

@Composable
private fun SheetOptionRow(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    badgeText: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                if (!enabled && badgeText != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
