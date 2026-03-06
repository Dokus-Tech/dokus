package tech.dokus.app.navigation

import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.HomeDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HomeDeepLinkCommandExecutionTest {

    @Test
    fun `open console clients command resolves to console clients tab`() {
        val steps = resolveHomeNavigationSteps(HomeNavigationCommand.OpenConsoleClients)

        assertEquals(1, steps.size)

        val tabStep = assertIs<HomeNavigationStep.TopLevelTab>(steps[0])
        assertEquals(HomeDestination.ConsoleClients, tabStep.destination)
    }

    @Test
    fun `open console clients command falls back to today when bookkeeper console access is unavailable`() {
        val steps = resolveHomeNavigationSteps(
            command = HomeNavigationCommand.OpenConsoleClients,
            canBCAccess = false
        )

        assertEquals(1, steps.size)

        val tabStep = assertIs<HomeNavigationStep.TopLevelTab>(steps[0])
        assertEquals(HomeDestination.Today, tabStep.destination)
    }

    @Test
    fun `open documents command resolves to documents tab`() {
        val steps = resolveHomeNavigationSteps(HomeNavigationCommand.OpenDocuments)

        assertEquals(1, steps.size)
        assertEquals(
            HomeDestination.Documents,
            assertIs<HomeNavigationStep.TopLevelTab>(steps[0]).destination
        )
    }

    @Test
    fun `open document review command resolves to documents tab then review route`() {
        val steps = resolveHomeNavigationSteps(
            HomeNavigationCommand.OpenDocumentReview(documentId = "doc-42")
        )

        assertEquals(2, steps.size)

        val tabStep = assertIs<HomeNavigationStep.TopLevelTab>(steps[0])
        assertEquals(HomeDestination.Documents, tabStep.destination)

        val pushStep = assertIs<HomeNavigationStep.Push>(steps[1])
        assertEquals(CashFlowDestination.DocumentReview("doc-42"), pushStep.destination)
    }
}
