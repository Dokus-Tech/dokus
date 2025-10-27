package ai.dokus.app.auth

import ai.dokus.app.auth.di.authPresentationModule
import ai.dokus.app.core.AppModule
import ai.dokus.app.core.DashboardWidget
import ai.dokus.app.core.ModuleSettingsGroup
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.navigation.NavigationProvider
import org.koin.core.module.Module

val authAppModule = object : AppModule {
    override val navigationProvider: NavigationProvider? = null
    override val homeNavigationProvider: NavigationProvider? = null
    override val diModules: List<Module> = listOf(
        authPlatformModule,      // 1. Platform-specific (SecureStorage, DatabaseDriverFactory)
        authNetworkModule,       // 2. Network layer (HTTP clients, RPC, HealthRemoteService)
        authDataModule,          // 3. Data layer (Repositories, DataSources, Database, Managers)
        authDomainModule,        // 4. Domain layer (UseCases - business logic)
        authPresentationModule,  // 5. Presentation layer (ViewModels, Initializers)
    )

    override val homeItems: List<HomeItem> = emptyList()
    override val settingsGroups: List<ModuleSettingsGroup> = emptyList()
    override val dashboardWidgets: List<DashboardWidget> = emptyList()
}