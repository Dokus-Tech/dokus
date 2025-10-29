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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import kotlin.random.Random

/**
 * Handles bootstrap navigation effects.
 *
 * Bootstrap ALWAYS completes and navigates to the appropriate destination.
 * After bootstrap navigation completes, any cached deep link is processed,
 * navigating from the bootstrap destination to the deep link destination.
 *
 * Back stack: Splash → BootstrapDestination → DeepLinkDestination
 */
private fun BootstrapViewModel.Effect.handle(navController: NavController) {
    when (this) {
        is BootstrapViewModel.Effect.Idle -> Unit

        is BootstrapViewModel.Effect.NeedsUpdate -> {
            navController.replace(CoreDestination.UpdateRequired)
        }

        is BootstrapViewModel.Effect.NeedsLogin -> {
            navController.replace(AuthDestination.Login)
        }

        is BootstrapViewModel.Effect.NeedsAccountConfirmation -> {
            navController.replace(AuthDestination.PendingConfirmAccount)
        }

        is BootstrapViewModel.Effect.Ok -> {
            navController.replace(CoreDestination.Home)
        }
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
        // Mysterious, atmospheric splash with floating particles and ethereal animations
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            // Deep mysterious background with animated elements
            MysteriousBackground()

            // Floating mystical particles
            FloatingParticles()

            // Foreground content
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Financial data grid pattern
                FinancialGridPattern(
                    sizeDp = 200.dp
                )
                Spacer(modifier = Modifier.height(32.dp))
                MysteriousLoadingStatus(bootstrapStates = loadingStates)
            }
        }
    }
}

@Composable
private fun MysteriousLoadingStatus(
    bootstrapStates: List<BootstrapViewModel.BootstrapState>,
    modifier: Modifier = Modifier
) {
    val latest = if (bootstrapStates.isNotEmpty()) bootstrapStates.last() else null

    // Pulsing opacity animation
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusAlpha"
    )

    if (latest != null) {
        Text(
            text = latest.localized,
            color = Color(0xFF9D8CFF).copy(alpha = alpha), // Mystical purple
            style = MaterialTheme.typography.bodyMedium.merge(
                TextStyle(
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp
                )
            ),
            modifier = modifier
        )
    }
}

@Composable
private fun MysteriousBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bgPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgPulseAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0F))) {
        // Deep space-like background
        val deepPurple = Color(0xFF1A0F2E)
        val deepBlue = Color(0xFF0F1A2E)
        val mysticalViolet = Color(0xFF2D1B4E)

        // Mysterious gradient layers
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    mysticalViolet.copy(alpha = pulseAlpha),
                    deepPurple.copy(alpha = pulseAlpha * 0.7f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.3f, size.height * 0.4f),
                radius = size.maxDimension * 0.6f
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    deepBlue.copy(alpha = pulseAlpha * 0.8f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.7f, size.height * 0.6f),
                radius = size.maxDimension * 0.5f
            )
        )

        // Atmospheric vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                center = center,
                radius = size.maxDimension * 0.8f
            )
        )
    }
}

