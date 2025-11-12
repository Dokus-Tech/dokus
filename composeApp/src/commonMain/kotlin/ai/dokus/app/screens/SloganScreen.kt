package ai.dokus.app.screens

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.app_name
import ai.dokus.app.resources.generated.copyright
import ai.dokus.app.resources.generated.develop_by
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.SpotlightEffect
import ai.dokus.foundation.design.tooling.PreviewParameters
import ai.dokus.foundation.design.tooling.PreviewParametersProvider
import ai.dokus.foundation.design.tooling.TestWrapper
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

@Composable
fun SloganScreen() {
    // Animation states for text elements
    val titleAlpha = remember { Animatable(0f) }
    val sloganLine1Alpha = remember { Animatable(0f) }
    val sloganLine1OffsetY = remember { Animatable(30f) }
    val sloganLine2Alpha = remember { Animatable(0f) }
    val sloganLine2OffsetY = remember { Animatable(30f) }
    val sloganLine3Alpha = remember { Animatable(0f) }
    val sloganLine3OffsetY = remember { Animatable(30f) }
    val creditsAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Stage 1: Title (0-0.8s)
        launch {
            titleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
        }

        // Stage 2: Slogan lines (1.0-3.0s)
        delay(1000)
        launch {
            sloganLine1Alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            sloganLine1OffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }

        delay(400)
        launch {
            sloganLine2Alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            sloganLine2OffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }

        delay(400)
        launch {
            sloganLine3Alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            sloganLine3OffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }

        // Stage 3: Credits (3.5s)
        delay(700)
        creditsAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Beautiful floating particles/bubbles background
        EnhancedFloatingBubbles()

        // Spotlight effect from top
        SpotlightEffect()

        // Main content
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App name
            Text(
                text = stringResource(Res.string.app_name),
                color = MaterialTheme.colorScheme.primaryContainer,
                style = TextStyle(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 12.sp
                ),
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Marketing slogans with staggered entrance
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                Text(
                    text = "Where sovereignty meets intelligence.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(sloganLine1Alpha.value)
                        .offset(y = sloganLine1OffsetY.value.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Crafted for those who build tomorrow.",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(sloganLine2Alpha.value)
                        .offset(y = sloganLine2OffsetY.value.dp)
                )

                Text(
                    text = "Trusted by those who own today.",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(sloganLine3Alpha.value)
                        .offset(y = sloganLine3OffsetY.value.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Separator
                Text(
                    text = "─────────",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        letterSpacing = 4.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.alpha(creditsAlpha.value)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Location and establishment
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Belgium",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.alpha(creditsAlpha.value)
                    )

                    Text(
                        text = "Est. 2025",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.alpha(creditsAlpha.value)
                    )
                }
            }
        }

        // Credits at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.alpha(creditsAlpha.value)
            ) {
                Text(
                    text = stringResource(Res.string.develop_by),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = stringResource(Res.string.copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
