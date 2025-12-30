package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.WorkspaceSelectionBody
import ai.dokus.app.auth.viewmodel.WorkspaceSelectAction
import ai.dokus.app.auth.viewmodel.WorkspaceSelectContainer
import ai.dokus.app.auth.viewmodel.WorkspaceSelectIntent
import ai.dokus.app.auth.viewmodel.WorkspaceSelectState
import tech.dokus.foundation.aura.components.background.EnhancedFloatingBubbles
import tech.dokus.foundation.aura.components.background.WarpJumpEffect
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.components.text.CopyRightText
import tech.dokus.foundation.aura.constrains.limitWidth
import tech.dokus.foundation.aura.constrains.withVerticalPadding
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.navigation.replace
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
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.foundation.app.mvi.container

@Composable
internal fun WorkspaceSelectScreen(
    container: WorkspaceSelectContainer = container()
) {
    val navController = LocalNavController.current

    // Warp animation state
    var isWarpActive by remember { mutableStateOf(false) }
    var selectedItemPosition by remember { mutableStateOf<Offset?>(null) }
    var shouldNavigate by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(true) }

    with(container.store) {
        val state by subscribe { action ->
            when (action) {
                WorkspaceSelectAction.NavigateToHome -> {
                    // Trigger warp animation instead of immediate navigation
                    isWarpActive = true
                    contentVisible = false
                }

                is WorkspaceSelectAction.ShowSelectionError -> {
                    // Handle selection error - could show snackbar
                }
            }
        }

        // Handle navigation after warp animation
        LaunchedEffect(shouldNavigate) {
            if (shouldNavigate) {
                delay(100) // Small delay for smooth transition
                navController.replace(CoreDestination.Home)
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
                    exit = fadeOut(animationSpec = tween(800))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EnhancedFloatingBubbles()
                    }
                }

                // Main content with fade animation
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = tween(600))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        WorkspaceSelectContent(
                            state = state,
                            onAddTenantClick = { navController.navigateTo(AuthDestination.WorkspaceCreate) },
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
}

@Composable
private fun IntentReceiver<WorkspaceSelectIntent>.WorkspaceSelectContent(
    state: WorkspaceSelectState,
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
                    intent(WorkspaceSelectIntent.SelectTenant(tenant.id))
                },
                onAddTenantClick = onAddTenantClick
            )
        }

        CopyRightText()
    }
}
