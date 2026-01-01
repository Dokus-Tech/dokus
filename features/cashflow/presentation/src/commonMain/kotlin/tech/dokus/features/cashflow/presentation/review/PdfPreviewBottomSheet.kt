package tech.dokus.features.cashflow.presentation.review

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.cashflow_document_preview_title
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constrains
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.X
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen bottom sheet for PDF preview on mobile.
 * Expands to 90% of screen height for comfortable document viewing.
 *
 * @param isVisible Whether the sheet is visible
 * @param onDismiss Callback when sheet is dismissed
 * @param previewState Current preview state
 * @param onLoadMore Callback to load more pages
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    previewState: DocumentPreviewState,
    onLoadMore: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    // Hide sheet when isVisible becomes false
    LaunchedEffect(isVisible) {
        if (!isVisible && sheetState.currentValue != SheetValue.Hidden) {
            sheetState.hide()
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier,
            dragHandle = null, // Use custom header instead
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f), // 90% of screen height
            ) {
                // Header with close button
                SheetHeader(onClose = onDismiss)

                // Preview content
                PdfPreviewPane(
                    state = previewState,
                    selectedFieldPath = null,
                    onLoadMore = onLoadMore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SheetHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Constrains.Spacing.medium,
                vertical = Constrains.Spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(48.dp)) // Balance for close button

        Text(
            text = stringResource(Res.string.cashflow_document_preview_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
        )

        IconButton(onClick = onClose) {
            PIcon(
                icon = FeatherIcons.X,
                description = stringResource(Res.string.action_close),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
