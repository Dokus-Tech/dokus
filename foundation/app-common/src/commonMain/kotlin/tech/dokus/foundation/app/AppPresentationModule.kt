package tech.dokus.foundation.app

import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.navigation.NavigationProvider
import org.koin.core.module.Module

interface AppPresentationModule {
    /** Primary navigation graph for the feature (main content area) */
    val navigationProvider: NavigationProvider?

    /** Navigation graph for home screen items */
    val homeNavigationProvider: NavigationProvider?

    /** Quick-access items displayed on the home screen */
    val homeItems: List<HomeItem>

    /** Settings sections contributed by this feature */
    val settingsGroups: List<ModuleSettingsGroup>

    /** Dashboard widgets showing real-time data and metrics */
    val dashboardWidgets: List<DashboardWidget>

    val presentationDi: AppPresentationModuleDi
}

interface AppPresentationModuleDi {
    val viewModels: Module?
    val presentation: Module?
}

val AppPresentationModuleDi.allModules: List<Module>
    get() = listOfNotNull(
        viewModels,
        presentation
    )
