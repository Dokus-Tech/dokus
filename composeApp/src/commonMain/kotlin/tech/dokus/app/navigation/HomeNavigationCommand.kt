package tech.dokus.app.navigation

import androidx.navigation.NavController
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.NavigationDestination
import tech.dokus.navigation.navigateTo
import tech.dokus.navigation.navigateToTopLevelTab

internal enum class HomeNavigationSource {
    CM,
    BC,
}

internal sealed interface HomeNavigationCommand {
    data object OpenConsoleClients : HomeNavigationCommand
    data class OpenDocuments(
        val source: HomeNavigationSource = HomeNavigationSource.CM,
    ) : HomeNavigationCommand
    data class OpenDocumentReview(val documentId: String) : HomeNavigationCommand
}

internal sealed interface HomeNavigationStep {
    data class TopLevelTab(val destination: HomeDestination) : HomeNavigationStep
    data class Push(val destination: NavigationDestination) : HomeNavigationStep
}

internal fun resolveHomeNavigationSteps(
    command: HomeNavigationCommand,
    canBCAccess: Boolean = true,
): List<HomeNavigationStep> {
    return when (command) {
        HomeNavigationCommand.OpenConsoleClients -> listOf(
            HomeNavigationStep.TopLevelTab(
                if (canBCAccess) HomeDestination.Accountant else HomeDestination.Today
            )
        )
        is HomeNavigationCommand.OpenDocuments -> listOf(
            HomeNavigationStep.TopLevelTab(HomeDestination.Documents)
        )
        is HomeNavigationCommand.OpenDocumentReview -> listOf(
            HomeNavigationStep.TopLevelTab(HomeDestination.Documents),
            HomeNavigationStep.Push(CashFlowDestination.DocumentReview(command.documentId))
        )
    }
}

internal fun NavController.executeHomeNavigationCommand(
    command: HomeNavigationCommand,
    canBCAccess: Boolean = true,
) {
    resolveHomeNavigationSteps(command, canBCAccess).forEach { step ->
        when (step) {
            is HomeNavigationStep.TopLevelTab -> navigateToTopLevelTab(step.destination)
            is HomeNavigationStep.Push -> navigateTo(step.destination)
        }
    }
}
