package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.workspace_bookkeeper_console
import tech.dokus.aura.resources.workspace_select_title
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.tiles.AddCompanyTile
import tech.dokus.foundation.aura.components.tiles.CompanyTile
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized

@Stable
@Composable
private fun Modifier.widthInWorkspaceItem(): Modifier =
    widthIn(min = 120.dp, max = 180.dp)

@Composable
fun WorkspaceSelectionBody(
    state: DokusState<List<Tenant>>,
    onTenantClick: (Tenant) -> Unit,
    onAddTenantClick: () -> Unit,
    onBookkeeperConsoleClick: (() -> Unit)? = null,
) {
    Text(
        text = stringResource(Res.string.workspace_select_title),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(Constraints.Spacing.xxLarge))

    StateDrivenContent(
        state = state,
        onTenantClick = onTenantClick,
        onAddTenantClick = onAddTenantClick,
        onBookkeeperConsoleClick = onBookkeeperConsoleClick,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StateDrivenContent(
    state: DokusState<List<Tenant>>,
    onTenantClick: (Tenant) -> Unit,
    onAddTenantClick: () -> Unit,
    onBookkeeperConsoleClick: (() -> Unit)? = null,
) {
    when (state) {
        is DokusState.Success -> {
            val tenants = state.data
            val showBCTile = onBookkeeperConsoleClick != null &&
                tenants.any { it.role == UserRole.Accountant }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    Constraints.Spacing.large,
                    alignment = Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
                maxItemsInEachRow = Int.MAX_VALUE,
            ) {
                tenants.forEach { tenant ->
                    CompanyTile(
                        modifier = Modifier.widthInWorkspaceItem(),
                        initial = tenant.displayName.initialOrEmpty,
                        label = tenant.displayName.value,
                        avatarUrl = tenant.avatar?.small,
                        badge = tenant.role?.localized,
                    ) {
                        onTenantClick(tenant)
                    }
                }

                if (showBCTile) {
                    BookkeeperConsoleTile(
                        modifier = Modifier.widthInWorkspaceItem(),
                        onClick = onBookkeeperConsoleClick!!,
                    )
                }

                AddCompanyTile(
                    modifier = Modifier.widthInWorkspaceItem(),
                    onClick = onAddTenantClick,
                )
            }
        }

        is DokusState.Error -> DokusErrorContent(
            text = state.exception.localized,
            retryHandler = state.retryHandler,
        )

        else -> DokusLoader()
    }
}

@Composable
private fun BookkeeperConsoleTile(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val label = stringResource(Res.string.workspace_bookkeeper_console)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        DokusCardSurface(onClick = onClick) {
            Box(
                modifier = Modifier.size(Constraints.AvatarSize.tile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Business,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private fun previewTenant(
    name: String,
    role: UserRole,
): Tenant = Tenant(
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
private fun WorkspaceSelectionEmptyPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        WorkspaceSelectionBody(
            state = tech.dokus.foundation.app.state.DokusStateSimple.Success(emptyList()),
            onTenantClick = {},
            onAddTenantClick = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun WorkspaceSelectionWithBCPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        WorkspaceSelectionBody(
            state = tech.dokus.foundation.app.state.DokusStateSimple.Success(
                listOf(
                    previewTenant("Dokus Tech", UserRole.Owner),
                    previewTenant("Client Corp", UserRole.Accountant),
                ),
            ),
            onTenantClick = {},
            onAddTenantClick = {},
            onBookkeeperConsoleClick = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun WorkspaceSelectionOwnerOnlyPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        WorkspaceSelectionBody(
            state = tech.dokus.foundation.app.state.DokusStateSimple.Success(
                listOf(
                    previewTenant("Dokus Tech", UserRole.Owner),
                ),
            ),
            onTenantClick = {},
            onAddTenantClick = {},
            onBookkeeperConsoleClick = {},
        )
    }
}
