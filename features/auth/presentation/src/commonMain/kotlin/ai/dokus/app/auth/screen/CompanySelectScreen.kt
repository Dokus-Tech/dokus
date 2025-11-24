package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.CompanySelectLayout
import ai.dokus.app.auth.components.CompanySelection
import ai.dokus.app.auth.viewmodel.CompanySelectViewModel
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.SpotlightFollowEffect
import ai.dokus.foundation.design.constrains.isLargeScreen
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.navigation.replace
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private fun CompanySelectViewModel.Effect.handle(navController: NavController) {
    when (this) {
        is CompanySelectViewModel.Effect.CompanySelected -> {
            navController.replace(CoreDestination.Home)
        }

        is CompanySelectViewModel.Effect.SelectionFailed -> {}
    }
}

@Composable
internal fun CompanySelectScreen(
    viewModel: CompanySelectViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        launch { viewModel.effect.collect { it.handle(navController) } }
        viewModel.loadOrganizations()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        EnhancedFloatingBubbles()
        SpotlightFollowEffect()

        CompanySelection(
            state = state,
            layout = if (isLargeScreen) CompanySelectLayout.Desktop else CompanySelectLayout.Mobile,
            onCompanyClick = { viewModel.selectOrganization(it.id) },
            onAddCompanyClick = { navController.navigateTo(AuthDestination.CompanyCreate) }
        )
    }
}