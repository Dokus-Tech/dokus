@file:Suppress("TopLevelPropertyNaming")

package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
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
import tech.dokus.foundation.aura.components.background.WarpJumpEffect

private const val ContentFadeOutDurationMs = 600
private const val NavigationDelayMs = 100L

@Composable
internal fun WorkspaceSelectScreen(
    state: WorkspaceSelectState,
    onIntent: (WorkspaceSelectIntent) -> Unit,
    onAddTenantClick: () -> Unit,
    triggerWarp: Boolean,
    onWarpComplete: () -> Unit,
) {
    var isWarpActive by remember { mutableStateOf(false) }
    var selectedItemPosition by remember { mutableStateOf<Offset?>(null) }
    var shouldNavigate by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(true) }

    LaunchedEffect(triggerWarp) {
        if (triggerWarp) {
            isWarpActive = true
            contentVisible = false
        }
    }

    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            delay(NavigationDelayMs)
            onWarpComplete()
        }
    }

    Scaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(ContentFadeOutDurationMs)),
            ) {
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

            WarpJumpEffect(
                isActive = isWarpActive,
                selectedItemPosition = selectedItemPosition,
                onAnimationComplete = {
                    shouldNavigate = true
                },
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
            triggerWarp = false,
            onWarpComplete = {},
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
            triggerWarp = false,
            onWarpComplete = {},
        )
    }
}
