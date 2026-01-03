package tech.dokus.app.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.viewmodel.BootstrapAction
import tech.dokus.app.viewmodel.BootstrapContainer
import tech.dokus.app.viewmodel.BootstrapIntent
import tech.dokus.app.viewmodel.BootstrapStep
import tech.dokus.app.viewmodel.BootstrapStepType
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.app_name
import tech.dokus.aura.resources.bootstrap_state_app_version_check
import tech.dokus.aura.resources.bootstrap_state_authenticating
import tech.dokus.aura.resources.bootstrap_state_checking_account_status
import tech.dokus.aura.resources.bootstrap_state_initializing
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.components.background.EnhancedFloatingBubbles
import tech.dokus.foundation.aura.components.background.SpotlightEffect
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.replace

private const val GoldAccentHex = 0xFFD4AF37
private val GoldAccent = Color(GoldAccentHex)

private val BootstrapStepType.localized: String
    @Composable get() = when (this) {
        BootstrapStepType.InitializeApp -> stringResource(Res.string.bootstrap_state_initializing)
        BootstrapStepType.CheckingLogin -> stringResource(Res.string.bootstrap_state_authenticating)
        BootstrapStepType.CheckUpdate -> stringResource(Res.string.bootstrap_state_app_version_check)
        BootstrapStepType.CheckingAccountStatus -> stringResource(Res.string.bootstrap_state_checking_account_status)
    }

/**
 * Splash screen using FlowMVI Container pattern.
 * Handles app initialization and navigation to appropriate destination.
 */
@Composable
internal fun SplashScreen(
    container: BootstrapContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            BootstrapAction.NavigateToLogin -> navController.replace(AuthDestination.Login)
            BootstrapAction.NavigateToUpdate -> navController.replace(CoreDestination.UpdateRequired)
            BootstrapAction.NavigateToAccountConfirmation -> navController.replace(
                AuthDestination.PendingConfirmAccount
            )
            BootstrapAction.NavigateToTenantSelection -> navController.replace(AuthDestination.WorkspaceSelect)
            BootstrapAction.NavigateToMain -> navController.replace(CoreDestination.Home)
        }
    }

    // Start bootstrap process when screen appears
    LaunchedEffect(Unit) {
        container.store.intent(BootstrapIntent.Load)
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
                    text = stringResource(Res.string.app_name),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    style = TextStyle(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(80.dp))

                // Bootstrap states as vertical list
                BootstrapStatesList(state.steps)
            }
        }
    }
}

@Composable
private fun BootstrapStatesList(
    steps: List<BootstrapStep>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        steps.forEach { step ->
            BootstrapStateItem(
                step = step,
                isActive = step.isActive,
                isCurrentStep = step.isCurrent
            )
        }
    }
}

@Composable
private fun BootstrapStateItem(
    step: BootstrapStep,
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
                        color = GoldAccent.copy(alpha = if (isCurrentStep) glowAlpha * 0.4f else 0.2f),
                        radius = if (isCurrentStep) 12.dp.toPx() else 8.dp.toPx()
                    )
                }

                // Inner dot
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(
                        color = GoldAccent.copy(alpha = if (isCurrentStep) 1f else 0.6f),
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
            text = step.type.localized,
            color = when {
                isCurrentStep -> GoldAccent.copy(alpha = glowAlpha)
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
