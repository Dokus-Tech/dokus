package ai.dokus.app.auth

import ai.dokus.app.auth.database.AuthDb
import ai.dokus.app.auth.di.authPresentationModule
import ai.dokus.app.core.AppDataModuleDi
import ai.dokus.app.core.AppDomainModuleDi
import ai.dokus.app.core.AppModule
import ai.dokus.app.core.AppPresentationModuleDi
import ai.dokus.app.core.DashboardWidget
import ai.dokus.app.core.ModuleSettingsGroup
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.navigation.NavigationProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object AuthAppModule : AppModule, KoinComponent {
    // Presentation layer
    override val navigationProvider: NavigationProvider? = null
    override val homeNavigationProvider: NavigationProvider? = null
    override val homeItems: List<HomeItem> = emptyList()
    override val settingsGroups: List<ModuleSettingsGroup> = emptyList()
    override val dashboardWidgets: List<DashboardWidget> = emptyList()

    override val presentationDi: AppPresentationModuleDi = object : AppPresentationModuleDi {
        override val viewModels = null
        override val presentation = authPresentationModule
    }

    // Data layer
    override val dataDi: AppDataModuleDi = object : AppDataModuleDi {
        override val platform = authPlatformModule
        override val network = authNetworkModule
        override val data = authDataModule
    }

    override suspend fun initializeData() {
        val authDb: AuthDb by inject()
        authDb.initialize()
    }

    // Domain layer
    override val domainDi: AppDomainModuleDi = object : AppDomainModuleDi {
        override val useCases = authDomainModule
    }
}