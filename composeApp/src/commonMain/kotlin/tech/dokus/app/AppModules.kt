package tech.dokus.app

import org.jetbrains.compose.resources.StringResource
import org.koin.core.module.Module
import tech.dokus.app.module.AppMainModule
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.more_horizontal
import tech.dokus.aura.resources.nav_more
import tech.dokus.features.auth.AuthAppModule
import tech.dokus.features.auth.authDataModule
import tech.dokus.features.auth.authDomainModule
import tech.dokus.features.auth.authNetworkModule
import tech.dokus.features.auth.authPlatformModule
import tech.dokus.features.cashflow.CashflowAppModule
import tech.dokus.features.cashflow.di.cashflowNetworkModule
import tech.dokus.features.contacts.ContactsAppModule
import tech.dokus.features.contacts.contactsDataModule
import tech.dokus.features.contacts.contactsDomainModule
import tech.dokus.features.contacts.contactsNetworkModule
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.diModules
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination

private val baseAppModules = listOf(
    AppMainModule,
    AuthAppModule,
    CashflowAppModule,
    ContactsAppModule,
)

private val conditionalModules = emptyList<AppModule>()

val appModules: List<AppModule> = baseAppModules + conditionalModules

private val appDataModules: List<Module> = listOf(
    authPlatformModule,
    authNetworkModule,
    authDataModule,
    authDomainModule,
    cashflowNetworkModule,
    contactsNetworkModule,
    contactsDataModule,
    contactsDomainModule,
)

val List<AppModule>.diModules: List<Module>
    get() = flatMap { it.diModules } + appDataModules

val List<AppModule>.homeNavigationProviders: List<NavigationProvider>
    get() = mapNotNull { it.homeNavigationProvider }

/** All nav items from all modules, flattened */
val List<AppModule>.allNavItems: List<NavItem>
    get() = flatMap { it.navGroups }.flatMap { it.items }

/** Desktop sections — groups merged by sectionId, items sorted by priority, sections sorted by order */
val List<AppModule>.navSectionsCombined: List<NavSection>
    get() {
        val allGroups = flatMap { it.navGroups }
        return allGroups
            .groupBy { it.sectionId }
            .map { (_, groups) ->
                val first = groups.minByOrNull { it.sectionOrder } ?: groups.first()
                val section = NavSection(
                    id = first.sectionId,
                    titleRes = first.sectionTitle,
                    iconRes = first.sectionIcon,
                    items = groups.flatMap { it.items }.sortedBy { it.priority },
                    defaultExpanded = groups.any { it.sectionDefaultExpanded },
                )
                first.sectionOrder to section
            }
            .sortedBy { (order, _) -> order }
            .map { (_, section) -> section }
    }

/** Mobile bottom tabs — items with mobileTabOrder + "More" appended */
val List<AppModule>.mobileTabConfigs: List<MobileTabConfig>
    get() {
        val itemTabs = allNavItems
            .filter { it.mobileTabOrder != null }
            .sortedBy { it.mobileTabOrder }
            .map { MobileTabConfig(it.id, it.titleRes, it.iconRes, it.destination) }
        return itemTabs + MobileTabConfig("more", Res.string.nav_more, Res.drawable.more_horizontal, HomeDestination.More)
    }

val List<AppModule>.settingsGroups: List<ModuleSettingsGroup>
    get() = flatMap { it.settingsGroups }.sortedBy { it.priority.order }

val List<AppModule>.settingsGroupsCombined: Map<StringResource, List<ModuleSettingsGroup>>
    get() = flatMap { it.settingsGroups }
        .groupBy { it.title }
        .mapValues { (_, groups) -> groups.sortedBy { it.priority.order } }
