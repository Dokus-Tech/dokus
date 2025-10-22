package ai.dokus.app.core

import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.NavigationDestination
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.koin.core.module.Module

interface AppModule {
    val navigationProvider: NavigationProvider?
    val homeNavigationProvider: NavigationProvider?
    val diModules: List<Module>
    val homeItems: List<HomeItem>
    val settingsGroups: List<ModuleSettingsGroup>
    val dashboardWidgets: List<DashboardWidget>
}

@Immutable
data class ModuleSettingsGroup(
    val title: StringResource,
    val sections: List<ModuleSettingsSection>
)

@Immutable
data class ModuleSettingsSection(
    val title: StringResource,
    val icon: ImageVector,
    val destination: NavigationDestination
)

val Collection<AppModule>.navigationProviders: List<NavigationProvider>
    get() = mapNotNull { it.navigationProvider }


val Collection<ModuleSettingsGroup>.allDestinations: List<NavigationDestination>
    get() = flatMap { it.sections.map { section -> section.destination } }