package tech.dokus.app.navigation

import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationOwnershipTest {

    @Test
    fun `root and home destination families are disjoint`() {
        val overlap = NavigationOwnershipPolicy.rootDestinationFamilies
            .intersect(NavigationOwnershipPolicy.homeDestinationFamilies)
        assertTrue(overlap.isEmpty())
    }

    @Test
    fun `owner mapping routes home destination family to home host`() {
        assertEquals(NavHostOwner.Home, NavigationOwnershipPolicy.ownerFor(HomeDestination.Documents))
    }

    @Test
    fun `owner mapping routes root families to root host`() {
        assertEquals(
            NavHostOwner.Root,
            NavigationOwnershipPolicy.ownerFor(CashFlowDestination.DocumentReview("doc-1"))
        )
        assertEquals(
            NavHostOwner.Root,
            NavigationOwnershipPolicy.ownerFor(ContactsDestination.ContactDetails("contact-1"))
        )
        assertEquals(
            NavHostOwner.Root,
            NavigationOwnershipPolicy.ownerFor(SettingsDestination.NotificationPreferences)
        )
        assertEquals(
            NavHostOwner.Root,
            NavigationOwnershipPolicy.ownerFor(AuthDestination.ProfileSettings)
        )
    }
}
