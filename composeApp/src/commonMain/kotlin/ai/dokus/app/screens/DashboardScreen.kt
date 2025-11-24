package ai.dokus.app.screens

import ai.dokus.app.core.state.isLoading
import ai.dokus.app.core.state.isSuccess
import ai.dokus.app.viewmodel.DashboardViewModel
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import ai.dokus.foundation.design.components.PIconPosition
import ai.dokus.foundation.design.components.common.PSearchFieldCompact
import ai.dokus.foundation.design.components.common.PTopAppBarSearchAction
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    var searchQuery by remember { mutableStateOf("") }

    val currentOrganizationState by viewModel.currentOrganizationState.collectAsState()
    val currentOrganization = currentOrganizationState.let { if (it.isSuccess()) it.data else null }

    LaunchedEffect(viewModel) {
        viewModel.refreshOrganization()
    }

    Scaffold(
        topBar = {
            PTopAppBarSearchAction(
                searchContent = {
                    PSearchFieldCompact(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search..."
                    )
                },
                actions = {
                    PButton(
                        text = currentOrganization?.legalName?.value ?: "Select Organization",
                        variant = PButtonVariant.Outline,
                        icon = Icons.Default.SwitchAccount,
                        iconPosition = PIconPosition.Trailing,
                        isLoading = currentOrganizationState.isLoading(),
                        onClick = { navController.navigateTo(AuthDestination.CompanySelect) }
                    )
                }
            )
        }
    ) { _ ->
        // Content goes here
    }
}
