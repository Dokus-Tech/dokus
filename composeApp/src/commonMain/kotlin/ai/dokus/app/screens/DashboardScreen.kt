package ai.dokus.app.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun DashboardScreen() {
    val navController = LocalNavController.current
    var searchQuery by remember { mutableStateOf("") }

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
                        text = "Workspace 1",
                        variant = PButtonVariant.Outline,
                        icon = Icons.Default.SwitchAccount,
                        iconPosition = PIconPosition.Trailing,
                        onClick = { navController.navigateTo(AuthDestination.CompanySelect) }
                    )
                }
            )
        }
    ) { _ ->
        // Content goes here
    }
}