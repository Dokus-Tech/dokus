package ai.dokus.app.cashflow

import ai.dokus.app.cashflow.cache.CashflowDb
import ai.dokus.app.cashflow.di.cashflowNetworkModule
import ai.dokus.app.cashflow.di.cashflowPresentationModule
import ai.dokus.app.cashflow.di.cashflowViewModelModule
import ai.dokus.app.cashflow.navigation.CashflowHomeNavigationProvider
import ai.dokus.app.cashflow.navigation.CashflowNavigationProvider
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.cashflow
import ai.dokus.app.resources.generated.cashflow_title
import ai.dokus.app.resources.generated.ml
import ai.dokus.app.resources.generated.settings_group_workspace
import ai.dokus.app.resources.generated.settings_peppol
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.design.model.HomeItemPriority
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.HomeDestination
import ai.dokus.foundation.navigation.destinations.SettingsDestination
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.AppDomainModuleDi
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.AppPresentationModuleDi
import tech.dokus.foundation.app.DashboardWidget
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.ModuleSettingsSection
import tech.dokus.foundation.app.SettingsPriority

/**
 * Cashflow module registration for dependency injection.
 *
 * This module provides authenticated access to the Cashflow backend service (invoices & expenses).
 */
object CashflowAppModule : AppModule, KoinComponent {
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
        ),
        HomeItem(
            destination = HomeDestination.AiChat,
            titleRes = Res.string.cashflow_title,
            iconRes = Res.drawable.ml,
            priority = HomeItemPriority.Medium,
            showTopBar = false
        )
    )
    override val settingsGroups: List<ModuleSettingsGroup> = listOf(
        ModuleSettingsGroup(
            title = Res.string.settings_group_workspace,
            priority = SettingsPriority.Medium,
            sections = listOf(
                ModuleSettingsSection(
                    title = Res.string.settings_peppol,
                    icon = Icons.Default.Email,
                    destination = SettingsDestination.PeppolSettings
                )
            )
        )
    )
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

    override suspend fun initializeData() {
        val cashflowDb: CashflowDb by inject()
        cashflowDb.initialize()
    }
}
