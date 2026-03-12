package tech.dokus.features.banking

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_subtitle
import tech.dokus.aura.resources.banking_balances_title
import tech.dokus.aura.resources.banking_payments_subtitle
import tech.dokus.aura.resources.banking_payments_title
import tech.dokus.aura.resources.nav_section_banking
import tech.dokus.aura.resources.wallet_2
import tech.dokus.features.banking.di.bankingPresentationModule
import tech.dokus.features.banking.di.bankingViewModelModule
import tech.dokus.features.banking.navigation.BankingHomeNavigationProvider
import tech.dokus.features.banking.navigation.BankingNavigationProvider
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.AppDomainModuleDi
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.AppPresentationModuleDi
import tech.dokus.foundation.app.DashboardWidget
import tech.dokus.foundation.app.ModuleNavGroup
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.ShellTopBarDefault
import tech.dokus.navigation.NavSectionIds
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination

object BankingAppModule : AppModule {
    override val navigationProvider: NavigationProvider = BankingNavigationProvider
    override val homeNavigationProvider: NavigationProvider = BankingHomeNavigationProvider
    override val navGroups: List<ModuleNavGroup> = listOf(
        ModuleNavGroup(
            sectionId = NavSectionIds.BANKING,
            sectionTitle = Res.string.nav_section_banking,
            sectionIcon = Res.drawable.wallet_2,
            sectionOrder = 1,
            items = listOf(
                NavItem(
                    id = "banking_balances",
                    titleRes = Res.string.banking_balances_title,
                    iconRes = Res.drawable.wallet_2,
                    destination = HomeDestination.Balances,
                    priority = 0,
                    shellTopBar = ShellTopBarDefault.Title,
                    subtitleRes = Res.string.banking_balances_subtitle,
                ),
                NavItem(
                    id = "banking_payments",
                    titleRes = Res.string.banking_payments_title,
                    iconRes = Res.drawable.wallet_2,
                    destination = HomeDestination.Payments,
                    priority = 1,
                    shellTopBar = ShellTopBarDefault.Title,
                    subtitleRes = Res.string.banking_payments_subtitle,
                ),
            ),
        ),
    )
    override val settingsGroups: List<ModuleSettingsGroup> = emptyList()
    override val dashboardWidgets: List<DashboardWidget> = emptyList()

    override val presentationDi: AppPresentationModuleDi = object : AppPresentationModuleDi {
        override val viewModels = bankingViewModelModule
        override val presentation = bankingPresentationModule
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
