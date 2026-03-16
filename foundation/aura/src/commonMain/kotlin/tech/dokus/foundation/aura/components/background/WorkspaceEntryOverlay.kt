package tech.dokus.foundation.aura.components.background

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import tech.dokus.foundation.aura.style.dokusEffects

private const val OverlayInitialDelayMs = 100L
private const val OverlayFadeDurationMs = 800

/**
 * Full-screen overlay shown on the Home screen when arriving from workspace selection.
 *
 * Starts fully opaque and fades out, revealing the real Home content underneath.
 * The initial delay gives the Home composables time to compose and start loading data.
 *
 * @param onFadeComplete Called when the overlay has fully faded out
 */
@Composable
fun WorkspaceEntryOverlay(
    onFadeComplete: () -> Unit,
) {
    val effects = MaterialTheme.dokusEffects
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(OverlayInitialDelayMs)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = OverlayFadeDurationMs,
                easing = FastOutSlowInEasing,
            ),
        )
        onFadeComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .background(effects.revealSurface),
    )
}
