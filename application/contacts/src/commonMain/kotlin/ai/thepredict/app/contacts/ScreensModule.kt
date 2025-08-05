package ai.thepredict.app.contacts

import ai.thepredict.app.contacts.screen.ContactsScreen
import ai.thepredict.app.navigation.HomeTabsNavigation
import cafe.adriel.voyager.core.registry.screenModule

val contactsScreensModule = screenModule {
    register<HomeTabsNavigation.Contacts> {
        ContactsScreen()
    }
}