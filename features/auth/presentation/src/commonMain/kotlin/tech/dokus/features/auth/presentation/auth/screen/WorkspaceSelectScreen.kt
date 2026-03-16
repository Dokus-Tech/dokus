package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.enums.FirmRole
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.auth.FirmWorkspaceSummary
import tech.dokus.domain.model.auth.TenantWorkspaceSummary
import tech.dokus.features.auth.mvi.WorkspaceSelectData
import tech.dokus.features.auth.mvi.WorkspaceSelectIntent
import tech.dokus.features.auth.mvi.WorkspaceSelectState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.features.auth.presentation.auth.components.WorkspaceSelectionBody
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingCenteredShell

@Composable
internal fun WorkspaceSelectScreen(
    state: WorkspaceSelectState,
    onIntent: (WorkspaceSelectIntent) -> Unit,
    onAddTenantClick: () -> Unit,
) {
    Scaffold {
        OnboardingCenteredShell {
            WorkspaceSelectionBody(
                state = state,
                onTenantClick = { tenantId ->
                    onIntent(WorkspaceSelectIntent.SelectTenant(tenantId))
                },
                onFirmClick = { firmId ->
                    onIntent(WorkspaceSelectIntent.SelectFirm(firmId))
                },
                onAddTenantClick = onAddTenantClick,
            )
        }
    }
}

private fun previewTenant(
    name: String,
    role: UserRole,
): TenantWorkspaceSummary = TenantWorkspaceSummary(
    id = TenantId.generate(),
    name = DisplayName(name),
    vatNumber = VatNumber("BE0123456789"),
    role = role,
    type = TenantType.Company,
)

private fun previewFirm(
    name: String,
    count: Int,
): FirmWorkspaceSummary = FirmWorkspaceSummary(
    id = FirmId.generate(),
    name = DisplayName(name),
    vatNumber = VatNumber("BE0123456789"),
    role = FirmRole.Owner,
    clientCount = count,
)

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun WorkspaceSelectScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        WorkspaceSelectScreen(
            state = WorkspaceSelectState(
                workspaces = DokusState.success(
                    WorkspaceSelectData(
                        tenants = listOf(
                            previewTenant("Dokus Tech", UserRole.Owner),
                            previewTenant("Client Corp", UserRole.Admin),
                        ),
                        firms = listOf(previewFirm("Kantoor Boonen", 8)),
                    )
                ),
            ),
            onIntent = {},
            onAddTenantClick = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Workspace Select Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun WorkspaceSelectScreenDesktopPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        WorkspaceSelectScreen(
            state = WorkspaceSelectState(
                workspaces = DokusState.success(
                    WorkspaceSelectData(
                        tenants = listOf(
                            previewTenant("Dokus Tech", UserRole.Owner),
                            previewTenant("Client Corp", UserRole.Editor),
                        ),
                        firms = emptyList(),
                    )
                ),
            ),
            onIntent = {},
            onAddTenantClick = {},
        )
    }
}
