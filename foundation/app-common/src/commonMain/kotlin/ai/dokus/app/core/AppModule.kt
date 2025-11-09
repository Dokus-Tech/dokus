package ai.dokus.app.core

import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.NavigationDestination
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.koin.core.module.Module

/**
 * Defines a feature module in the Dokus application architecture.
 *
 * Each feature (auth, invoicing, expense, etc.) implements this interface to register navigation,
 * dependency injection modules, home items, settings, and dashboard widgets. Enables plugin-like
 * architecture where features are self-contained and independently manageable.
 */
interface AppModule : AppPresentationModule, AppDataModule, AppDomainModule

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

val AppModule.diModules: List<Module>
    get() = buildList {
        addAll(presentationDi.allModules)
        addAll(dataDi.allModules)
        addAll(domainDi.allModules)
    }