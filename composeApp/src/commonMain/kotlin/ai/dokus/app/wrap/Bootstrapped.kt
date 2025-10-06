package ai.dokus.app.app.wrap

import ai.dokus.app.app.banking.bankingDiModule
import ai.dokus.app.app.cashflow.cashflowDiModule
import ai.dokus.app.app.contacts.contactsDiModule
import ai.dokus.app.app.core.coreDiModule
import ai.dokus.app.app.dashboard.dashboardDiModule
import ai.dokus.app.app.home.homeDiModule
import ai.dokus.app.app.inventory.inventoryDiModule
import ai.dokus.app.app.onboarding.onboardingDiModule
import ai.dokus.app.app.profile.profileDiModule
import ai.dokus.app.app.simulations.simulationDiModule
import ai.dokus.app.repository.repositoryDiModule
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
                homeDiModule,
                dashboardDiModule,
                contactsDiModule,
                cashflowDiModule,
                simulationDiModule,
                inventoryDiModule,
                bankingDiModule,
                profileDiModule,
            )
        }
    ) {
        content()
    }
}