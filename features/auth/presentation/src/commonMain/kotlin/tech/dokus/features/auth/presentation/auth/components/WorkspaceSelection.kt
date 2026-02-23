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
import tech.dokus.aura.resources.workspace_select_title
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.tiles.AddCompanyTile
import tech.dokus.foundation.aura.components.tiles.CompanyTile
import tech.dokus.foundation.aura.constrains.Constraints

@Stable
@Composable
private fun Modifier.widthInWorkspaceItem(): Modifier =
    widthIn(min = 120.dp, max = 180.dp)

@Composable
fun WorkspaceSelectionBody(
    state: DokusState<List<Tenant>>,
    onTenantClick: (Tenant) -> Unit,
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
        onAddTenantClick = onAddTenantClick,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StateDrivenContent(
    state: DokusState<List<Tenant>>,
    onTenantClick: (Tenant) -> Unit,
    onAddTenantClick: () -> Unit,
) {
    when (state) {
        is DokusState.Success -> {
            val tenants = state.data
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
                    ) {
                        onTenantClick(tenant)
                    }
                }

                AddCompanyTile(
                    modifier = Modifier.widthInWorkspaceItem(),
                    onClick = onAddTenantClick,
                )
            }
        }

        is DokusState.Error -> DokusErrorContent(state.exception, state.retryHandler)

        else -> DokusLoader()
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun WorkspaceSelectionBodyPreview(
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
