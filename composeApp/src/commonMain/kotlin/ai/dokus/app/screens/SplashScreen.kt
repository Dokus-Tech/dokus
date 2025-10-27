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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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

    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current

    LaunchedEffect("SplashScreen") {
//        scope.launch { viewModel.effect.collect { it.handle(navController) } }
        viewModel.load()
    }

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedLoadingStates(bootstrapStates = loadingStates)
        }
    }
}

@Composable
private fun AnimatedLoadingStates(
    bootstrapStates: List<BootstrapViewModel.BootstrapState>,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        bootstrapStates.forEachIndexed { index, state ->
            val isLatest = index == bootstrapStates.lastIndex

            // Latest state is fully visible, others fade out progressively
            val targetAlpha = when {
                isLatest -> 1f
                index == bootstrapStates.lastIndex - 1 -> 0.6f
                index == bootstrapStates.lastIndex - 2 -> 0.3f
                else -> 0.1f
            }

            val animatedAlpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = tween(durationMillis = 300),
                label = "LoadingStateAlpha"
            )

            val targetColor = if (isLatest) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            val animatedColor by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(durationMillis = 300),
                label = "LoadingStateColor"
            )

            Text(
                text = state.localized,
                color = animatedColor,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.alpha(animatedAlpha)
            )
        }
    }
}