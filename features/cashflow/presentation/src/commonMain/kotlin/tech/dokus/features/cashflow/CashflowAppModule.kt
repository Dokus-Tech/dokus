package tech.dokus.features.cashflow

import tech.dokus.features.cashflow.cache.CashflowDb
import tech.dokus.features.cashflow.di.cashflowNetworkModule
import tech.dokus.features.cashflow.di.cashflowPresentationModule
import tech.dokus.features.cashflow.di.cashflowViewModelModule
import tech.dokus.features.cashflow.navigation.CashflowHomeNavigationProvider
import tech.dokus.features.cashflow.navigation.CashflowNavigationProvider
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow
import tech.dokus.aura.resources.cashflow_title
import tech.dokus.aura.resources.chat_title
import tech.dokus.aura.resources.ml
import tech.dokus.aura.resources.settings_group_workspace
import tech.dokus.aura.resources.settings_peppol
import tech.dokus.foundation.aura.model.HomeItem
import tech.dokus.foundation.aura.model.HomeItemPriority
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination
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
            titleRes = Res.string.chat_title,
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
