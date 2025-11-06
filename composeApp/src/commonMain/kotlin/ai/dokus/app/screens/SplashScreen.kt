package ai.dokus.app.screens

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.bootstrap_state_app_version_check
import ai.dokus.app.resources.generated.bootstrap_state_authenticating
import ai.dokus.app.resources.generated.bootstrap_state_checking_account_status
import ai.dokus.app.resources.generated.bootstrap_state_initializing
import ai.dokus.app.viewmodel.BootstrapViewModel
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.replace
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Handles bootstrap navigation effects.
 */
private fun BootstrapViewModel.Effect.handle(navController: NavController) {
    when (this) {
        is BootstrapViewModel.Effect.Idle -> Unit
        is BootstrapViewModel.Effect.NeedsUpdate -> navController.replace(CoreDestination.UpdateRequired)
        is BootstrapViewModel.Effect.NeedsLogin -> navController.replace(AuthDestination.Login)
        is BootstrapViewModel.Effect.NeedsAccountConfirmation -> navController.replace(AuthDestination.PendingConfirmAccount)
        is BootstrapViewModel.Effect.Ok -> navController.replace(CoreDestination.Home)
    }
}

private val BootstrapViewModel.BootstrapState.localized: String
    @Composable get() = when (this) {
        BootstrapViewModel.BootstrapState.InitializeApp -> stringResource(Res.string.bootstrap_state_initializing)
        BootstrapViewModel.BootstrapState.CheckingLogin -> stringResource(Res.string.bootstrap_state_authenticating)
        BootstrapViewModel.BootstrapState.CheckUpdate -> stringResource(Res.string.bootstrap_state_app_version_check)
        BootstrapViewModel.BootstrapState.CheckingAccountStatus -> stringResource(Res.string.bootstrap_state_checking_account_status)
    }

@Composable
fun SplashScreen(
    viewModel: BootstrapViewModel = koinViewModel(),
) {
    val loadingStates by viewModel.loadingState.collectAsState()
    val navController = LocalNavController.current

    // Collect navigation effects and start bootstrap
    LaunchedEffect(Unit) { viewModel.effect.collect { it.handle(navController) } }
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold { contentPadding ->
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
                Text(
                    text = "D[#]kus",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    style = TextStyle(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(80.dp))

                // Bootstrap states as vertical list
                BootstrapStatesList(
                    allStates = listOf(
                        BootstrapViewModel.BootstrapState.InitializeApp,
                        BootstrapViewModel.BootstrapState.CheckingLogin,
                        BootstrapViewModel.BootstrapState.CheckingAccountStatus,
                        BootstrapViewModel.BootstrapState.CheckUpdate
                    ),
                    completedStates = loadingStates
                )
            }
        }
    }
}

@Composable
private fun SpotlightEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight")

    // More dramatic pulsing with wider range
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlightPulse"
    )

    // Subtle horizontal drift for more life
    val drift by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlightDrift"
    )

    // Radius breathing effect
    val radiusScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radiusBreathing"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f + drift
        val topY = size.height * 0.25f // Position spotlight to hit DOKUS area

        // Main spotlight cone from top - brighter and more dramatic
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFD4AF37).copy(alpha = pulseAlpha * 0.7f), // Gold center - much brighter
                    Color(0xFFFFE4A0).copy(alpha = pulseAlpha * 0.4f), // Light gold
                    Color(0xFFFFE4A0).copy(alpha = pulseAlpha * 0.15f), // Extended glow
                    Color.Transparent
                ),
                center = Offset(centerX, topY),
                radius = size.maxDimension * radiusScale
            )
        )

        // Secondary bright inner core
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFE4A0).copy(alpha = pulseAlpha * 0.8f),
                    Color(0xFFD4AF37).copy(alpha = pulseAlpha * 0.3f),
                    Color.Transparent
                ),
                center = Offset(centerX, topY),
                radius = size.maxDimension * 0.3f * radiusScale
            )
        )

        // Ambient outer glow that breathes
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = pulseAlpha * 0.25f),
                    Color(0xFFD4AF37).copy(alpha = pulseAlpha * 0.1f),
                    Color.Transparent
                ),
                center = Offset(centerX, topY),
                radius = size.maxDimension * 0.9f * radiusScale
            )
        )
    }
}

@Composable
private fun BootstrapStatesList(
    allStates: List<BootstrapViewModel.BootstrapState>,
    completedStates: List<BootstrapViewModel.BootstrapState>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        allStates.forEach { state ->
            val isActive = completedStates.contains(state)
            val isCurrentStep = completedStates.lastOrNull() == state

            BootstrapStateItem(
                state = state,
                isActive = isActive,
                isCurrentStep = isCurrentStep
            )
        }
    }
}

