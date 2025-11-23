package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.CompanySelectLayout
import ai.dokus.app.auth.components.CompanySelection
import ai.dokus.app.auth.viewmodel.CompanySelectViewModel
import ai.dokus.foundation.design.constrains.isLargeScreen
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.navigation.replace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CompanySelectScreen(
    viewModel: CompanySelectViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadOrganizations()
    }

    CompanySelection(
        state = state,
        layout = if (isLargeScreen) CompanySelectLayout.Desktop else CompanySelectLayout.Mobile,
        onCompanyClick = { navController.replace(CoreDestination.Home) },
        onAddCompanyClick = { navController.navigateTo(AuthDestination.CompanyCreate) }
    )
}