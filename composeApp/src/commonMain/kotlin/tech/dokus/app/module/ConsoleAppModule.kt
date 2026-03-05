package tech.dokus.app.module

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_search
import tech.dokus.aura.resources.bar_chart
import tech.dokus.aura.resources.chart_bar_trend_up
import tech.dokus.aura.resources.console_activity_title
import tech.dokus.aura.resources.console_clients_title
import tech.dokus.aura.resources.console_export_title
import tech.dokus.aura.resources.console_requests_title
import tech.dokus.aura.resources.file_text
import tech.dokus.aura.resources.inbox
import tech.dokus.aura.resources.nav_section_accounting
import tech.dokus.aura.resources.nav_section_console_tools
import tech.dokus.aura.resources.search
import tech.dokus.aura.resources.settings
import tech.dokus.aura.resources.wallet_2
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.AppDomainModuleDi
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.AppPresentationModuleDi
import tech.dokus.foundation.app.DashboardWidget
import tech.dokus.foundation.app.ModuleNavGroup
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.NavContext
import tech.dokus.foundation.aura.model.DesktopNavPlacement
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.ShellTopBarDefault
import tech.dokus.navigation.destinations.HomeDestination

internal object ConsoleAppModule : AppModule {
    override val navigationProvider = null
    override val homeNavigationProvider = null

    override val navGroups: List<ModuleNavGroup> = listOf(
        ModuleNavGroup(
            sectionId = "console_triage",
            sectionTitle = Res.string.nav_section_accounting,
            sectionIcon = Res.drawable.chart_bar_trend_up,
            navContext = NavContext.FIRM,
            sectionOrder = 0,
            sectionDefaultExpanded = true,
            items = listOf(
                NavItem(
                    id = "console_search",
                    titleRes = Res.string.action_search,
                    iconRes = Res.drawable.search,
                    destination = HomeDestination.Search,
                    priority = -10,
                    shellTopBar = ShellTopBarDefault.Title,
                    desktopPlacement = DesktopNavPlacement.PinnedTop,
                    desktopShortcutHint = "⌘K",
                ),
                NavItem(
                    id = "console_clients",
                    titleRes = Res.string.console_clients_title,
                    iconRes = Res.drawable.wallet_2,
                    destination = HomeDestination.ConsoleClients,
                    priority = 0,
                    shellTopBar = ShellTopBarDefault.Title,
                ),
                NavItem(
                    id = "console_requests",
                    titleRes = Res.string.console_requests_title,
                    iconRes = Res.drawable.inbox,
                    destination = HomeDestination.ConsoleRequests,
                    priority = 10,
                    shellTopBar = ShellTopBarDefault.Title,
                ),
                NavItem(
                    id = "console_activity",
                    titleRes = Res.string.console_activity_title,
                    iconRes = Res.drawable.bar_chart,
                    destination = HomeDestination.ConsoleActivity,
                    priority = 20,
                    shellTopBar = ShellTopBarDefault.Title,
                ),
            )
        ),
        ModuleNavGroup(
            sectionId = "console_tools",
            sectionTitle = Res.string.nav_section_console_tools,
            sectionIcon = Res.drawable.settings,
            navContext = NavContext.FIRM,
            sectionOrder = 1,
            sectionDefaultExpanded = true,
            items = listOf(
                NavItem(
                    id = "console_export",
                    titleRes = Res.string.console_export_title,
                    iconRes = Res.drawable.file_text,
                    destination = HomeDestination.ConsoleExport,
                    priority = 0,
                    shellTopBar = ShellTopBarDefault.Title,
                ),
            )
        ),
    )

    override val settingsGroups: List<ModuleSettingsGroup> = emptyList()
    override val dashboardWidgets: List<DashboardWidget> = emptyList()

    override val presentationDi: AppPresentationModuleDi = object : AppPresentationModuleDi {
        override val viewModels = null
        override val presentation = null
    }

    override val dataDi: AppDataModuleDi = object : AppDataModuleDi {
        override val platform = null
        override val network = null
        override val data = null
    }
    override val domainDi: AppDomainModuleDi = object : AppDomainModuleDi {
        override val useCases = null
    }
}
