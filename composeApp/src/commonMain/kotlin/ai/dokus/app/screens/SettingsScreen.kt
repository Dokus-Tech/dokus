package ai.dokus.app.screens

import ai.dokus.app.auth.screen.ProfileSettingsContent
import ai.dokus.app.cashflow.screens.settings.PeppolSettingsContent
import ai.dokus.app.core.ModuleSettingsGroup
import ai.dokus.app.core.ModuleSettingsSection
import ai.dokus.app.core.local.LocalAppModules
import ai.dokus.app.core.state.isLoading
import ai.dokus.app.core.state.isSuccess
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.settings_current_workspace
import ai.dokus.app.resources.generated.settings_select_workspace
import ai.dokus.app.screens.settings.AppearanceSettingsContent
import ai.dokus.app.screens.settings.TeamSettingsContent
import ai.dokus.app.screens.settings.WorkspaceSettingsContent
import ai.dokus.app.settingsGroupsCombined
import ai.dokus.app.viewmodel.SettingsViewModel
import ai.dokus.foundation.design.components.ListSettingsItem
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.NavigationDestination
import ai.dokus.foundation.navigation.destinations.SettingsDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val screenSize = LocalScreenSize.current

    if (screenSize.isLarge) {
        // Desktop: Split-pane layout
        SettingsSplitPaneLayout(viewModel = viewModel)
    } else {
        // Mobile: Traditional navigation layout
        SettingsMobileLayout(viewModel = viewModel)
    }
}

/**
 * Desktop split-pane layout with navigation on left and content on right.
 */
@Composable
private fun SettingsSplitPaneLayout(
    viewModel: SettingsViewModel
) {
    val appModules = LocalAppModules.current
    val settingsGroups = remember(appModules) { appModules.settingsGroupsCombined }

    // Get all sections flattened for selection tracking
    val allSections = remember(settingsGroups) {
        settingsGroups.values.flatten().flatMap { it.sections }
    }

    // Track selected section (first section selected by default)
    var selectedSection by rememberSaveable(stateSaver = SettingsSectionSaver) {
        mutableStateOf(allSections.firstOrNull())
    }

    Scaffold { contentPadding ->
        Row(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            // Left Navigation Panel (320dp fixed width per UX specs)
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp
            ) {
                SettingsNavigationPanel(
                    viewModel = viewModel,
                    settingsGroups = settingsGroups,
                    selectedSection = selectedSection,
                    onSectionSelected = { selectedSection = it }
                )
            }

            // Divider between panes
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Right Content Panel (fills remaining space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                selectedSection?.let { section ->
                    SettingsContentPane(section = section)
                } ?: run {
                    // Empty state when no section selected
                    SettingsEmptyState()
                }
            }
        }
    }
}

/**
 * Mobile layout with traditional vertical list and navigation.
 */
@Composable
private fun SettingsMobileLayout(
    viewModel: SettingsViewModel
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
                    sections = groups.flatMap { group -> group.sections },
                    selectedSection = null, // No selection state on mobile
                    onSectionClick = { section ->
                        navController.navigateTo(section.destination)
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Navigation panel for desktop split-pane layout.
 */
@Composable
private fun SettingsNavigationPanel(
    viewModel: SettingsViewModel,
    settingsGroups: Map<StringResource, List<ModuleSettingsGroup>>,
    selectedSection: ModuleSettingsSection?,
    onSectionSelected: (ModuleSettingsSection) -> Unit
) {
    val navController = LocalNavController.current
    val currentTenantState by viewModel.currentTenantState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadCurrentTenant()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Workspace Picker
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

        // Settings Groups (flat list with headers)
        settingsGroups.forEach { (groupTitle, groups) ->
            // Group header
            Text(
                text = stringResource(groupTitle),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp)
            )

            // Section items
            groups.flatMap { group -> group.sections }.forEach { section ->
                val isSelected = selectedSection == section
                ListSettingsItem(
                    text = stringResource(section.title),
                    icon = section.icon,
                    isSelected = isSelected,
                    onClick = { onSectionSelected(section) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Content pane that renders the appropriate settings content based on destination.
 */
@Composable
private fun SettingsContentPane(
    section: ModuleSettingsSection
) {
    // Add a title header for the content pane
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Content header with section title
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Text(
                text = stringResource(section.title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(top = 24.dp, bottom = 16.dp)
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // Route to appropriate content composable based on destination
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (section.destination) {
                is AuthDestination.ProfileSettings -> {
                    ProfileSettingsContent()
                }
                is SettingsDestination.WorkspaceSettings -> {
                    WorkspaceSettingsContent()
                }
                is SettingsDestination.TeamSettings -> {
                    TeamSettingsContent()
                }
                is SettingsDestination.AppearanceSettings -> {
                    AppearanceSettingsContent()
                }
                is SettingsDestination.PeppolSettings -> {
                    PeppolSettingsContent()
                }
                else -> {
                    // Fallback for unknown destinations
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Settings content for ${section.destination::class.simpleName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state shown when no section is selected.
 */
@Composable
private fun SettingsEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "Select a setting to configure",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Choose from the options on the left",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
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
    selectedSection: ModuleSettingsSection?,
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
                    isSelected = selectedSection == section,
                    onClick = { onSectionClick(section) }
                )
            }
        }
    }
}

/**
 * Custom saver for ModuleSettingsSection to persist selection across configuration changes.
 * Uses the destination's serialized name as the key.
 */
private val SettingsSectionSaver = androidx.compose.runtime.saveable.Saver<ModuleSettingsSection?, String>(
    save = { section ->
        section?.destination?.let { dest ->
            when (dest) {
                is AuthDestination.ProfileSettings -> "profile"
                is SettingsDestination.WorkspaceSettings -> "workspace"
                is SettingsDestination.TeamSettings -> "team"
                is SettingsDestination.AppearanceSettings -> "appearance"
                is SettingsDestination.PeppolSettings -> "peppol"
                else -> null
            }
        }
    },
    restore = { key ->
        // We can't restore the full section without access to the modules,
        // so we return null and let the default selection logic handle it
        null
    }
)
