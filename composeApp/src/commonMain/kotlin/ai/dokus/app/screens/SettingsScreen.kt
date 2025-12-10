package ai.dokus.app.screens

import ai.dokus.app.core.ModuleSettingsGroup
import ai.dokus.app.core.ModuleSettingsSection
import ai.dokus.app.core.local.LocalAppModules
import ai.dokus.app.core.state.isLoading
import ai.dokus.app.core.state.isSuccess
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.settings_current_workspace
import ai.dokus.app.resources.generated.settings_select_workspace
import ai.dokus.app.settingsGroupsCombined
import ai.dokus.app.viewmodel.SettingsViewModel
import ai.dokus.foundation.design.components.ListSettingsItem
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val appModules = LocalAppModules.current
    val settingsGroups = remember(appModules) { appModules.settingsGroupsCombined }

    val currentTenantState by viewModel.currentTenantState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadCurrentTenant()
    }

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .withContentPaddingForScrollable()
        ) {
            // Workspace Picker Card
            WorkspacePickerCard(
                workspaceName = currentTenantState.let {
                    if (it.isSuccess()) it.data?.displayName?.value else null
                },
                isLoading = currentTenantState.isLoading(),
                onClick = {
                    navController.navigateTo(AuthDestination.WorkspaceSelect)
                }
            )

            Spacer(Modifier.height(24.dp))

            // Settings Groups
            settingsGroups.forEach { (groupTitle, groups) ->
                SettingsGroupCard(
                    title = groupTitle,
                    sections = groups.flatMap { it.sections },
                    onSectionClick = { section ->
                        navController.navigateTo(section.destination)
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WorkspacePickerCard(
    workspaceName: String?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_current_workspace),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).padding(top = 4.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = workspaceName ?: stringResource(Res.string.settings_select_workspace),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: StringResource,
    sections: List<ModuleSettingsSection>,
    onSectionClick: (ModuleSettingsSection) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            sections.forEach { section ->
                ListSettingsItem(
                    text = stringResource(section.title),
                    icon = section.icon,
                    isSelected = false,
                    onClick = { onSectionClick(section) }
                )
            }
        }
    }
}
