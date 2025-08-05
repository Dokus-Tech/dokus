package ai.thepredict.app.wrap

import ai.thepredict.app.cashflow.cashflowDiModule
import ai.thepredict.app.contacts.contactsDiModule
import ai.thepredict.app.core.configureDi
import ai.thepredict.app.core.coreDiModule
import ai.thepredict.app.dashboard.dashboardDiModule
import ai.thepredict.app.home.homeDiModule
import ai.thepredict.app.inventory.inventoryDiModule
import ai.thepredict.app.onboarding.onboardingDiModule
import ai.thepredict.app.simulations.simulationDiModule
import ai.thepredict.repository.repositoryDiModule
import ai.thepredict.ui.uiDiModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun Bootstrapped(content: @Composable () -> Unit) {
    LaunchedEffect("app-bootstrap") {
        configureDi(
            coreDiModule,
            uiDiModule,
            repositoryDiModule,
            onboardingDiModule,
            homeDiModule,
            dashboardDiModule,
            contactsDiModule,
            cashflowDiModule,
            simulationDiModule,
            inventoryDiModule,
        )
    }

    content()
}