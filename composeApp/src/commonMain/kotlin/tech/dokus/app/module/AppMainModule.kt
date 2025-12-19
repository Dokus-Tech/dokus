package tech.dokus.app.module

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.chart_bar_trend_up
import ai.dokus.app.resources.generated.home_dashboard
import ai.dokus.app.resources.generated.home_settings
import ai.dokus.app.resources.generated.settings_appearance
import ai.dokus.app.resources.generated.settings_group_app
import ai.dokus.app.resources.generated.settings_group_workspace
import ai.dokus.app.resources.generated.settings_team
import ai.dokus.app.resources.generated.settings_workspace_details
import ai.dokus.app.resources.generated.user
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.design.model.HomeItemPriority
import ai.dokus.foundation.navigation.destinations.HomeDestination
import ai.dokus.foundation.navigation.destinations.SettingsDestination
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Palette
import tech.dokus.app.diModuleApp
import tech.dokus.app.diModuleUseCases
import tech.dokus.app.navigation.AppNavigationProvider
import tech.dokus.app.navigation.HomeNavigationProvider
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.AppDomainModuleDi
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.AppPresentationModuleDi
import tech.dokus.foundation.app.DashboardWidget
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.ModuleSettingsSection
import tech.dokus.foundation.app.SettingsPriority

internal object AppMainModule : AppModule {
    // Presentation layer
    override val navigationProvider = AppNavigationProvider
    override val homeNavigationProvider = HomeNavigationProvider
    override val homeItems: List<HomeItem> = listOf(
        HomeItem(
            titleRes = Res.string.home_dashboard,
            iconRes = Res.drawable.chart_bar_trend_up,
            destination = HomeDestination.Dashboard,
            priority = HomeItemPriority.High,
            showTopBar = false
        ),
        HomeItem(
            titleRes = Res.string.home_settings,
            iconRes = Res.drawable.user,
            destination = HomeDestination.Settings,
            showTopBar = true,
            priority = HomeItemPriority.Low,
        ),
    )
    override val settingsGroups: List<ModuleSettingsGroup> = listOf(
        ModuleSettingsGroup(
            title = Res.string.settings_group_workspace,
            priority = SettingsPriority.Medium,
            sections = listOf(
                ModuleSettingsSection(
                    title = Res.string.settings_workspace_details,
                    icon = Icons.Default.Business,
                    destination = SettingsDestination.WorkspaceSettings
                ),
                ModuleSettingsSection(
                    title = Res.string.settings_team,
                    icon = Icons.Default.Group,
                    destination = SettingsDestination.TeamSettings
                )
            )
        ),
        ModuleSettingsGroup(
            title = Res.string.settings_group_app,
            priority = SettingsPriority.Low,
            sections = listOf(
                ModuleSettingsSection(
                    title = Res.string.settings_appearance,
                    icon = Icons.Default.Palette,
                    destination = SettingsDestination.AppearanceSettings
                )
            )
        )
    )
    override val dashboardWidgets: List<DashboardWidget> = emptyList()

    override val presentationDi: AppPresentationModuleDi = object : AppPresentationModuleDi {
        override val viewModels = diModuleApp
        override val presentation = null
    }

    // Data layer
    override val dataDi: AppDataModuleDi = AppDataMainModuleDi

    // Domain layer
    override val domainDi: AppDomainModuleDi = object : AppDomainModuleDi {
        override val useCases = diModuleUseCases
    }
}