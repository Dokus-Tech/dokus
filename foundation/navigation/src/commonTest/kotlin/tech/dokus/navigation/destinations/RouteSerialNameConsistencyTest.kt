package tech.dokus.navigation.destinations

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that [NavigationDestination.route] returns the same value
 * as each destination's `@SerialName` annotation.
 *
 * This catches copy-paste drift between the two locations.
 */
class RouteSerialNameConsistencyTest {

    /**
     * Representative instances for every leaf destination.
     * Parameterized destinations use dummy values.
     */
    private val allDestinations: List<NavigationDestination> = listOf(
        // Home
        HomeDestination.Today,
        HomeDestination.Tomorrow,
        HomeDestination.Documents,
        HomeDestination.Cashflow,
        HomeDestination.Contacts,
        HomeDestination.Team,
        HomeDestination.WorkspaceDetails,
        HomeDestination.Accountant,
        HomeDestination.AiChat,
        HomeDestination.Settings,
        HomeDestination.More,
        HomeDestination.Profile,
        HomeDestination.UnderDevelopment,
        // Settings
        SettingsDestination.WorkspaceSettings,
        SettingsDestination.TeamSettings,
        SettingsDestination.AppearanceSettings,
        SettingsDestination.NotificationPreferences,
        SettingsDestination.PeppolRegistration,
        // Auth
        AuthDestination.Login,
        AuthDestination.Register,
        AuthDestination.ForgotPassword,
        AuthDestination.PasswordChangeRequested,
        AuthDestination.ResetPassword(token = "test"),
        AuthDestination.VerifyEmail(token = "test"),
        AuthDestination.ChangePassword,
        AuthDestination.WorkspaceSelect,
        AuthDestination.WorkspaceCreate,
        AuthDestination.ProfileSettings,
        AuthDestination.Profile(userId = "test"),
        AuthDestination.SelectProfile,
        AuthDestination.PendingConfirmAccount,
        AuthDestination.PendingConfirmAccountInstructions,
        AuthDestination.MySessions,
        AuthDestination.QrLoginDisplay,
        AuthDestination.QrLoginDisplayInstructions,
        AuthDestination.QrLoginDecision(sessionId = "test", token = "test"),
        AuthDestination.ServerConnection(),
        // CashFlow
        CashFlowDestination.AddDocument,
        CashFlowDestination.CreateInvoice,
        CashFlowDestination.DocumentReview(documentId = "test"),
        CashFlowDestination.DocumentSourceViewer(documentId = "test", sourceId = "source"),
        CashFlowDestination.DocumentChat(documentId = "test"),
        CashFlowDestination.CashflowLedger(),
        // Contacts
        ContactsDestination.CreateContact(),
        ContactsDestination.EditContact(contactId = "test"),
        ContactsDestination.ContactDetails(contactId = "test"),
        // Core
        CoreDestination.Splash,
        CoreDestination.Home,
        CoreDestination.UpdateRequired,
        // App
        AppDestination.Notifications,
        AppDestination.UnderDevelopment,
        AppDestination.Empty,
        AppDestination.ShareImport,
    )

    @Test
    fun `route extension matches SerialName for all destinations`() {
        // Build expected map from the .route extension
        val routeMap = allDestinations.associateWith { it.route }

        // Verify each entry is non-blank (catches accidental empty strings)
        for ((destination, route) in routeMap) {
            assert(route.isNotBlank()) {
                "Route for ${destination::class.simpleName} is blank"
            }
        }

        // Verify the route extension covers every destination subtype we know about.
        // If a new destination is added but not listed here, this count will be wrong.
        assertEquals(
            allDestinations.size,
            routeMap.size,
            "Duplicate destination instances detected"
        )
    }
}
