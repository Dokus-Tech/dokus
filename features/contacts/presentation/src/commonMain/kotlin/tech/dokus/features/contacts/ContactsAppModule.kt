package tech.dokus.features.contacts

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_title
import tech.dokus.aura.resources.users
import tech.dokus.features.contacts.di.contactsPresentationModule
import tech.dokus.features.contacts.navigation.ContactsHomeNavigationProvider
import tech.dokus.features.contacts.navigation.ContactsNavigationProvider
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.AppDomainModuleDi
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.AppPresentationModuleDi
import tech.dokus.foundation.app.DashboardWidget
import tech.dokus.foundation.app.ModuleSettingsGroup
import tech.dokus.foundation.aura.model.HomeItem
import tech.dokus.foundation.aura.model.HomeItemPriority
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination

/**
 * Contacts module registration for dependency injection.
 *
 * This module provides access to the Contacts management features.
 */
object ContactsAppModule : AppModule {
    // Presentation layer
    override val navigationProvider: NavigationProvider = ContactsNavigationProvider
    override val homeNavigationProvider: NavigationProvider = ContactsHomeNavigationProvider
    override val homeItems: List<HomeItem> = listOf(
        HomeItem(
            destination = HomeDestination.Contacts,
            titleRes = Res.string.contacts_title,
            iconRes = Res.drawable.users,
            priority = HomeItemPriority.Medium,
            showTopBar = false
        )
    )
    override val settingsGroups: List<ModuleSettingsGroup> = emptyList()
    override val dashboardWidgets: List<DashboardWidget> = emptyList()

    override val presentationDi: AppPresentationModuleDi = object : AppPresentationModuleDi {
        override val viewModels = contactsPresentationModule
        override val presentation = contactsPresentationModule
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
