package tech.dokus.app

import ai.dokus.app.auth.AuthAppModule
import ai.dokus.app.cashflow.CashflowAppModule
import ai.dokus.app.contacts.ContactsAppModule
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.navigation.NavigationProvider
import org.jetbrains.compose.resources.StringResource
import org.koin.core.module.Module
import tech.dokus.app.module.AppMainModule
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.diModules

private val baseAppModules = listOf<AppModule>(
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
