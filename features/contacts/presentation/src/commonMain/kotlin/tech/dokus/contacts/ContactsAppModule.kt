package tech.dokus.contacts

import ai.dokus.app.contacts.contactsDataModule
import ai.dokus.app.contacts.contactsDomainModule
import ai.dokus.app.contacts.contactsNetworkModule
import ai.dokus.app.contacts.datasource.ContactsDb
import tech.dokus.contacts.di.contactsPresentationModule
import tech.dokus.contacts.navigation.ContactsHomeNavigationProvider
import tech.dokus.contacts.navigation.ContactsNavigationProvider
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.contacts_title
import ai.dokus.app.resources.generated.users
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.design.model.HomeItemPriority
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.HomeDestination
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.AppDomainModuleDi
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.AppPresentationModuleDi
import tech.dokus.foundation.app.DashboardWidget
import tech.dokus.foundation.app.ModuleSettingsGroup

/**
 * Contacts module registration for dependency injection.
 *
 * This module provides access to the Contacts management features.
 */
object ContactsAppModule : AppModule, KoinComponent {
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
        override val network = contactsNetworkModule
        override val data = contactsDataModule
    }

    // Domain layer
    override val domainDi: AppDomainModuleDi = object : AppDomainModuleDi {
        override val useCases = contactsDomainModule
    }

    override suspend fun initializeData() {
        val contactsDb: ContactsDb by inject()
        contactsDb.initialize()
    }
}