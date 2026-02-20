package tech.dokus.app.module

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import tech.dokus.app.diModuleApp
import tech.dokus.app.diModuleUseCases
import tech.dokus.app.navigation.AppNavigationProvider
import tech.dokus.app.navigation.HomeNavigationProvider
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.bar_chart
import tech.dokus.aura.resources.calculator
import tech.dokus.aura.resources.chart_bar_trend_up
import tech.dokus.aura.resources.file_text
import tech.dokus.aura.resources.home_today
import tech.dokus.aura.resources.nav_accountant
import tech.dokus.aura.resources.nav_documents
import tech.dokus.aura.resources.nav_reports
import tech.dokus.aura.resources.nav_section_accounting
import tech.dokus.aura.resources.nav_section_company
import tech.dokus.aura.resources.nav_team
import tech.dokus.aura.resources.nav_vat
import tech.dokus.aura.resources.settings_appearance
import tech.dokus.aura.resources.settings_group_account
import tech.dokus.aura.resources.settings_group_app
import tech.dokus.aura.resources.settings_group_workspace
import tech.dokus.aura.resources.settings_notifications
import tech.dokus.aura.resources.settings_team
import tech.dokus.aura.resources.settings_workspace_details
import tech.dokus.aura.resources.user
import tech.dokus.aura.resources.users
import tech.dokus.aura.resources.wallet_2
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.AppDomainModuleDi
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.AppPresentationModuleDi
import tech.dokus.foundation.app.DashboardWidget
import tech.dokus.foundation.app.ModuleNavGroup
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.ModuleSettingsSection
import tech.dokus.foundation.app.SettingsPriority
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.ShellTopBarDefault
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination

internal object AppMainModule : AppModule {
    // Presentation layer
    override val navigationProvider = AppNavigationProvider
    override val homeNavigationProvider = HomeNavigationProvider
    override val navGroups: List<ModuleNavGroup> = listOf(
        ModuleNavGroup(
            sectionId = "accounting",
            sectionTitle = Res.string.nav_section_accounting,
            sectionIcon = Res.drawable.chart_bar_trend_up,
            sectionOrder = 0,
            sectionDefaultExpanded = true,
            items = listOf(
                NavItem(
                    id = "today",
                    titleRes = Res.string.home_today,
                    iconRes = Res.drawable.chart_bar_trend_up,
                    destination = HomeDestination.Today,
                    priority = 0,
                    mobileTabOrder = 0,
                    shellTopBar = ShellTopBarDefault.Search,
                ),
                NavItem(
                    id = "documents",
                    titleRes = Res.string.nav_documents,
                    iconRes = Res.drawable.file_text,
                    destination = HomeDestination.Documents,
                    priority = 10,
                    mobileTabOrder = 1,
                    shellTopBar = ShellTopBarDefault.Search,
                ),
                NavItem(
                    id = "accountant",
                    titleRes = Res.string.nav_accountant,
                    iconRes = Res.drawable.wallet_2,
                    destination = HomeDestination.Accountant,
                    priority = 25,
                    shellTopBar = ShellTopBarDefault.Title,
                ),
                NavItem(
                    id = "vat",
                    titleRes = Res.string.nav_vat,
                    iconRes = Res.drawable.calculator,
                    destination = HomeDestination.UnderDevelopment,
                    comingSoon = true,
                    priority = 30,
                ),
                NavItem(
                    id = "reports",
                    titleRes = Res.string.nav_reports,
                    iconRes = Res.drawable.bar_chart,
                    destination = HomeDestination.UnderDevelopment,
                    comingSoon = true,
                    priority = 40,
                ),
            ),
        ),
        ModuleNavGroup(
            sectionId = "company",
            sectionTitle = Res.string.nav_section_company,
            sectionIcon = Res.drawable.users,
            sectionOrder = 1,
            items = listOf(
                NavItem(
                    id = "company_details",
                    titleRes = Res.string.settings_workspace_details,
                    iconRes = Res.drawable.user,
                    destination = HomeDestination.WorkspaceDetails,
                    priority = 0,
                ),
                NavItem(
                    id = "team",
                    titleRes = Res.string.nav_team,
                    iconRes = Res.drawable.users,
                    destination = HomeDestination.Team,
                    priority = 20,
                ),
            ),
        ),
    )
    override val settingsGroups: List<ModuleSettingsGroup> = listOf(
        ModuleSettingsGroup(
            title = Res.string.settings_group_account,
            priority = SettingsPriority.High,
            sections = listOf(
                ModuleSettingsSection(
                    title = Res.string.settings_notifications,
                    icon = Icons.Default.Notifications,
                    destination = SettingsDestination.NotificationPreferences
                )
            )
        ),
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
