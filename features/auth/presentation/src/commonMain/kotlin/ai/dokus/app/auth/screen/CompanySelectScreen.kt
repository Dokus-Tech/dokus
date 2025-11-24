package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.CompanySelectLayout
import ai.dokus.app.auth.components.CompanySelection
import ai.dokus.app.auth.viewmodel.CompanySelectViewModel
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.WarpJumpEffect
import ai.dokus.foundation.design.constrains.isLargeScreen
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.navigation.replace
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CompanySelectScreen(
    viewModel: CompanySelectViewModel = koinViewModel()
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
                    is CompanySelectViewModel.Effect.CompanySelected -> {
                        // Trigger warp animation instead of immediate navigation
                        isWarpActive = true
                        contentVisible = false
                    }

                    is CompanySelectViewModel.Effect.SelectionFailed -> {}
                }
            }
        }
        viewModel.loadOrganizations()
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            CompanySelection(
                state = state,
                layout = if (isLargeScreen) CompanySelectLayout.Desktop else CompanySelectLayout.Mobile,
                onCompanyClick = { organization ->
                    // Note: We'd ideally capture the click position here
                    // For now, warp will start from center
                    viewModel.selectOrganization(organization.id)
                },
                onAddCompanyClick = { navController.navigateTo(AuthDestination.CompanyCreate) }
            )
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