package ai.dokus.app.screens

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.bootstrap_state_app_version_check
import ai.dokus.app.resources.generated.bootstrap_state_authenticating
import ai.dokus.app.resources.generated.bootstrap_state_checking_account_status
import ai.dokus.app.resources.generated.bootstrap_state_initializing
import ai.dokus.app.viewmodel.BootstrapViewModel
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.SpotlightEffect
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.replace
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Handles bootstrap navigation effects.
 */
private fun BootstrapViewModel.Effect.handle(navController: NavController) {
    when (this) {
        is BootstrapViewModel.Effect.Idle -> Unit
        is BootstrapViewModel.Effect.NeedsUpdate -> navController.replace(CoreDestination.UpdateRequired)
        is BootstrapViewModel.Effect.NeedsLogin -> navController.replace(AuthDestination.Login)
        is BootstrapViewModel.Effect.NeedsAccountConfirmation -> navController.replace(
            AuthDestination.PendingConfirmAccount
        )
        is BootstrapViewModel.Effect.Ok -> navController.replace(CoreDestination.Home)
    }
}

private val BootstrapViewModel.BootstrapState.localized: String
    @Composable get() = when (this) {
        is BootstrapViewModel.BootstrapState.InitializeApp -> stringResource(Res.string.bootstrap_state_initializing)
        is BootstrapViewModel.BootstrapState.CheckingLogin -> stringResource(Res.string.bootstrap_state_authenticating)
        is BootstrapViewModel.BootstrapState.CheckUpdate -> stringResource(Res.string.bootstrap_state_app_version_check)
        is BootstrapViewModel.BootstrapState.CheckingAccountStatus -> stringResource(Res.string.bootstrap_state_checking_account_status)
    }

@Composable
fun SplashScreen(
    viewModel: BootstrapViewModel = koinViewModel(),
) {
    val loadingStates by viewModel.state.collectAsState()
    val navController = LocalNavController.current

    // Collect navigation effects and start bootstrap
    LaunchedEffect("SplashScreen") {
        launch { viewModel.effect.collect { it.handle(navController) } }
        viewModel.load()
    }

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
                BootstrapStatesList(loadingStates)
            }
        }
    }
}

@Composable
private fun BootstrapStatesList(
    states: List<BootstrapViewModel.BootstrapState>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        states.forEach { state ->
            BootstrapStateItem(
                state = state,
                isActive = state.isActive,
                isCurrentStep = state.isCurrent
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