package ai.dokus.app.contacts

import ai.dokus.app.contacts.di.contactsPresentationModule
import ai.dokus.app.contacts.di.contactsViewModelModule
import ai.dokus.app.contacts.navigation.ContactsHomeNavigationProvider
import ai.dokus.app.core.AppDataModuleDi
import ai.dokus.app.core.AppDomainModuleDi
import ai.dokus.app.core.AppModule
import ai.dokus.app.core.AppPresentationModuleDi
import ai.dokus.app.core.DashboardWidget
import ai.dokus.app.core.ModuleSettingsGroup
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.contacts_title
import ai.dokus.app.resources.generated.users
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.design.model.HomeItemPriority
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.HomeDestination

/**
 * Contacts module registration for dependency injection.
 *
 * This module provides access to the Contacts management features.
 */
object ContactsAppModule : AppModule {
    // Presentation layer
    override val navigationProvider: NavigationProvider? = null
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
        override val viewModels = contactsViewModelModule
        override val presentation = contactsPresentationModule
    }

    // Data layer - not yet needed
    override val dataDi: AppDataModuleDi = object : AppDataModuleDi {
        override val platform = null
        override val network = null
        override val data = null
    }

    // Domain layer - not yet needed
    override val domainDi: AppDomainModuleDi = object : AppDomainModuleDi {
        override val useCases = null
    }
}
