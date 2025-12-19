package ai.dokus.app.auth

import ai.dokus.app.auth.database.AuthDb
import ai.dokus.app.auth.di.authPresentationModule
import ai.dokus.app.auth.navigation.AuthNavigationProvider
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.AppDomainModuleDi
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.AppPresentationModuleDi
import tech.dokus.foundation.app.DashboardWidget
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.ModuleSettingsSection
import tech.dokus.foundation.app.SettingsPriority
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.settings_group_account
import ai.dokus.app.resources.generated.settings_profile
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.AuthDestination
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object AuthAppModule : AppModule, KoinComponent {
    // Presentation layer
    override val navigationProvider: NavigationProvider? = AuthNavigationProvider
    override val homeNavigationProvider: NavigationProvider? = null
    override val homeItems: List<HomeItem> = emptyList()
    override val settingsGroups: List<ModuleSettingsGroup> = listOf(
        ModuleSettingsGroup(
            title = Res.string.settings_group_account,
            priority = SettingsPriority.High,
            sections = listOf(
                ModuleSettingsSection(
                    title = Res.string.settings_profile,
                    icon = Icons.Default.Person,
                    destination = AuthDestination.ProfileSettings
                )
            )
        )
    )
    override val dashboardWidgets: List<DashboardWidget> = emptyList()

    override val presentationDi: AppPresentationModuleDi = object : AppPresentationModuleDi {
        override val viewModels = authPresentationModule
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