@Composable
private fun BootstrapStateItem(
    state: BootstrapViewModel.BootstrapState,
    isActive: Boolean,
    isCurrentStep: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stateGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Row(
        modifier = Modifier.width(300.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status indicator dot
        Box(
            modifier = Modifier.size(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                // Outer glow
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawCircle(
                        color = Color(0xFFD4AF37).copy(alpha = if (isCurrentStep) glowAlpha * 0.4f else 0.2f),
                        radius = if (isCurrentStep) 12.dp.toPx() else 8.dp.toPx()
                    )
                }

                // Inner dot
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(
                        color = Color(0xFFD4AF37).copy(alpha = if (isCurrentStep) 1f else 0.6f),
                        radius = 6.dp.toPx()
                    )
                }
            } else {
                // Inactive dot
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = 6.dp.toPx()
                    )
                }
            }
        }

        // State text
        Text(
            text = state.localized,
            color = when {
                isCurrentStep -> Color(0xFFD4AF37).copy(alpha = glowAlpha)
                isActive -> Color.White.copy(alpha = 0.7f)
                else -> Color.White.copy(alpha = 0.3f)
            },
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = if (isCurrentStep) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = if (isCurrentStep) 1.5.sp else 0.5.sp
            )
        )
    }
}

@Composable
private fun EnhancedFloatingBubbles() {
    // Increased particle count for fuller effect
    val bubbleCount = 100
    val bubbles = remember {
        List(bubbleCount) {
            Bubble(
                x = Random.nextFloat(),
                startY = Random.nextFloat(), // Start from anywhere on screen
                size = Random.nextFloat() * 3f + 0.5f, // Smaller particles (0.5-3.5px)
                speed = Random.nextFloat() * 0.5f + 0.2f, // Varied speeds for depth
                wobbleAmplitude = Random.nextFloat() * 25f + 10f,
                wobbleFrequency = Random.nextFloat() * 4f + 1f,
                delay = Random.nextFloat() * 20f,
                opacity = Random.nextFloat() * 0.3f + 0.15f, // More subtle opacity
                angle = Random.nextFloat() * 360f // Random initial angle for circular motion
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50000, easing = LinearEasing)
        ),
        label = "bubbleTime"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        bubbles.forEach { bubble ->
            val progress = ((time + bubble.delay) * bubble.speed) % 100f / 100f

            // Y position: float upward continuously
            val baseY = (bubble.startY + progress) % 1.2f
            val y = canvasHeight * baseY

            // X position: complex motion combining wobble and circular drift
            val wobblePhase = (time + bubble.delay) * bubble.wobbleFrequency * 0.05f
            val wobble = sin(wobblePhase) * bubble.wobbleAmplitude

            // Add subtle circular drift
            val circularPhase = (time + bubble.delay + bubble.angle) * 0.02f
            val circularDrift = cos(circularPhase) * 15f

            val x = (bubble.x * canvasWidth) + wobble + circularDrift

            // Smooth continuous fade based on distance from center and top
            val distanceFromCenter = kotlin.math.abs(x - canvasWidth / 2f) / (canvasWidth / 2f)
            val centerFade = 1f - (distanceFromCenter * 0.3f) // Brighter in center

            // Fade based on vertical position
            val verticalFade = when {
                baseY < 0.15f -> baseY / 0.15f // Fade in at bottom
                baseY > 1.0f -> kotlin.math.max(0f, (1.2f - baseY) / 0.2f) // Fade out at top
                else -> 1f
            }

            val alpha = centerFade * verticalFade * bubble.opacity

            // Add subtle speed variation based on size (smaller = faster)
            val sizeSpeedFactor = 1f + (1f - bubble.size / 3.5f) * 0.3f
            val adjustedY = y - (progress * 10f * sizeSpeedFactor)

            // Draw multi-layered bubble for depth

            // Outer glow (largest, most transparent)
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.06f),
                radius = bubble.size * 5f,
                center = Offset(x, adjustedY)
            )

            // Middle glow
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.12f),
                radius = bubble.size * 2.5f,
                center = Offset(x, adjustedY)
            )

            // Core glow with gold tint
            drawCircle(
                color = Color(0xFFD4AF37).copy(alpha = alpha * 0.08f),
                radius = bubble.size * 1.3f,
                center = Offset(x, adjustedY)
            )

            // Main bubble
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.4f),
                radius = bubble.size,
                center = Offset(x, adjustedY)
            )

            // Highlight for 3D effect
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.7f),
                radius = bubble.size * 0.35f,
                center = Offset(
                    x - bubble.size * 0.3f,
                    adjustedY - bubble.size * 0.3f
                )
            )

            // Subtle shadow/depth on opposite side
            drawCircle(
                color = Color(0xFF000000).copy(alpha = alpha * 0.08f),
                radius = bubble.size * 0.25f,
                center = Offset(
                    x + bubble.size * 0.35f,
                    adjustedY + bubble.size * 0.35f
                )
            )
        }
    }
}

private data class Bubble(
    val x: Float,
    val startY: Float,
    val size: Float,
    val speed: Float,
    val wobbleAmplitude: Float,
    val wobbleFrequency: Float,
    val delay: Float,
    val opacity: Float,
    val angle: Float
)
