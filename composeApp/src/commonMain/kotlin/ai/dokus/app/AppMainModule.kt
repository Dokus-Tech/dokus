package ai.dokus.app

import ai.dokus.app.core.AppDataModuleDi
import ai.dokus.app.core.AppDomainModuleDi
import ai.dokus.app.core.AppModule
import ai.dokus.app.core.AppPresentationModuleDi
import ai.dokus.app.core.DashboardWidget
import ai.dokus.app.core.ModuleSettingsGroup
import ai.dokus.app.navigation.AppNavigationProvider
import ai.dokus.app.navigation.HomeNavigationProvider
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.design.model.HomeItemPriority
import ai.dokus.foundation.navigation.destinations.HomeDestination
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.home_dashboard
import ai.dokus.app.resources.generated.home_settings

object AppMainModule : AppModule {
    // Presentation layer
    override val navigationProvider = AppNavigationProvider
    override val homeNavigationProvider = HomeNavigationProvider
    override val homeItems: List<HomeItem> = listOf(
        HomeItem(
            title = Res.string.home_dashboard,
            icon = Icons.Default.Dashboard,
            destination = HomeDestination.Dashboard,
            priority = HomeItemPriority.High,
        ),
        HomeItem(
            title = Res.string.home_settings,
            icon = Icons.Default.Settings,
            destination = HomeDestination.Settings,
            showTopBar = true,
            priority = HomeItemPriority.Low,
        ),
    )
    override val settingsGroups: List<ModuleSettingsGroup> = emptyList()
    override val dashboardWidgets: List<DashboardWidget> = emptyList()

    override val presentationDi: AppPresentationModuleDi = object : AppPresentationModuleDi {
        override val viewModels = diModuleApp
        override val presentation = null
    }

    // Data layer
    override val dataDi: AppDataModuleDi = object : AppDataModuleDi {
        override val platform = null
        override val network = null
        override val data = null
    }

    // Domain layer
    override val domainDi: AppDomainModuleDi = object : AppDomainModuleDi {
        override val useCases = diModuleUseCases
    }
}