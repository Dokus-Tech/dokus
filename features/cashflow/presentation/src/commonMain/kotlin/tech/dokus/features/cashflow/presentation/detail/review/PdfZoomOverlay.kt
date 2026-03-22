package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import tech.dokus.features.cashflow.presentation.detail.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.detail.PdfPreviewPane
import tech.dokus.foundation.aura.constrains.Constraints
import androidx.compose.ui.graphics.Color

/**
 * Fullscreen PDF zoom overlay.
 *
 * Shows a dark scrim covering the entire review surface with the full-size
 * PdfPreviewPane. Clicking outside or pressing Esc closes the overlay.
 *
 * Reuses the existing [PdfPreviewPane] composable.
 */
@Composable
internal fun PdfZoomOverlay(
    visible: Boolean,
    previewState: DocumentPreviewState,
    onDismiss: () -> Unit,
    onLoadMore: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            PdfPreviewPane(
                state = previewState,
                selectedFieldPath = null,
                onLoadMore = onLoadMore,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Constraints.Spacing.xxxLarge)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}, // Consume click to prevent dismissal
                    ),
            )
        }
    }
}