@Composable
private fun FinancialGridPattern(
    sizeDp: androidx.compose.ui.unit.Dp
) {
    val transition = rememberInfiniteTransition(label = "financialGrid")

    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val dataFlowProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing)
        ),
        label = "dataFlow"
    )

    Canvas(modifier = Modifier.size(sizeDp)) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val gridSize = size.minDimension

        // Financial color palette - sharp, professional
        val accentCyan = Color(0xFF5BCCFF)
        val accentPurple = Color(0xFF9D8CFF)
        val accentGreen = Color(0xFF5BFF9D)

        // Draw hexagonal grid nodes
        val hexRadius = gridSize * 0.15f
        val hexPositions = listOf(
            Offset(centerX, centerY - hexRadius * 1.8f), // Top
            Offset(centerX - hexRadius * 1.5f, centerY - hexRadius * 0.9f), // Top-left
            Offset(centerX + hexRadius * 1.5f, centerY - hexRadius * 0.9f), // Top-right
            Offset(centerX, centerY), // Center
            Offset(centerX - hexRadius * 1.5f, centerY + hexRadius * 0.9f), // Bottom-left
            Offset(centerX + hexRadius * 1.5f, centerY + hexRadius * 0.9f), // Bottom-right
            Offset(centerX, centerY + hexRadius * 1.8f) // Bottom
        )

        // Draw connecting lines between nodes (like transaction flows)
        val connections = listOf(
            0 to 1, 0 to 2, // Top to sides
            1 to 3, 2 to 3, // Sides to center
            3 to 4, 3 to 5, // Center to bottom sides
            4 to 6, 5 to 6  // Bottom sides to bottom
        )

        connections.forEach { (from, to) ->
            val progress = ((dataFlowProgress * connections.size) - connections.indexOf(from to to)) % 1f
            if (progress in 0f..1f) {
                val startPos = hexPositions[from]
                val endPos = hexPositions[to]
                val currentEnd = Offset(
                    startPos.x + (endPos.x - startPos.x) * progress,
                    startPos.y + (endPos.y - startPos.y) * progress
                )

                drawLine(
                    color = accentCyan.copy(alpha = pulseAlpha * 0.5f),
                    start = startPos,
                    end = currentEnd,
                    strokeWidth = 2f
                )

                // Data flow indicator
                drawCircle(
                    color = accentCyan.copy(alpha = pulseAlpha),
                    radius = 4f,
                    center = currentEnd
                )
            } else {
                // Static line when no data flowing
                drawLine(
                    color = accentCyan.copy(alpha = 0.15f),
                    start = hexPositions[from],
                    end = hexPositions[to],
                    strokeWidth = 1f
                )
            }
        }

        // Draw hexagonal nodes
        hexPositions.forEachIndexed { index, position ->
            val nodeAlpha = when (index) {
                3 -> pulseAlpha // Center node pulses strongest
                else -> pulseAlpha * 0.7f
            }

            // Outer glow
            drawCircle(
                color = accentPurple.copy(alpha = nodeAlpha * 0.2f),
                radius = hexRadius * 0.35f,
                center = position
            )

            // Node core
            drawCircle(
                color = accentPurple.copy(alpha = nodeAlpha * 0.8f),
                radius = hexRadius * 0.15f,
                center = position
            )

            // Sharp edge highlight
            drawCircle(
                color = Color.White.copy(alpha = nodeAlpha * 0.6f),
                radius = hexRadius * 0.08f,
                center = position
            )
        }

        // Corner accent lines - suggesting data boundaries
        val cornerLength = gridSize * 0.12f
        val cornerOffset = gridSize * 0.4f
        val cornerPositions = listOf(
            Offset(centerX - cornerOffset, centerY - cornerOffset),
            Offset(centerX + cornerOffset, centerY - cornerOffset),
            Offset(centerX - cornerOffset, centerY + cornerOffset),
            Offset(centerX + cornerOffset, centerY + cornerOffset)
        )

        cornerPositions.forEachIndexed { index, pos ->
            val xDir = if (index % 2 == 0) 1f else -1f
            val yDir = if (index < 2) 1f else -1f

            // Horizontal line
            drawLine(
                color = accentGreen.copy(alpha = pulseAlpha * 0.4f),
                start = pos,
                end = Offset(pos.x + cornerLength * xDir, pos.y),
                strokeWidth = 2f
            )

            // Vertical line
            drawLine(
                color = accentGreen.copy(alpha = pulseAlpha * 0.4f),
                start = pos,
                end = Offset(pos.x, pos.y + cornerLength * yDir),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun FloatingParticles() {
    // Create mystical floating particles
    val particleCount = 20
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 3f + 1f,
                speed = Random.nextFloat() * 0.5f + 0.2f,
                phase = Random.nextFloat() * 360f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing)
        ),
        label = "particleTime"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val offsetY = ((time * particle.speed + particle.phase) % 360f) / 360f
            val x = particle.x * size.width
            val y = (particle.y + offsetY) % 1f * size.height

            // Particle glow
            drawCircle(
                color = Color(0xFF9D8CFF).copy(alpha = 0.15f),
                radius = particle.size * 3f,
                center = Offset(x, y)
            )

            // Particle core
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = particle.size,
                center = Offset(x, y)
            )
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val phase: Float
)