package tech.dokus.app.navigation

import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.HomeDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HomeDeepLinkCommandExecutionTest {

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
