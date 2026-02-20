package tech.dokus.app.navigation

import androidx.navigation.NavController
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.NavigationDestination
import tech.dokus.navigation.navigateTo
import tech.dokus.navigation.navigateToTopLevelTab

internal sealed interface HomeNavigationCommand {
    data object OpenDocuments : HomeNavigationCommand
    data class OpenDocumentReview(val documentId: String) : HomeNavigationCommand
}

internal sealed interface HomeNavigationStep {
    data class TopLevelTab(val destination: HomeDestination) : HomeNavigationStep
    data class Push(val destination: NavigationDestination) : HomeNavigationStep
}

internal fun resolveHomeNavigationSteps(command: HomeNavigationCommand): List<HomeNavigationStep> {
    return when (command) {
        HomeNavigationCommand.OpenDocuments -> listOf(
            HomeNavigationStep.TopLevelTab(HomeDestination.Documents)
        )
        is HomeNavigationCommand.OpenDocumentReview -> listOf(
            HomeNavigationStep.TopLevelTab(HomeDestination.Documents),
            HomeNavigationStep.Push(CashFlowDestination.DocumentReview(command.documentId))
        )
    }
}

internal fun NavController.executeHomeNavigationCommand(command: HomeNavigationCommand) {
    resolveHomeNavigationSteps(command).forEach { step ->
        when (step) {
            is HomeNavigationStep.TopLevelTab -> navigateToTopLevelTab(step.destination)
            is HomeNavigationStep.Push -> navigateTo(step.destination)
        }
    }
}
