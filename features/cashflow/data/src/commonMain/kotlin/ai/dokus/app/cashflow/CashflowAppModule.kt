package ai.dokus.app.cashflow

import ai.dokus.app.cashflow.di.cashflowNetworkModule
import ai.dokus.app.core.AppDataModuleDi
import ai.dokus.app.core.AppDomainModuleDi
import ai.dokus.app.core.AppModule
import ai.dokus.app.core.AppPresentationModuleDi
import ai.dokus.app.core.DashboardWidget
import ai.dokus.app.core.ModuleSettingsGroup
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.navigation.NavigationProvider

/**
 * Cashflow module registration for dependency injection.
 *
 * This module provides authenticated access to the Cashflow backend service (invoices & expenses).
 * Currently only provides data layer; presentation layer to be added later.
 */
object CashflowAppModule : AppModule {
    // Presentation layer - not yet implemented
    override val navigationProvider: NavigationProvider? = null
    override val homeNavigationProvider: NavigationProvider? = null
    override val homeItems: List<HomeItem> = emptyList()
    override val settingsGroups: List<ModuleSettingsGroup> = emptyList()
    override val dashboardWidgets: List<DashboardWidget> = emptyList()

    override val presentationDi: AppPresentationModuleDi = object : AppPresentationModuleDi {
        override val viewModels = null
        override val presentation = null
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
