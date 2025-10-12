package ai.dokus.app.wrap

import ai.dokus.app.banking.bankingDiModule
import ai.dokus.app.cashflow.cashflowDiModule
import ai.dokus.app.contacts.contactsDiModule
import ai.dokus.app.core.coreDiModule
import ai.dokus.app.dashboard.dashboardDiModule
import ai.dokus.app.inventory.inventoryDiModule
import ai.dokus.app.onboarding.onboardingDiModule
import ai.dokus.app.repository.repositoryDiModule
import ai.dokus.app.simulations.simulationDiModule
import ai.dokus.foundation.ui.uiDiModule
import androidx.compose.runtime.Composable
import org.koin.compose.KoinApplication

@Composable
fun Bootstrapped(content: @Composable () -> Unit) {
    KoinApplication(
        application = {
            modules(
                coreDiModule,
                uiDiModule,
                repositoryDiModule,
                onboardingDiModule,
                dashboardDiModule,
                contactsDiModule,
                cashflowDiModule,
                simulationDiModule,
                inventoryDiModule,
                bankingDiModule,
            )
        }
    ) {
        content()
    }
}