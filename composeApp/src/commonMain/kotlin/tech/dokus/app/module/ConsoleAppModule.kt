package tech.dokus.app.module

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chart_bar_trend_up
import tech.dokus.aura.resources.home_today
import tech.dokus.aura.resources.nav_accountant
import tech.dokus.aura.resources.nav_more
import tech.dokus.aura.resources.nav_reports
import tech.dokus.aura.resources.nav_section_accounting
import tech.dokus.aura.resources.nav_section_company
import tech.dokus.aura.resources.wallet_2
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.AppDomainModuleDi
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.AppPresentationModuleDi
import tech.dokus.foundation.app.DashboardWidget
import tech.dokus.foundation.app.ModuleNavGroup
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.NavContext
import tech.dokus.foundation.aura.model.NavItem
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
                    id = "console_clients",
                    titleRes = Res.string.nav_accountant,
                    iconRes = Res.drawable.wallet_2,
                    destination = HomeDestination.ConsoleClients,
                    priority = 0,
                    shellTopBar = null,
                ),
                NavItem(
                    id = "console_requests",
                    titleRes = Res.string.home_today,
                    iconRes = Res.drawable.chart_bar_trend_up,
                    destination = HomeDestination.ConsoleRequests,
                    priority = 10,
                    shellTopBar = null,
                ),
                NavItem(
                    id = "console_activity",
                    titleRes = Res.string.nav_more,
                    iconRes = Res.drawable.chart_bar_trend_up,
                    destination = HomeDestination.ConsoleActivity,
                    priority = 20,
                    shellTopBar = null,
                ),
            )
        ),
        ModuleNavGroup(
            sectionId = "console_tools",
            sectionTitle = Res.string.nav_section_company,
            sectionIcon = Res.drawable.chart_bar_trend_up,
            navContext = NavContext.FIRM,
            sectionOrder = 1,
            items = listOf(
                NavItem(
                    id = "console_export",
                    titleRes = Res.string.nav_reports,
                    iconRes = Res.drawable.chart_bar_trend_up,
                    destination = HomeDestination.ConsoleExport,
                    priority = 0,
                    shellTopBar = null,
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
