@file:Suppress("TopLevelPropertyNaming")

package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import tech.dokus.features.auth.mvi.WorkspaceSelectIntent
import tech.dokus.features.auth.mvi.WorkspaceSelectState
import tech.dokus.features.auth.presentation.auth.components.WorkspaceSelectionBody
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingCenteredShell
import tech.dokus.foundation.aura.components.background.WarpJumpEffect

private const val ContentFadeOutDurationMs = 600
private const val NavigationDelayMs = 100L

@Composable
internal fun WorkspaceSelectScreen(
    state: WorkspaceSelectState,
    onIntent: (WorkspaceSelectIntent) -> Unit,
    onAddTenantClick: () -> Unit,
    triggerWarp: Boolean,
    onWarpComplete: () -> Unit,
) {
    var isWarpActive by remember { mutableStateOf(false) }
    var selectedItemPosition by remember { mutableStateOf<Offset?>(null) }
    var shouldNavigate by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(true) }

    LaunchedEffect(triggerWarp) {
        if (triggerWarp) {
            isWarpActive = true
            contentVisible = false
        }
    }

    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            delay(NavigationDelayMs)
            onWarpComplete()
        }
    }

    Scaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(ContentFadeOutDurationMs)),
            ) {
                OnboardingCenteredShell {
                    WorkspaceSelectionBody(
                        state = state,
                        onTenantClick = { tenant ->
                            onIntent(WorkspaceSelectIntent.SelectTenant(tenant.id))
                        },
                        onAddTenantClick = onAddTenantClick,
                    )
                }
            }

            WarpJumpEffect(
                isActive = isWarpActive,
                selectedItemPosition = selectedItemPosition,
                onAnimationComplete = {
                    shouldNavigate = true
                },
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun WorkspaceSelectScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        WorkspaceSelectScreen(
            state = WorkspaceSelectState.Content(data = emptyList()),
            onIntent = {},
            onAddTenantClick = {},
            triggerWarp = false,
            onWarpComplete = {},
        )
    }
}
