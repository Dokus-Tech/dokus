package tech.dokus.features.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.settings_group_account
import tech.dokus.aura.resources.settings_profile
import tech.dokus.features.auth.di.authPresentationModule
import tech.dokus.features.auth.navigation.AuthNavigationProvider
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.AppDomainModuleDi
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.AppPresentationModuleDi
import tech.dokus.foundation.app.DashboardWidget
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.app.ModuleSettingsSection
import tech.dokus.foundation.app.SettingsPriority
import tech.dokus.foundation.app.ModuleNavGroup
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.AuthDestination

object AuthAppModule : AppModule {
    // Presentation layer
    override val navigationProvider: NavigationProvider? = AuthNavigationProvider
    override val homeNavigationProvider: NavigationProvider? = null
    override val navGroups: List<ModuleNavGroup> = emptyList()
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
        override val platform = null
        override val network = null
        override val data = null
    }

    // Domain layer
    override val domainDi: AppDomainModuleDi = object : AppDomainModuleDi {
        override val useCases = null
    }
}
