package tech.dokus.features.cashflow.presentation.cashflow.components.upload

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.upload_documents_absorbed
import tech.dokus.aura.resources.upload_release_to_upload

// Animation constants
private const val PulseAnimationDurationMs = 1200
private const val AlphaInitial = 0.6f
private const val AlphaTarget = 1f
private const val SubtitleAlphaMultiplier = 0.7f

// UI dimensions
private val HeaderTopSpacing = 180.dp
private val TitleSubtitleSpacing = 8.dp

// Colors
private const val HighlightColorValue = 0xFFFFAA00

/**
 * Header prompt shown when dragging files over the upload overlay.
 *
 * Displays pulsing text prompting the user to release files
 * with "Release to Upload" as the primary message and
 * "Documents will be absorbed" as the secondary message.
 *
 * @param modifier Modifier to apply to the header
 */
@Composable
fun UploadOverlayHeader(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "promptPulse")

    val alpha by infiniteTransition.animateFloat(
        initialValue = AlphaInitial,
        targetValue = AlphaTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PulseAnimationDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textPulse"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(HeaderTopSpacing))

        Text(
            text = stringResource(Res.string.upload_release_to_upload),
            style = MaterialTheme.typography.headlineMedium,
            color = Color(HighlightColorValue).copy(alpha = alpha),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(TitleSubtitleSpacing))

        Text(
            text = stringResource(Res.string.upload_documents_absorbed),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = alpha * SubtitleAlphaMultiplier),
            textAlign = TextAlign.Center
        )
    }
}
