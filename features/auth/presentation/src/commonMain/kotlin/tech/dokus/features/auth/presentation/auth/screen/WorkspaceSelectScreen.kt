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
import tech.dokus.features.auth.mvi.WorkspaceSelectIntent
import tech.dokus.features.auth.mvi.WorkspaceSelectState
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
    onBookkeeperConsoleClick: (() -> Unit)? = null,
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
                        onTenantClick = { tenant ->
                            onIntent(WorkspaceSelectIntent.SelectTenant(tenant.id))
                        },
                        onAddTenantClick = onAddTenantClick,
                        onBookkeeperConsoleClick = onBookkeeperConsoleClick,
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

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private fun previewTenant(
    name: String,
    role: tech.dokus.domain.enums.UserRole,
): tech.dokus.domain.model.Tenant = tech.dokus.domain.model.Tenant(
    id = tech.dokus.domain.ids.TenantId(kotlin.uuid.Uuid.random()),
    type = tech.dokus.domain.enums.TenantType.Freelancer,
    legalName = tech.dokus.domain.LegalName(name),
    displayName = tech.dokus.domain.DisplayName(name),
    subscription = tech.dokus.domain.enums.SubscriptionTier.Core,
    status = tech.dokus.domain.enums.TenantStatus.Active,
    language = tech.dokus.domain.enums.Language.En,
    vatNumber = tech.dokus.domain.ids.VatNumber("BE0123456789"),
    role = role,
    createdAt = kotlinx.datetime.LocalDateTime(2025, 1, 1, 0, 0),
    updatedAt = kotlinx.datetime.LocalDateTime(2025, 1, 1, 0, 0),
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
            state = WorkspaceSelectState.Content(
                data = listOf(
                    previewTenant("Dokus Tech", tech.dokus.domain.enums.UserRole.Owner),
                    previewTenant("Client Corp", tech.dokus.domain.enums.UserRole.Accountant),
                ),
            ),
            onIntent = {},
            onAddTenantClick = {},
            onBookkeeperConsoleClick = {},
            triggerWarp = false,
            onWarpComplete = {},
        )
    }
}
