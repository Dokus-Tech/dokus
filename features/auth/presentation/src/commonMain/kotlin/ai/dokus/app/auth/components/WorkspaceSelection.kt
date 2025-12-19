package ai.dokus.app.auth.components

import tech.dokus.foundation.app.state.DokusState
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.components.tiles.AddCompanyTile
import ai.dokus.foundation.design.components.tiles.CompanyTile
import ai.dokus.foundation.domain.model.Tenant
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

@Stable
private fun Modifier.widthInWorkspaceItem(): Modifier = widthIn(min = 140.dp, max = 320.dp)

@Composable
fun WorkspaceSelectionBody(
    state: DokusState<List<Tenant>>,
    onTenantClick: (Tenant) -> Unit,
    onAddTenantClick: () -> Unit,
) {
    SectionTitle(
        text = "Select your workspace",
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
                        label = tenant.displayName.value
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
