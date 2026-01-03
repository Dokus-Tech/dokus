package tech.dokus.app

import org.jetbrains.compose.resources.StringResource
import org.koin.core.module.Module
import tech.dokus.app.module.AppMainModule
import tech.dokus.features.auth.AuthAppModule
import tech.dokus.features.cashflow.CashflowAppModule
import tech.dokus.features.contacts.ContactsAppModule
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.diModules
import tech.dokus.foundation.aura.model.HomeItem
import tech.dokus.navigation.NavigationProvider

private val baseAppModules = listOf(
    AppMainModule,
    AuthAppModule,
    CashflowAppModule,
    ContactsAppModule,
)

private val conditionalModules = emptyList<AppModule>()

val appModules: List<AppModule> = baseAppModules + conditionalModules

val List<AppModule>.diModules: List<Module>
    get() = flatMap { it.diModules }

val List<AppModule>.homeNavigationProviders: List<NavigationProvider>
    get() = mapNotNull { it.homeNavigationProvider }

val List<AppModule>.homeItems: List<HomeItem>
    get() = flatMap { it.homeItems }.sortedBy { it.priority }

val List<AppModule>.settingsGroups: List<ModuleSettingsGroup>
    get() = flatMap { it.settingsGroups }.sortedBy { it.priority.order }

val List<AppModule>.settingsGroupsCombined: Map<StringResource, List<ModuleSettingsGroup>>
    get() = flatMap { it.settingsGroups }
        .groupBy { it.title }
        .mapValues { (_, groups) -> groups.sortedBy { it.priority.order } }
