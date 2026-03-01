package tech.dokus.app.navigation

import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.HomeDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HomeDeepLinkCommandExecutionTest {

    @Test
    fun `open console clients command resolves to accountant tab`() {
        val steps = resolveHomeNavigationSteps(HomeNavigationCommand.OpenConsoleClients)

        assertEquals(1, steps.size)

        val tabStep = assertIs<HomeNavigationStep.TopLevelTab>(steps[0])
        assertEquals(HomeDestination.Accountant, tabStep.destination)
    }

    @Test
    fun `open console clients command falls back to today when console access is unavailable`() {
        val steps = resolveHomeNavigationSteps(
            command = HomeNavigationCommand.OpenConsoleClients,
            canConsoleAccess = false
        )

        assertEquals(1, steps.size)

        val tabStep = assertIs<HomeNavigationStep.TopLevelTab>(steps[0])
        assertEquals(HomeDestination.Today, tabStep.destination)
    }

    @Test
    fun `open documents command keeps documents tab regardless of source`() {
        val workspaceSteps = resolveHomeNavigationSteps(
            HomeNavigationCommand.OpenDocuments(source = HomeNavigationSource.Workspace)
        )
        val consoleSteps = resolveHomeNavigationSteps(
            HomeNavigationCommand.OpenDocuments(source = HomeNavigationSource.Console)
        )

        assertEquals(1, workspaceSteps.size)
        assertEquals(1, consoleSteps.size)
        assertEquals(
            HomeDestination.Documents,
            assertIs<HomeNavigationStep.TopLevelTab>(workspaceSteps[0]).destination
        )
        assertEquals(
            HomeDestination.Documents,
            assertIs<HomeNavigationStep.TopLevelTab>(consoleSteps[0]).destination
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
