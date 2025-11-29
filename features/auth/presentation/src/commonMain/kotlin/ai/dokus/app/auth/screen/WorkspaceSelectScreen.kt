package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.WorkspaceSelectionBody
import ai.dokus.app.auth.viewmodel.WorkspaceSelectViewModel
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.WarpJumpEffect
import ai.dokus.foundation.design.components.text.AppNameText
import ai.dokus.foundation.design.components.text.CopyRightText
import ai.dokus.foundation.design.constrains.limitWidth
import ai.dokus.foundation.design.constrains.withVerticalPadding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun WorkspaceSelectScreen(
    viewModel: WorkspaceSelectViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsState()

    // Warp animation state
    var isWarpActive by remember { mutableStateOf(false) }
    var selectedItemPosition by remember { mutableStateOf<Offset?>(null) }
    var shouldNavigate by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(true) }

    // Handle navigation after warp animation
    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            delay(100) // Small delay for smooth transition
            navController.replace(CoreDestination.Home)
        }
    }

    LaunchedEffect(viewModel) {
        launch {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is WorkspaceSelectViewModel.Effect.WorkspaceSelected -> {
                        // Trigger warp animation instead of immediate navigation
                        isWarpActive = true
                        contentVisible = false
                    }

                    is WorkspaceSelectViewModel.Effect.SelectionFailed -> {}
                }
            }
        }
        viewModel.loadTenants()
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
                    Column(
                        modifier = Modifier
                            .withVerticalPadding()
                            .limitWidth()
                            .fillMaxHeight()
                            .align(Alignment.Center),
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
                                    viewModel.selectTenant(tenant.id)
                                },
                                onAddTenantClick = { navController.navigateTo(AuthDestination.WorkspaceCreate) }
                            )
                        }

                        CopyRightText()
                    }
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
