package tech.dokus.features.auth.presentation.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Email
import tech.dokus.domain.LegalName
import tech.dokus.domain.Name
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.enums.Country
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.User
import tech.dokus.features.auth.mvi.ProfileSettingsState
import tech.dokus.features.auth.mvi.ServerConnectionState
import tech.dokus.features.auth.presentation.auth.components.WorkspaceSelectionBody
import tech.dokus.features.auth.presentation.auth.components.steps.CompanyNameStep
import tech.dokus.features.auth.presentation.auth.components.steps.TypeSelectionStep
import tech.dokus.features.auth.presentation.auth.components.steps.VatAndAddressStep
import tech.dokus.features.auth.presentation.auth.model.AddressFormState
import tech.dokus.features.auth.presentation.auth.model.LookupState
import tech.dokus.features.auth.presentation.auth.screen.ProfileSettingsScreen
import tech.dokus.features.auth.presentation.auth.screen.RegisterConfirmationScreen
import tech.dokus.features.auth.presentation.auth.screen.ServerConnectionScreen
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.components.text.CopyRightText

@RunWith(Parameterized::class)
class AuthAdditionalScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = viewport.deviceConfig,
        showSystemUi = false,
        maxPercentDifference = 0.1
    )

    @Test
    fun registerConfirmationScreen() {
        paparazzi.snapshotAllViewports("RegisterConfirmationScreen", viewport) {
            RegisterConfirmationScreen()
        }
    }

    @Test
    fun workspaceSelectionContent() {
        val state = DokusState.success(sampleTenants())
        paparazzi.snapshotAllViewports("WorkspaceSelectScreen_content", viewport) {
            WorkspaceSelectionPreview(state)
        }
    }

    @Test
    fun workspaceCreateStep_typeSelection() {
        paparazzi.snapshotAllViewports("WorkspaceCreateStep_typeSelection", viewport) {
            WizardStepContainer {
                TypeSelectionStep(
                    selectedType = TenantType.Company,
                    hasFreelancerWorkspace = false,
                    onTypeSelected = {},
                    onBackPress = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Test
    fun workspaceCreateStep_companyName() {
        paparazzi.snapshotAllViewports("WorkspaceCreateStep_companyName", viewport) {
            WizardStepContainer {
                CompanyNameStep(
                    companyName = "Acme Corporation",
                    lookupState = LookupState.Idle,
                    onCompanyNameChanged = {},
                    onBackPress = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Test
    fun workspaceCreateStep_vatAndAddress() {
        paparazzi.snapshotAllViewports("WorkspaceCreateStep_vatAndAddress", viewport) {
            WizardStepContainer {
                VatAndAddressStep(
                    vatNumber = VatNumber("BE0123456789"),
                    address = AddressFormState(
                        streetLine1 = "Main Street 12",
                        streetLine2 = "Suite 3B",
                        city = "Brussels",
                        postalCode = "1000",
                        country = Country.Belgium
                    ),
                    onVatNumberChanged = {},
                    onAddressChanged = {},
                    onBackPress = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Test
    fun serverConnectionScreen() {
        val state = ServerConnectionState.Input(
            protocol = "https",
            host = "selfhosted.dokus.local",
            port = "8443"
        )
        val currentServer = ServerConfig(
            host = "dokus.local",
            port = 8080,
            protocol = "http",
            name = "Local Dokus",
            isCloud = false
        )
        paparazzi.snapshotAllViewports("ServerConnectionScreen", viewport) {
            ServerConnectionScreen(
                state = state,
                currentServer = currentServer,
                onIntent = {}
            )
        }
    }

    @Test
    fun profileSettingsScreen_viewing() {
        paparazzi.snapshotAllViewports("ProfileSettingsScreen_viewing", viewport) {
            ProfileSettingsPreview(
                state = ProfileSettingsState.Viewing(sampleUser())
            )
        }
    }

    @Test
    fun profileSettingsScreen_editing() {
        val user = sampleUser()
        paparazzi.snapshotAllViewports("ProfileSettingsScreen_editing", viewport) {
            ProfileSettingsPreview(
                state = ProfileSettingsState.Editing(
                    user = user,
                    editFirstName = Name("Ada"),
                    editLastName = Name("Byron")
                )
            )
        }
    }
}

@Composable
private fun WorkspaceSelectionPreview(state: DokusState<List<Tenant>>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AppNameText()
        WorkspaceSelectionBody(
            state = state,
            onTenantClick = {},
            onAddTenantClick = {}
        )
        CopyRightText()
    }
}

@Composable
private fun WizardStepContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}

@Composable
private fun ProfileSettingsPreview(state: ProfileSettingsState) {
    val snackbarHostState = remember { SnackbarHostState() }
    ProfileSettingsScreen(
        state = state,
        currentServer = ServerConfig.Cloud,
        isLoggingOut = false,
        snackbarHostState = snackbarHostState,
        onIntent = {},
        onChangeServer = {},
        onResetToCloud = {},
        onLogout = {}
    )
}

private fun sampleTenants(): List<Tenant> {
    val now = LocalDateTime(2024, 1, 1, 10, 0)
    return listOf(
        Tenant(
            id = TenantId("00000000-0000-0000-0000-000000000001"),
            type = TenantType.Company,
            legalName = LegalName("Acme Corporation"),
            displayName = DisplayName("Acme"),
            subscription = SubscriptionTier.default,
            status = TenantStatus.Active,
            language = Language.En,
            vatNumber = VatNumber.Empty,
            createdAt = now,
            updatedAt = now
        ),
        Tenant(
            id = TenantId("00000000-0000-0000-0000-000000000002"),
            type = TenantType.Company,
            legalName = LegalName("Dokus Labs"),
            displayName = DisplayName("Dokus"),
            subscription = SubscriptionTier.default,
            status = TenantStatus.Active,
            language = Language.En,
            vatNumber = VatNumber.Empty,
            createdAt = now,
            updatedAt = now
        ),
        Tenant(
            id = TenantId("00000000-0000-0000-0000-000000000003"),
            type = TenantType.Freelancer,
            legalName = LegalName("Freelance Studio"),
            displayName = DisplayName("Studio"),
            subscription = SubscriptionTier.default,
            status = TenantStatus.Active,
            language = Language.En,
            vatNumber = VatNumber("BE0123456789"),
            createdAt = now,
            updatedAt = now
        )
    )
}

private fun sampleUser(): User {
    val now = LocalDateTime(2024, 1, 2, 9, 0)
    return User(
        id = UserId("00000000-0000-0000-0000-000000000010"),
        email = Email("user@example.com"),
        firstName = Name("Ada"),
        lastName = Name("Lovelace"),
        emailVerified = true,
        createdAt = now,
        updatedAt = now
    )
}

private fun Paparazzi.snapshotAllViewports(
    baseName: String,
    viewport: ScreenshotViewport,
    content: @Composable () -> Unit
) {
    snapshot("${baseName}_${viewport.displayName}_light") {
        ScreenshotTestWrapper(isDarkMode = false, screenSize = viewport.screenSize) {
            content()
        }
    }
    snapshot("${baseName}_${viewport.displayName}_dark") {
        ScreenshotTestWrapper(isDarkMode = true, screenSize = viewport.screenSize) {
            content()
        }
    }
}
