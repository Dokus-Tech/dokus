package tech.dokus.foundation.app

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.koin.core.module.Module
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.NavigationDestination

/**
 * Defines a feature module in the Dokus application architecture.
 *
 * Each feature (auth, invoicing, expense, etc.) implements this interface to register navigation,
 * dependency injection modules, home items, settings, and dashboard widgets. Enables plugin-like
 * architecture where features are self-contained and independently manageable.
 */
interface AppModule : AppPresentationModule, AppDataModule, AppDomainModule

/**
 * Priority for settings groups, determining display order.
 */
enum class SettingsPriority(val order: Int) {
    High(0),
    Medium(1),
    Low(2)
}

@Immutable
data class ModuleSettingsGroup(
    val title: StringResource,
    val sections: List<ModuleSettingsSection>,
    val priority: SettingsPriority = SettingsPriority.Medium
)

@Immutable
data class ModuleSettingsSection(
    val title: StringResource,
    val icon: ImageVector,
    val destination: NavigationDestination
)

/** Navigation group contributed by a feature module. Same sectionId from different modules gets merged. */
@Immutable
data class ModuleNavGroup(
    val sectionId: String,
    val sectionTitle: StringResource,
    val sectionIcon: DrawableResource,
    val sectionOrder: Int = 0,
    val sectionDefaultExpanded: Boolean = false,
    val items: List<NavItem>,
)

val Collection<AppModule>.navigationProviders: List<NavigationProvider>
    get() = mapNotNull { it.navigationProvider }

val Collection<ModuleSettingsGroup>.allDestinations: List<NavigationDestination>
    get() = flatMap { it.sections.map { section -> section.destination } }

val AppModule.diModules: List<Module>
    get() = buildList {
        addAll(presentationDi.allModules)
        addAll(dataDi.allModules)
        addAll(domainDi.allModules)
    }
