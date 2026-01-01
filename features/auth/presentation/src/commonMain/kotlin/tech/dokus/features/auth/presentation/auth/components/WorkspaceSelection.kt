package tech.dokus.features.auth.presentation.auth.components

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.workspace_select_title
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.components.tiles.AddCompanyTile
import tech.dokus.foundation.aura.components.tiles.CompanyTile
import tech.dokus.domain.model.Tenant
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

@Stable
private fun Modifier.widthInWorkspaceItem(): Modifier = widthIn(min = 140.dp, max = 320.dp)

@Composable
fun WorkspaceSelectionBody(
    state: DokusState<List<Tenant>>,
    onTenantClick: (Tenant) -> Unit,
    onAddTenantClick: () -> Unit,
) {
    SectionTitle(
        text = stringResource(Res.string.workspace_select_title),
        horizontalArrangement = Arrangement.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    StateDrivenContent(
        state = state,
        onTenantClick = onTenantClick,
        onAddTenantClick = onAddTenantClick
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
            // FlowRow adapts to the available width, suitable for both mobile and desktop.
            // Items use width constraints so they can expand when space allows.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = Int.MAX_VALUE
            ) {
                tenants.forEach { tenant ->
                    CompanyTile(
                        // Let tiles grow but keep a sensible minimum so rows wrap nicely on mobile.
                        modifier = Modifier.widthInWorkspaceItem(),
                        initial = tenant.displayName.initialOrEmpty,
                        label = tenant.displayName.value,
                        avatarUrl = tenant.avatar?.small
                    ) { onTenantClick(tenant) }
                }
                // Always provide an option to add a tenant; participates in the flow like others.
                AddCompanyTile(
                    modifier = Modifier.widthInWorkspaceItem(),
                    onClick = onAddTenantClick
                )
            }
        }

        is DokusState.Error -> {
            DokusErrorContent(state.exception, state.retryHandler)
        }

        else -> {
            CircularProgressIndicator()
        }
    }
}
