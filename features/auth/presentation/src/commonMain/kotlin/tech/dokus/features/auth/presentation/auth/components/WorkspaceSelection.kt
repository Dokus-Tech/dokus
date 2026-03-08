package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.console_clients_count
import tech.dokus.aura.resources.workspace_add
import tech.dokus.aura.resources.workspace_select_title
import tech.dokus.domain.DisplayName
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.FirmRole
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.auth.FirmWorkspaceSummary
import tech.dokus.domain.model.auth.TenantWorkspaceSummary
import tech.dokus.features.auth.mvi.WorkspaceSelectState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.tiles.AddCompanyTile
import tech.dokus.foundation.aura.components.tiles.CompanyTile
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import coil3.ImageLoader
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.foundation.app.network.rememberAuthenticatedImageLoader
import tech.dokus.foundation.app.network.rememberResolvedApiUrl

@Stable
@Composable
private fun Modifier.widthInWorkspaceItem(): Modifier =
    widthIn(min = 120.dp, max = 180.dp)

@Composable
fun WorkspaceSelectionBody(
    state: WorkspaceSelectState,
    onTenantClick: (TenantId) -> Unit,
    onFirmClick: (FirmId) -> Unit,
    onAddTenantClick: () -> Unit,
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
        onFirmClick = onFirmClick,
        onAddTenantClick = onAddTenantClick,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StateDrivenContent(
    state: WorkspaceSelectState,
    onTenantClick: (TenantId) -> Unit,
    onFirmClick: (FirmId) -> Unit,
    onAddTenantClick: () -> Unit,
) {
    when (state) {
        WorkspaceSelectState.Loading -> DokusLoader()
        is WorkspaceSelectState.Error -> DokusErrorContent(
            exception = state.exception,
            retryHandler = state.retryHandler,
        )
        is WorkspaceSelectState.Content,
        is WorkspaceSelectState.SelectingTenant,
        is WorkspaceSelectState.SelectingFirm,
        -> {
            val contentState = state.toContentState()
            val sortedWorkspaceEntries = buildWorkspaceEntries(contentState)
            val imageLoader = rememberAuthenticatedImageLoader()

            WorkspaceSection(
                items = {
                    sortedWorkspaceEntries.forEach { entry ->
                        WorkspaceTile(
                            entry = entry,
                            imageLoader = imageLoader,
                            onTenantClick = onTenantClick,
                            onFirmClick = onFirmClick,
                        )
                    }

                    AddCompanyTile(
                        modifier = Modifier.widthInWorkspaceItem(),
                        label = stringResource(Res.string.workspace_add),
                        onClick = onAddTenantClick,
                    )
                },
            )
        }
    }
}

@Composable
private fun WorkspaceTile(
    entry: WorkspaceEntry,
    imageLoader: ImageLoader,
    onTenantClick: (TenantId) -> Unit,
    onFirmClick: (FirmId) -> Unit,
) {
    when (entry) {
        is WorkspaceEntry.Firm -> {
            CompanyTile(
                modifier = Modifier.widthInWorkspaceItem(),
                initial = entry.summary.name.initialOrEmpty,
                label = entry.summary.name.value,
                badge = stringResource(
                    Res.string.console_clients_count,
                    entry.summary.clientCount,
                ),
                imageLoader = imageLoader,
            ) {
                onFirmClick(entry.summary.id)
            }
        }

        is WorkspaceEntry.Tenant -> {
            CompanyTile(
                modifier = Modifier.widthInWorkspaceItem(),
                initial = entry.summary.name.initialOrEmpty,
                label = entry.summary.name.value,
                avatarUrl = rememberResolvedApiUrl(entry.summary.avatar?.medium),
                badge = entry.summary.role.localized,
                imageLoader = imageLoader,
            ) {
                onTenantClick(entry.summary.id)
            }
        }
    }
}

private sealed interface WorkspaceEntry {
    val sortName: String
    val stableKey: String

    data class Tenant(
        val summary: TenantWorkspaceSummary,
    ) : WorkspaceEntry {
        override val sortName: String = summary.name.value
        override val stableKey: String = "tenant:${summary.id}"
    }

    data class Firm(
        val summary: FirmWorkspaceSummary,
    ) : WorkspaceEntry {
        override val sortName: String = summary.name.value
        override val stableKey: String = "firm:${summary.id}"
    }
}

private fun buildWorkspaceEntries(
    state: WorkspaceSelectState.Content,
): List<WorkspaceEntry> {
    return buildList {
        addAll(state.tenants.map(WorkspaceEntry::Tenant))
        addAll(state.firms.map(WorkspaceEntry::Firm))
    }.sortedWith(
        compareBy<WorkspaceEntry>(
            { it.sortName.lowercase() },
            { it.sortName },
            { it.stableKey },
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceSection(
    items: @Composable () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            Constraints.Spacing.large,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
        maxItemsInEachRow = Int.MAX_VALUE,
    ) {
        items()
    }
}

private fun WorkspaceSelectState.toContentState(): WorkspaceSelectState.Content {
    return when (this) {
        is WorkspaceSelectState.Content -> this
        is WorkspaceSelectState.SelectingTenant -> WorkspaceSelectState.Content(
            tenants = tenants,
            firms = firms,
        )
        is WorkspaceSelectState.SelectingFirm -> WorkspaceSelectState.Content(
            tenants = tenants,
            firms = firms,
        )
        else -> WorkspaceSelectState.Content(emptyList(), emptyList())
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

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

@Preview
@Composable
private fun WorkspaceSelectionEmptyPreview(
    @PreviewParameter(
        PreviewParametersProvider::class,
    ) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        WorkspaceSelectionBody(
            state = WorkspaceSelectState.Content(
                tenants = listOf(previewTenant("Dokus Tech", UserRole.Owner)),
                firms = emptyList(),
            ),
            onTenantClick = {},
            onFirmClick = {},
            onAddTenantClick = {},
        )
    }
}

@Preview
@Composable
private fun WorkspaceSelectionWithPracticePreview(
    @PreviewParameter(
        PreviewParametersProvider::class,
    ) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        WorkspaceSelectionBody(
            state = WorkspaceSelectState.Content(
                tenants = listOf(
                    previewTenant("Dokus Tech", UserRole.Owner),
                    previewTenant("Client Corp", UserRole.Admin),
                ),
                firms = listOf(
                    previewFirm("Kantoor Boonen", 8),
                ),
            ),
            onTenantClick = {},
            onFirmClick = {},
            onAddTenantClick = {},
        )
    }
}

@Preview(name = "Workspace Selection Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun WorkspaceSelectionDesktopPreview(
    @PreviewParameter(
        PreviewParametersProvider::class,
    ) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        WorkspaceSelectionBody(
            state = WorkspaceSelectState.Content(
                tenants = listOf(
                    previewTenant("Dokus Tech", UserRole.Owner),
                    previewTenant("Client Corp", UserRole.Editor),
                ),
                firms = listOf(
                    previewFirm("Kantoor Boonen", 8),
                    previewFirm("Acme Accounting", 3),
                ),
            ),
            onTenantClick = {},
            onFirmClick = {},
            onAddTenantClick = {},
        )
    }
}
