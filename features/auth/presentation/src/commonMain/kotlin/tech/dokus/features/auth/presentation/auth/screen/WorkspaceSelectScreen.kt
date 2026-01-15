@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for UI constants (Kotlin convention)

package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.delay
import tech.dokus.features.auth.mvi.WorkspaceSelectIntent
import tech.dokus.features.auth.mvi.WorkspaceSelectState
import tech.dokus.features.auth.presentation.auth.components.WorkspaceSelectionBody
import tech.dokus.foundation.aura.components.background.CalmParticleField
import tech.dokus.foundation.aura.components.background.WarpJumpEffect
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.components.text.CopyRightText
import tech.dokus.foundation.aura.constrains.limitWidth
import tech.dokus.foundation.aura.constrains.withVerticalPadding

private const val BackgroundFadeOutDurationMs = 800
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
    // Warp animation state
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

    Scaffold { contentPadding ->
        Box(
            modifier = Modifier
                .padding(
                    bottom = contentPadding.calculateBottomPadding(),
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                    top = contentPadding.calculateTopPadding(),
                )
                .fillMaxSize()
        ) {
            // Background effects with fade animation
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(BackgroundFadeOutDurationMs))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CalmParticleField()
                }
            }

            // Main content with fade animation
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(ContentFadeOutDurationMs))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    WorkspaceSelectContent(
                        state = state,
                        onIntent = onIntent,
                        onAddTenantClick = onAddTenantClick,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Warp jump effect overlay
            WarpJumpEffect(
                isActive = isWarpActive,
                selectedItemPosition = selectedItemPosition,
                onAnimationComplete = {
                    shouldNavigate = true
                }
            )
        }
    }
}

@Composable
private fun WorkspaceSelectContent(
    state: WorkspaceSelectState,
    onIntent: (WorkspaceSelectIntent) -> Unit,
    onAddTenantClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .withVerticalPadding()
            .limitWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AppNameText()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WorkspaceSelectionBody(
                state = state,
                onTenantClick = { tenant ->
                    onIntent(WorkspaceSelectIntent.SelectTenant(tenant.id))
                },
                onAddTenantClick = onAddTenantClick
            )
        }

        CopyRightText()
    }
}
