package tech.dokus.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.viewmodel.BootstrapAction
import tech.dokus.app.viewmodel.BootstrapContainer
import tech.dokus.app.viewmodel.BootstrapIntent
import tech.dokus.app.viewmodel.BootstrapStep
import tech.dokus.app.viewmodel.BootstrapStepType
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.bootstrap_state_app_version_check
import tech.dokus.aura.resources.bootstrap_state_authenticating
import tech.dokus.aura.resources.bootstrap_state_checking_account_status
import tech.dokus.aura.resources.bootstrap_state_initializing
import tech.dokus.aura.resources.bootstrap_version_footer
import tech.dokus.aura.resources.subscription_tier_core
import tech.dokus.domain.config.appVersion
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.components.background.BootstrapBackground
import tech.dokus.foundation.aura.components.background.BootstrapBackgroundLayout
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.components.text.DokusLogo
import tech.dokus.foundation.aura.components.text.DokusLogoEmphasis
import tech.dokus.foundation.aura.style.dokusSpacing
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.replace
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val MainPathCompletionHold = 420.milliseconds
private val StepRevealDelay = 220.milliseconds
private val StepRevealDuration = 220.milliseconds
private val ActivePulseDuration = 1_400.milliseconds
private val ProgressAnimationDuration = 650.milliseconds
private val SettledTransitionDuration = 280.milliseconds
private val DotScaleTransitionDuration = 220.milliseconds
private const val BootstrapStepCount = 4
private val StepRowWidth = 340.dp
private val StepDotSize = 12.dp
private val StepRingSize = 18.dp
private val SplashLogoTopPaddingMobile = 56.dp
private val SplashLogoTopPaddingDesktop = 68.dp
private val SplashChecklistBottomPaddingMobile = 120.dp
private val SplashChecklistBottomPaddingDesktop = 136.dp
private fun Duration.toMillisInt(): Int = inWholeMilliseconds.toInt()

private val BootstrapStepType.localized: String
    @Composable get() = when (this) {
        BootstrapStepType.InitializeApp -> stringResource(Res.string.bootstrap_state_initializing)
        BootstrapStepType.CheckingLogin -> stringResource(Res.string.bootstrap_state_authenticating)
        BootstrapStepType.CheckUpdate -> stringResource(Res.string.bootstrap_state_app_version_check)
        BootstrapStepType.CheckingAccountStatus -> stringResource(Res.string.bootstrap_state_checking_account_status)
    }

@Composable
internal fun SplashRoute(
    container: BootstrapContainer = container()
) {
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    var mainNavigationScheduled by remember { mutableStateOf(false) }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            BootstrapAction.NavigateToLogin -> navController.replace(AuthDestination.Login)
            BootstrapAction.NavigateToUpdate -> navController.replace(CoreDestination.UpdateRequired)
            BootstrapAction.NavigateToAccountConfirmation -> navController.replace(
                AuthDestination.PendingConfirmAccount
            )
            BootstrapAction.NavigateToTenantSelection -> navController.replace(AuthDestination.WorkspaceSelect)
            BootstrapAction.NavigateToMain -> {
                if (!mainNavigationScheduled) {
                    mainNavigationScheduled = true
                    scope.launch {
                        delay(MainPathCompletionHold)
                        navController.replace(CoreDestination.Home)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        container.store.intent(BootstrapIntent.Load)
    }

    SplashScreen(
        steps = state.steps,
    )
}

@Composable
internal fun SplashScreen(
    steps: List<BootstrapStep>,
) {
    val spacing = MaterialTheme.dokusSpacing
    val isLargeScreen = LocalScreenSize.current.isLarge
    val backgroundLayout = if (isLargeScreen) {
        BootstrapBackgroundLayout.Desktop
    } else {
        BootstrapBackgroundLayout.Mobile
    }
    val logoTopPadding = if (isLargeScreen) SplashLogoTopPaddingDesktop else SplashLogoTopPaddingMobile
    val checklistBottomPadding = if (isLargeScreen) {
        SplashChecklistBottomPaddingDesktop
    } else {
        SplashChecklistBottomPaddingMobile
    }
    val completedSteps = steps.count { it.isActive && !it.isCurrent }
    val progressTarget = (completedSteps.toFloat() / BootstrapStepCount.toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = ProgressAnimationDuration.toMillisInt(), easing = FastOutSlowInEasing),
        label = "splashProgress",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        BootstrapBackground(
            progress = progress,
            layout = backgroundLayout,
        )

        DokusLogo.Full(
            emphasis = DokusLogoEmphasis.Hero,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = logoTopPadding),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = checklistBottomPadding),
        ) {
            BootstrapStatesList(steps = steps)
        }

        val tier = stringResource(Res.string.subscription_tier_core)
        val footerText = stringResource(Res.string.bootstrap_version_footer, appVersion.versionName, tier)

        Text(
            text = footerText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = spacing.xxLarge),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.14.em),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
        )
    }
}

