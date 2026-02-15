package tech.dokus.features.cashflow

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow
import tech.dokus.aura.resources.cashflow_title
import tech.dokus.aura.resources.chat_title
import tech.dokus.aura.resources.ml
import tech.dokus.aura.resources.nav_forecast
import tech.dokus.aura.resources.nav_section_accounting
import tech.dokus.aura.resources.nav_tomorrow
import tech.dokus.aura.resources.trending_up
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.features.cashflow.di.cashflowPresentationModule
import tech.dokus.features.cashflow.di.cashflowViewModelModule
import tech.dokus.features.cashflow.navigation.CashflowHomeNavigationProvider
import tech.dokus.features.cashflow.navigation.CashflowNavigationProvider
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.AppDomainModuleDi
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.AppPresentationModuleDi
import tech.dokus.foundation.app.DashboardWidget
import tech.dokus.foundation.app.ModuleNavGroup
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.ShellTopBarDefault
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination

/**
 * Cashflow module registration for dependency injection.
 *
 * This module provides authenticated access to the Cashflow backend service (invoices & expenses).
 */
object CashflowAppModule : AppModule {
    // Presentation layer
    override val navigationProvider: NavigationProvider = CashflowNavigationProvider
    override val homeNavigationProvider: NavigationProvider = CashflowHomeNavigationProvider
    override val navGroups: List<ModuleNavGroup> = listOf(
        ModuleNavGroup(
            sectionId = "accounting",
            sectionTitle = Res.string.nav_section_accounting,
            sectionIcon = Res.drawable.cashflow,
            sectionOrder = 0,
            items = listOf(
                NavItem(
                    id = "cashflow",
                    titleRes = Res.string.cashflow_title,
                    iconRes = Res.drawable.cashflow,
                    destination = HomeDestination.Cashflow,
                    priority = 20,
                    mobileTabOrder = 2,
                    shellTopBar = ShellTopBarDefault.Title,
                ),
            ),
        ),
        ModuleNavGroup(
            sectionId = "tomorrow",
            sectionTitle = Res.string.nav_tomorrow,
            sectionIcon = Res.drawable.ml,
            sectionOrder = 2,
            items = listOf(
                NavItem(
                    id = "ai_chat",
                    titleRes = Res.string.chat_title,
                    iconRes = Res.drawable.ml,
                    destination = HomeDestination.AiChat,
                    requiredTier = SubscriptionTier.One,
                    priority = 0,
                ),
                NavItem(
                    id = "forecast",
                    titleRes = Res.string.nav_forecast,
                    iconRes = Res.drawable.trending_up,
                    destination = HomeDestination.UnderDevelopment,
                    comingSoon = true,
                    requiredTier = SubscriptionTier.One,
                    priority = 10,
                ),
            ),
        ),
    )
    override val settingsGroups: List<ModuleSettingsGroup> = emptyList()
    override val dashboardWidgets: List<DashboardWidget> = emptyList()

    override val presentationDi: AppPresentationModuleDi = object : AppPresentationModuleDi {
        override val viewModels = cashflowViewModelModule
        override val presentation = cashflowPresentationModule
    }

    // Data layer - authenticated RPC client
    override val dataDi: AppDataModuleDi = object : AppDataModuleDi {
        override val platform = null
        override val network = null
        override val data = null
    }

    // Domain layer - not yet needed
    override val domainDi: AppDomainModuleDi = object : AppDomainModuleDi {
        override val useCases = null
    }
}
