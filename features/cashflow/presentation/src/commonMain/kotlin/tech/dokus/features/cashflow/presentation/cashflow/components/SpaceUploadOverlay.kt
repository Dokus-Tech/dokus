package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInQuart
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.BlackHoleVortex
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.GravitationalDocumentsLayer
import tech.dokus.features.cashflow.presentation.cashflow.components.upload.UploadOverlayHeader
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// Animation constants
private const val GravitationalFallDurationMs = 1400
private const val PostAbsorptionDelayMs = 400L
private const val FadeInDurationMs = 500
private const val FadeOutDurationMs = 400

// UI constants
private val BackdropBlur = 20.dp
private const val BackdropAlpha = 0.85f

/**
 * Data class representing a flying document in the upload animation.
 */
data class FlyingDocument(
    val id: String,
    val name: String,
    val startX: Float,
    val startY: Float,
    val targetAngle: Float,
    val progress: Animatable<Float, *> = Animatable(0f)
)

/**
 * Magnetic Black Hole upload overlay.
 * Features a dark vortex that gravitationally pulls documents in with physics-based animation.
 * Background is blurred for a dramatic effect.
 */
@Composable
fun SpaceUploadOverlay(
    isVisible: Boolean,
    isDragging: Boolean,
    flyingDocuments: List<FlyingDocument>,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Track animation completion
    LaunchedEffect(flyingDocuments) {
        if (flyingDocuments.isNotEmpty()) {
            // Animate all documents with gravitational acceleration
            flyingDocuments.forEach { doc ->
                doc.progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = GravitationalFallDurationMs,
                        easing = EaseInQuart // Accelerating fall into black hole
                    )
                )
            }
            delay(PostAbsorptionDelayMs) // Pause after documents absorbed
            onAnimationComplete()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(FadeInDurationMs)),
        exit = fadeOut(tween(FadeOutDurationMs)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Blurred dark backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(BackdropBlur)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = BackdropAlpha))
            )

            // The Black Hole
            BlackHoleVortex(
                isActive = isDragging,
                modifier = Modifier.align(Alignment.Center)
            )

            // Drop zone prompt
            if (isDragging) {
                UploadOverlayHeader(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Flying documents with gravitational pull
            GravitationalDocumentsLayer(
                documents = flyingDocuments
            )
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun SpaceUploadOverlayPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SpaceUploadOverlay(
            isVisible = true,
            isDragging = false,
            flyingDocuments = emptyList(),
            onAnimationComplete = {},
        )
    }
}
