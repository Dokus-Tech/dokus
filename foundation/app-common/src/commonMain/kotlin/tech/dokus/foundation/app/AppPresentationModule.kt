package tech.dokus.foundation.app

import org.koin.core.module.Module
import tech.dokus.navigation.NavigationProvider

interface AppPresentationModule {
    /** Primary navigation graph for the feature (main content area) */
    val navigationProvider: NavigationProvider?

    /** Navigation graph for home screen items */
    val homeNavigationProvider: NavigationProvider?

    /** Navigation groups contributed by this feature, merged by sectionId at the app level */
    val navGroups: List<ModuleNavGroup>

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
