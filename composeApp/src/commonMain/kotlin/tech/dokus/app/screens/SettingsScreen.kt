package tech.dokus.app.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Settings
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.screens.settings.route.AppearanceSettingsRoute
import tech.dokus.app.screens.settings.route.NotificationPreferencesRoute
import tech.dokus.app.screens.settings.route.TeamSettingsRoute
import tech.dokus.app.screens.settings.route.WorkspaceSettingsRoute
import tech.dokus.app.settingsGroupsCombined
import tech.dokus.app.viewmodel.SettingsAction
import tech.dokus.app.viewmodel.SettingsContainer
import tech.dokus.app.viewmodel.SettingsIntent
import tech.dokus.app.viewmodel.SettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.settings_current_workspace
import tech.dokus.aura.resources.settings_select_hint
import tech.dokus.aura.resources.settings_select_prompt
import tech.dokus.aura.resources.settings_select_workspace
import tech.dokus.aura.resources.settings_unknown_section
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.auth.presentation.auth.route.ProfileSettingsRoute
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.ModuleSettingsSection
import tech.dokus.foundation.app.local.LocalAppModules
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.ListSettingsItem
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

/**
 * Settings screen using FlowMVI Container pattern.
 * Displays settings navigation with split-pane (desktop) or list (mobile) layout.
 */
@Composable
internal fun SettingsScreen(
    container: SettingsContainer = container()
) {
    val screenSize = LocalScreenSize.current
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            SettingsAction.NavigateToWorkspaceSelect -> {
                navController.navigateTo(AuthDestination.WorkspaceSelect)
            }

            is SettingsAction.ShowError -> {
                pendingError = action.error
            }
        }
    }

    // Load tenant when screen appears
    LaunchedEffect(Unit) {
        container.store.intent(SettingsIntent.Load)
    }

    if (screenSize.isLarge) {
        // Desktop: Split-pane layout
        SettingsSplitPaneLayout(
            state = state,
            snackbarHostState = snackbarHostState
        )
    } else {
        // Mobile: Traditional navigation layout
        SettingsMobileLayout(
            state = state,
        )
    }
}

/**
 * Desktop split-pane layout with navigation on left and content on right.
 */
@Composable
private fun SettingsSplitPaneLayout(
    state: SettingsState,
    snackbarHostState: SnackbarHostState
) {
    val appModules = LocalAppModules.current
    val settingsGroups = remember(appModules) { appModules.settingsGroupsCombined }

    // Get all sections flattened for selection tracking
    val allSections = remember(settingsGroups) {
        settingsGroups.values.flatten().flatMap { it.sections }
    }

    // Track selected section (first section selected by default)
    var selectedSection by remember { mutableStateOf(allSections.firstOrNull()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Surface {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                // Left Navigation Panel - matches HomeScreen's RailNavigationLayout pattern
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    SettingsNavigationPanel(
                        state = state,
                        settingsGroups = settingsGroups,
                        selectedSection = selectedSection,
                        onSectionSelected = { selectedSection = it }
                    )
                }

                // Right Content Panel (fills remaining space)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.TopStart
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
}

/**
 * Mobile layout with traditional vertical list and navigation.
 */
@Composable
private fun SettingsMobileLayout(
    state: SettingsState,
) {
    val navController = LocalNavController.current
    val appModules = LocalAppModules.current
    val settingsGroups = remember(appModules) { appModules.settingsGroupsCombined }

    // Extract tenant from state
    val currentTenant = if (state.isSuccess()) state.data else null
    val isLoading = state.isLoading()

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
                workspaceName = currentTenant?.displayName?.value,
                isLoading = isLoading,
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
    state: SettingsState,
    settingsGroups: Map<StringResource, List<ModuleSettingsGroup>>,
    selectedSection: ModuleSettingsSection?,
    onSectionSelected: (ModuleSettingsSection) -> Unit
) {
    val navController = LocalNavController.current

    // Extract tenant from state
    val currentTenant = if (state.isSuccess()) state.data else null
    val isLoading = state.isLoading()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Workspace Picker
        WorkspacePickerCard(
            workspaceName = currentTenant?.displayName?.value,
            isLoading = isLoading,
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
                style = MaterialTheme.typography.headlineMedium,
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
                    // Use the full screen as embedded content for now
                    ProfileSettingsRoute()
                }

                is SettingsDestination.WorkspaceSettings -> {
                    WorkspaceSettingsRoute()
                }

                is SettingsDestination.TeamSettings -> {
                    TeamSettingsRoute()
                }

                is SettingsDestination.AppearanceSettings -> {
                    AppearanceSettingsRoute()
                }

                is SettingsDestination.NotificationPreferences -> {
                    NotificationPreferencesRoute()
                }

                else -> {
                    // Fallback for unknown destinations
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_unknown_section),
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
                text = stringResource(Res.string.settings_select_prompt),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.settings_select_hint),
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
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        padding = DokusCardPadding.Default,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    DokusLoader(size = DokusLoaderSize.Small)
                } else {
                    Text(
                        text = workspaceName
                            ?: stringResource(Res.string.settings_select_workspace),
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
    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
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
                    onClick = { onSectionClick(section) }
                )
            }
        }
    }
}

