package ai.dokus.app.cashflow

import ai.dokus.app.cashflow.di.cashflowNetworkModule
import ai.dokus.app.cashflow.di.cashflowPresentationModule
import ai.dokus.app.cashflow.di.cashflowViewModelModule
import ai.dokus.app.cashflow.navigation.CashflowHomeNavigationProvider
import ai.dokus.app.cashflow.navigation.CashflowNavigationProvider
import ai.dokus.app.core.AppDataModuleDi
import ai.dokus.app.core.AppDomainModuleDi
import ai.dokus.app.core.AppModule
import ai.dokus.app.core.AppPresentationModuleDi
import ai.dokus.app.core.DashboardWidget
import ai.dokus.app.core.ModuleSettingsGroup
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.cashflow
import ai.dokus.app.resources.generated.cashflow_title
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.design.model.HomeItemPriority
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.HomeDestination

/**
 * Cashflow module registration for dependency injection.
 *
 * This module provides authenticated access to the Cashflow backend service (invoices & expenses).
 */
object CashflowAppModule : AppModule {
    // Presentation layer
    override val navigationProvider: NavigationProvider = CashflowNavigationProvider
    override val homeNavigationProvider: NavigationProvider = CashflowHomeNavigationProvider
    override val homeItems: List<HomeItem> = listOf(
        HomeItem(
            destination = HomeDestination.Cashflow,
            titleRes = Res.string.cashflow_title,
            iconRes = Res.drawable.cashflow,
            priority = HomeItemPriority.High,
            showTopBar = false
        )
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
        override val network = cashflowNetworkModule
        override val data = null
    }

    // Domain layer - not yet needed
    override val domainDi: AppDomainModuleDi = object : AppDomainModuleDi {
        override val useCases = null
    }
}
