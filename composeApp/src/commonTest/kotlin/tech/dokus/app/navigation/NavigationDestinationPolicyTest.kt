package tech.dokus.app.navigation

import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Verifies destination shape assumptions used by navigation policy tests.
 *
 * `navigateTo` no longer performs class-based dedupe. Top-level dedupe/state-restore
 * is now explicit via `navigateToTopLevelTab`.
 *
 * Singleton destinations are `data object`; parameterized destinations are `data class`.
 */
class NavigationDestinationPolicyTest {

    // -- Singleton destinations (data objects) --

    @Test
    fun `HomeDestination members are singletons`() {
        assertIs<HomeDestination>(HomeDestination.Team)
        assertIs<HomeDestination>(HomeDestination.Contacts)
        assertIs<HomeDestination>(HomeDestination.Accountant)
        assertIs<HomeDestination>(HomeDestination.Documents)
    }

    @Test
    fun `SettingsDestination members are singletons`() {
        assertIs<SettingsDestination>(SettingsDestination.WorkspaceSettings)
        assertIs<SettingsDestination>(SettingsDestination.AppearanceSettings)
        assertIs<SettingsDestination>(SettingsDestination.NotificationPreferences)
    }

    @Test
    fun `CashFlowDestination object members are singletons`() {
        assertIs<CashFlowDestination>(CashFlowDestination.AddDocument)
        assertIs<CashFlowDestination>(CashFlowDestination.CreateInvoice)
    }

    @Test
    fun `AuthDestination object members are singletons`() {
        assertIs<AuthDestination>(AuthDestination.Login)
        assertIs<AuthDestination>(AuthDestination.Register)
    }

    // -- Parameterized destinations (data classes with constructor args) --

    @Test
    fun `ContactsDestination members are parameterized`() {
        val detail = ContactsDestination.ContactDetails(contactId = "test")
        assertIs<ContactsDestination>(detail)
    }

    @Test
    fun `CashFlowDestination data class members are parameterized`() {
        val review = CashFlowDestination.DocumentReview(documentId = "test")
        assertIs<CashFlowDestination>(review)
        val sourceViewer = CashFlowDestination.DocumentSourceViewer(
            documentId = "test",
            sourceId = "source",
        )
        assertIs<CashFlowDestination>(sourceViewer)
    }

    @Test
    fun `AuthDestination data class members are parameterized`() {
        val reset = AuthDestination.ResetPassword(token = "test")
        assertIs<AuthDestination>(reset)
    }
}
