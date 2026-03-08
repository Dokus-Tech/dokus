package tech.dokus.app

import org.jetbrains.compose.resources.StringResource
import org.koin.core.module.Module
import tech.dokus.app.module.AppMainModule
import tech.dokus.app.module.ConsoleAppModule
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
import tech.dokus.foundation.app.NavContext
import tech.dokus.foundation.app.ModuleNavGroup
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.diModules
import tech.dokus.foundation.aura.model.DesktopNavPlacement
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination

private val baseAppModules = listOf(
    AppMainModule,
    ConsoleAppModule,
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

private fun List<AppModule>.groupsFor(navContext: NavContext): List<ModuleNavGroup> =
    flatMap { it.navGroups }.filter { it.navContext == navContext }

/** All nav items from all modules, flattened */
fun List<AppModule>.allNavItems(navContext: NavContext): List<NavItem> =
    groupsFor(navContext).flatMap { it.items }

/** Backward-compatible aggregate across all contexts. */
val List<AppModule>.allNavItems: List<NavItem>
    get() = flatMap { it.navGroups }.flatMap { it.items }

/** Desktop sections — groups merged by sectionId, items sorted by priority, sections sorted by order */
fun List<AppModule>.navSectionsCombined(navContext: NavContext): List<NavSection> {
    val allGroups = groupsFor(navContext)
    return allGroups
        .groupBy { it.sectionId }
        .map { (_, groups) ->
            val first = groups.minByOrNull { it.sectionOrder } ?: groups.first()
            val sectionItems = groups
                .flatMap { it.items }
                .filter { it.desktopPlacement == DesktopNavPlacement.Section }
                .sortedBy { it.priority }
            NavSection(
                id = first.sectionId,
                titleRes = first.sectionTitle,
                iconRes = first.sectionIcon,
                order = first.sectionOrder,
                items = sectionItems,
                defaultExpanded = groups.any { it.sectionDefaultExpanded },
            )
        }
        .filter { it.items.isNotEmpty() }
        .sortedBy { it.order }
}

val List<AppModule>.navSectionsCombined: List<NavSection>
    get() {
        val allGroups = flatMap { it.navGroups }
        return allGroups
            .groupBy { it.sectionId }
            .map { (_, groups) ->
                val first = groups.minByOrNull { it.sectionOrder } ?: groups.first()
                val sectionItems = groups
                    .flatMap { it.items }
                    .filter { it.desktopPlacement == DesktopNavPlacement.Section }
                    .sortedBy { it.priority }
                NavSection(
                    id = first.sectionId,
                    titleRes = first.sectionTitle,
                    iconRes = first.sectionIcon,
                    order = first.sectionOrder,
                    items = sectionItems,
                    defaultExpanded = groups.any { it.sectionDefaultExpanded },
                )
            }
            .filter { it.items.isNotEmpty() }
            .sortedBy { it.order }
    }

/** Desktop pinned items rendered above sectioned groups. */
fun List<AppModule>.desktopPinnedItems(navContext: NavContext): List<NavItem> =
    allNavItems(navContext)
        .filter { it.desktopPlacement == DesktopNavPlacement.PinnedTop }
        .sortedBy { it.priority }

val List<AppModule>.desktopPinnedItems: List<NavItem>
    get() = allNavItems
        .filter { it.desktopPlacement == DesktopNavPlacement.PinnedTop }
        .sortedBy { it.priority }

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