@Composable
private fun BootstrapStatesList(
    steps: List<BootstrapStep>,
) {
    val spacing = MaterialTheme.dokusSpacing

    var visibleCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(steps.size) {
        visibleCount = 0
        repeat(steps.size) { index ->
            visibleCount = index + 1
            if (index < steps.lastIndex) {
                delay(StepRevealDelay)
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        steps.forEachIndexed { index, step ->
            AnimatedVisibility(
                visible = index < visibleCount,
                enter = fadeIn(animationSpec = tween(StepRevealDuration.toMillisInt(), easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = tween(StepRevealDuration.toMillisInt(), easing = FastOutSlowInEasing),
                    ) { it / 2 },
            ) {
                BootstrapStateItem(step = step)
            }
        }
    }
}

@Composable
private fun BootstrapStateItem(
    step: BootstrapStep,
) {
    val spacing = MaterialTheme.dokusSpacing
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val isCompleted = step.isActive && !step.isCurrent

    val pulseTransition = rememberInfiniteTransition(label = "statePulse")
    val ringScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ActivePulseDuration.toMillisInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringScale",
    )
    val ringAlpha by pulseTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ActivePulseDuration.toMillisInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringAlpha",
    )
    val currentTextPulse by pulseTransition.animateFloat(
        initialValue = 0.74f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ActivePulseDuration.toMillisInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "currentTextPulse",
    )

    val settledDotColor by animateColorAsState(
        targetValue = when {
            isCompleted -> accent.copy(alpha = 0.86f)
            else -> onSurface.copy(alpha = 0.20f)
        },
        animationSpec = tween(durationMillis = SettledTransitionDuration.toMillisInt(), easing = FastOutSlowInEasing),
        label = "settledDotColor",
    )
    val settledTextColor by animateColorAsState(
        targetValue = when {
            isCompleted -> onSurface.copy(alpha = 0.74f)
            else -> onSurface.copy(alpha = 0.36f)
        },
        animationSpec = tween(durationMillis = SettledTransitionDuration.toMillisInt(), easing = FastOutSlowInEasing),
        label = "settledTextColor",
    )
    val dotScale by animateFloatAsState(
        targetValue = when {
            step.isCurrent -> 1f
            isCompleted -> 0.96f
            else -> 0.90f
        },
        animationSpec = tween(durationMillis = DotScaleTransitionDuration.toMillisInt(), easing = FastOutSlowInEasing),
        label = "dotScale",
    )

    Row(
        modifier = Modifier.width(StepRowWidth),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.large),
    ) {
        Box(
            modifier = Modifier.size(StepRingSize),
            contentAlignment = Alignment.Center,
        ) {
            if (step.isCurrent) {
                Canvas(modifier = Modifier.size(StepRingSize)) {
                    drawCircle(
                        color = accent.copy(alpha = ringAlpha),
                        radius = (size.minDimension / 2f) * ringScale,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .size(StepDotSize)
                    .graphicsLayer(scaleX = dotScale, scaleY = dotScale),
            ) {
                drawCircle(
                    color = if (step.isCurrent) accent else settledDotColor,
                    radius = size.minDimension / 2f,
                )
            }
        }

        Text(
            text = step.type.localized,
            color = if (step.isCurrent) accent.copy(alpha = currentTextPulse) else settledTextColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (step.isCurrent) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Preview
@Composable
private fun SplashScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        SplashScreen(
            steps = listOf(
                BootstrapStep(BootstrapStepType.InitializeApp, isActive = true, isCurrent = false),
                BootstrapStep(BootstrapStepType.CheckUpdate, isActive = true, isCurrent = true),
                BootstrapStep(BootstrapStepType.CheckingLogin, isActive = false, isCurrent = false),
                BootstrapStep(BootstrapStepType.CheckingAccountStatus, isActive = false, isCurrent = false),
            ),
        )
    }
}
