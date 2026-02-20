package tech.dokus.app.navigation

import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Verifies destination shape assumptions used by navigation policy tests.
 *
 * `navigateTo` no longer performs class-based dedupe. Top-level dedupe/state-restore
 * is now explicit via `navigateToTopLevelTab`.
 */
class NavigationDestinationPolicyTest {

    // -- Singleton destinations --

    @Test
    fun `HomeDestination members are singletons`() {
        assertNotNull(HomeDestination.Team::class.objectInstance)
        assertNotNull(HomeDestination.Contacts::class.objectInstance)
        assertNotNull(HomeDestination.Accountant::class.objectInstance)
        assertNotNull(HomeDestination.Documents::class.objectInstance)
    }

    @Test
    fun `SettingsDestination members are singletons`() {
        assertNotNull(SettingsDestination.WorkspaceSettings::class.objectInstance)
        assertNotNull(SettingsDestination.AppearanceSettings::class.objectInstance)
        assertNotNull(SettingsDestination.NotificationPreferences::class.objectInstance)
    }

    @Test
    fun `CashFlowDestination object members are singletons`() {
        assertNotNull(CashFlowDestination.AddDocument::class.objectInstance)
        assertNotNull(CashFlowDestination.CreateInvoice::class.objectInstance)
    }

    @Test
    fun `AuthDestination object members are singletons`() {
        assertNotNull(AuthDestination.Login::class.objectInstance)
        assertNotNull(AuthDestination.Register::class.objectInstance)
    }

    // -- Parameterized destinations --

    @Test
    fun `ContactsDestination members are parameterized`() {
        assertNull(ContactsDestination.ContactDetails::class.objectInstance)
        assertNull(ContactsDestination.EditContact::class.objectInstance)
        assertNull(ContactsDestination.CreateContact::class.objectInstance)
    }

    @Test
    fun `CashFlowDestination data class members are parameterized`() {
        assertNull(CashFlowDestination.DocumentReview::class.objectInstance)
        assertNull(CashFlowDestination.DocumentChat::class.objectInstance)
        assertNull(CashFlowDestination.CashflowLedger::class.objectInstance)
    }

    @Test
    fun `AuthDestination data class members are parameterized`() {
        assertNull(AuthDestination.ResetPassword::class.objectInstance)
        assertNull(AuthDestination.VerifyEmail::class.objectInstance)
        assertNull(AuthDestination.ServerConnection::class.objectInstance)
    }
}
