package tech.dokus.app.screens

import ai.dokus.app.auth.screen.ProfileSettingsScreen
import ai.dokus.app.cashflow.screens.settings.PeppolSettingsScreen
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.settings_current_workspace
import ai.dokus.app.resources.generated.settings_select_hint
import ai.dokus.app.resources.generated.settings_select_prompt
import ai.dokus.app.resources.generated.settings_select_workspace
import ai.dokus.app.resources.generated.settings_unknown_section
import tech.dokus.foundation.aura.components.ListSettingsItem
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.SettingsDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.screens.settings.AppearanceSettingsContent
import tech.dokus.app.screens.settings.TeamSettingsContent
import tech.dokus.app.screens.settings.WorkspaceSettingsContent
import tech.dokus.app.settingsGroupsCombined
import tech.dokus.app.viewmodel.SettingsAction
import tech.dokus.app.viewmodel.SettingsContainer
import tech.dokus.app.viewmodel.SettingsIntent
import tech.dokus.app.viewmodel.SettingsState
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.ModuleSettingsSection
import tech.dokus.foundation.app.local.LocalAppModules
import tech.dokus.foundation.app.mvi.container
import tech.dokus.domain.exceptions.DokusException

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
            snackbarHostState = snackbarHostState
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
    var selectedSection by rememberSaveable(stateSaver = SettingsSectionSaver) {
        mutableStateOf(allSections.firstOrNull())
    }

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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
    snackbarHostState: SnackbarHostState
) {
    val navController = LocalNavController.current
    val appModules = LocalAppModules.current
    val settingsGroups = remember(appModules) { appModules.settingsGroupsCombined }

    // Extract tenant from state
    val currentTenant = (state as? SettingsState.Content)?.tenant
    val isLoading = state is SettingsState.Loading

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
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
    state: SettingsState,
    settingsGroups: Map<StringResource, List<ModuleSettingsGroup>>,
    selectedSection: ModuleSettingsSection?,
    onSectionSelected: (ModuleSettingsSection) -> Unit
) {
    val navController = LocalNavController.current

    // Extract tenant from state
    val currentTenant = (state as? SettingsState.Content)?.tenant
    val isLoading = state is SettingsState.Loading

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
                    // Use the full screen as embedded content for now
                    ProfileSettingsScreen()
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
                    // Use the full screen as embedded content for now
                    PeppolSettingsScreen()
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
private val SettingsSectionSaver = Saver<ModuleSettingsSection?, String>(
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
    restore = { _ ->
        // We can't restore the full section without access to the modules,
        // so we return null and let the default selection logic handle it
        null
    }
)
