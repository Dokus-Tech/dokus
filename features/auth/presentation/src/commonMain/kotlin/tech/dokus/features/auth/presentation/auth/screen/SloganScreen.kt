@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for animation/UI constants (Kotlin convention)

package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.app_slogan
import tech.dokus.aura.resources.brand_motto
import tech.dokus.aura.resources.copyright
import tech.dokus.aura.resources.slogan_line_2
import tech.dokus.aura.resources.slogan_line_3
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// Animation timing constants
private const val InitialOffsetY = 30f
private const val InitialDelayMs = 500L
private const val StaggerDelayMs = 400L
private const val CreditsDelayMs = 700L
private const val SloganFadeDurationMs = 600
private const val CreditsFadeDurationMs = 1000

// Text style constants
private val HeadlineLetterSpacing = 1.sp
private val SubtitleLetterSpacing = 0.5.sp
private const val SubtitleTextAlpha = 0.85f
private const val MottoTextAlpha = 0.6f
private const val CopyrightTextAlpha = 0.4f

// Layout constants
private val SloganSpacing = 24.dp
private val SloganHorizontalPadding = 40.dp
private val SloganInternalSpacing = 8.dp
private val CreditsContainerPadding = 32.dp
private val CreditsSpacing = 4.dp

@Composable
internal fun SloganScreen() {
    // Animation states for text elements
    val sloganLine1Alpha = remember { Animatable(0f) }
    val sloganLine1OffsetY = remember { Animatable(InitialOffsetY) }
    val sloganLine2Alpha = remember { Animatable(0f) }
    val sloganLine2OffsetY = remember { Animatable(InitialOffsetY) }
    val sloganLine3Alpha = remember { Animatable(0f) }
    val sloganLine3OffsetY = remember { Animatable(InitialOffsetY) }
    val creditsAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Stage 1: Slogan lines (0.5-2.5s)
        delay(InitialDelayMs)
        launch {
            sloganLine1Alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(SloganFadeDurationMs, easing = FastOutSlowInEasing)
            )
        }
        launch {
            sloganLine1OffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(SloganFadeDurationMs, easing = FastOutSlowInEasing)
            )
        }

        delay(StaggerDelayMs)
        launch {
            sloganLine2Alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(SloganFadeDurationMs, easing = FastOutSlowInEasing)
            )
        }
        launch {
            sloganLine2OffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(SloganFadeDurationMs, easing = FastOutSlowInEasing)
            )
        }

        delay(StaggerDelayMs)
        launch {
            sloganLine3Alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(SloganFadeDurationMs, easing = FastOutSlowInEasing)
            )
        }
        launch {
            sloganLine3OffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(SloganFadeDurationMs, easing = FastOutSlowInEasing)
            )
        }

        // Stage 2: Credits (2.7s)
        delay(CreditsDelayMs)
        creditsAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(CreditsFadeDurationMs, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content (background effects are provided by the parent container once)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Marketing slogans with staggered entrance
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SloganSpacing),
                modifier = Modifier.padding(horizontal = SloganHorizontalPadding)
            ) {
                Text(
                    text = stringResource(Res.string.app_slogan),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = HeadlineLetterSpacing
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(sloganLine1Alpha.value)
                        .offset(y = sloganLine1OffsetY.value.dp)
                )

                Spacer(modifier = Modifier.height(SloganInternalSpacing))

                Text(
                    text = stringResource(Res.string.slogan_line_2),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = SubtitleLetterSpacing
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SubtitleTextAlpha),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(sloganLine2Alpha.value)
                        .offset(y = sloganLine2OffsetY.value.dp)
                )

                Text(
                    text = stringResource(Res.string.slogan_line_3),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = SubtitleLetterSpacing
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = SubtitleTextAlpha),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(sloganLine3Alpha.value)
                        .offset(y = sloganLine3OffsetY.value.dp)
                )
            }
        }

        // Credits at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(CreditsContainerPadding),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CreditsSpacing),
                modifier = Modifier.alpha(creditsAlpha.value)
            ) {
                Text(
                    text = stringResource(Res.string.brand_motto),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = HeadlineLetterSpacing
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = MottoTextAlpha)
                )
                Text(
                    text = stringResource(Res.string.copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = CopyrightTextAlpha)
                )
            }
        }
    }
}

@Preview
@Composable
private fun SloganPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SloganScreen()
    }
}
