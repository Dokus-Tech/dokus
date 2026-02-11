package tech.dokus.features.auth.presentation.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.Paparazzi
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.app_slogan
import tech.dokus.aura.resources.brand_motto
import tech.dokus.aura.resources.copyright
import tech.dokus.aura.resources.slogan_line_2
import tech.dokus.aura.resources.slogan_line_3
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.features.auth.mvi.ForgotPasswordState
import tech.dokus.features.auth.mvi.NewPasswordState
import tech.dokus.features.auth.mvi.WorkspaceCreateState
import tech.dokus.features.auth.mvi.WorkspaceSelectState
import tech.dokus.features.auth.presentation.auth.model.AddressFormState
import tech.dokus.features.auth.presentation.auth.model.WorkspaceWizardStep
import tech.dokus.features.auth.presentation.auth.screen.ForgotPasswordScreen
import tech.dokus.features.auth.presentation.auth.screen.NewPasswordScreen
import tech.dokus.features.auth.presentation.auth.screen.ProfileScreen
import tech.dokus.features.auth.presentation.auth.screen.WorkspaceCreateScreen
import tech.dokus.features.auth.presentation.auth.screen.WorkspaceSelectScreen

@RunWith(Parameterized::class)
class AuthScreenScreenshotTest(private val viewport: ScreenshotViewport) {

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
    fun workspaceSelectScreen_content() {
        val state = WorkspaceSelectState.Content(sampleTenants())
        paparazzi.snapshotAllViewports("WorkspaceSelectScreen_content", viewport) {
            WorkspaceSelectScreen(
                state = state,
                onIntent = {},
                onAddTenantClick = {},
                triggerWarp = false,
                onWarpComplete = {}
            )
        }
    }

    @Test
    fun workspaceCreateScreen_typeSelection() {
        val state = WorkspaceCreateState.Wizard(
            step = WorkspaceWizardStep.TypeSelection,
            tenantType = TenantType.Company,
            hasFreelancerWorkspace = false,
            userName = "Ada Lovelace",
            companyName = LegalName(""),
            address = AddressFormState()
        )
        paparazzi.snapshotAllViewports("WorkspaceCreateScreen_typeSelection", viewport) {
            WorkspaceCreateScreen(
                state = state,
                onIntent = {},
                onNavigateUp = {},
                triggerWarp = false,
                onWarpComplete = {}
            )
        }
    }

    @Test
    fun sloganScreen_static() {
        paparazzi.snapshotAllViewports("SloganScreen_static", viewport) {
            SloganContentStatic()
        }
    }

    @Test
    fun profileScreen() {
        paparazzi.snapshotAllViewports("ProfileScreen", viewport) {
            ProfileScreen()
        }
    }

    @Test
    fun forgotPasswordScreen() {
        paparazzi.snapshotAllViewports("ForgotPasswordScreen", viewport) {
            ForgotPasswordScreen(
                state = ForgotPasswordState.Idle(),
                onIntent = {}
            )
        }
    }

    @Test
    fun newPasswordScreen() {
        paparazzi.snapshotAllViewports("NewPasswordScreen", viewport) {
            NewPasswordScreen(
                state = NewPasswordState.Idle(),
                onIntent = {}
            )
        }
    }
}

@Composable
private fun SloganContentStatic() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                Text(
                    text = stringResource(Res.string.app_slogan),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.slogan_line_2),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(Res.string.slogan_line_3),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.brand_motto),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = stringResource(Res.string.copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
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
            createdAt = now,
            vatNumber = VatNumber.Empty,
            updatedAt = now
        ),
        Tenant(
            id = TenantId("00000000-0000-0000-0000-000000000002"),
            type = TenantType.Company,
            legalName = LegalName("Dokus Labs"),
            displayName = DisplayName("Dokus"),
            subscription = SubscriptionTier.SelfHosted,
            status = TenantStatus.Active,
            vatNumber = VatNumber.Empty,
            language = Language.En,
            createdAt = now,
            updatedAt = now
        ),
        Tenant(
            id = TenantId("00000000-0000-0000-0000-000000000003"),
            type = TenantType.Freelancer,
            legalName = LegalName("Freelance Studio"),
            displayName = DisplayName("Studio"),
            subscription = SubscriptionTier.default,
            vatNumber = VatNumber.Empty,
            status = TenantStatus.Active,
            language = Language.En,
            createdAt = now,
            updatedAt = now
        )
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
