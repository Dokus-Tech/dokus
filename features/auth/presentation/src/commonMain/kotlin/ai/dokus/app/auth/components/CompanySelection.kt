package ai.dokus.app.auth.components

import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.design.components.common.ErrorBox
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.components.tiles.AddCompanyTile
import ai.dokus.foundation.design.components.tiles.CompanyTile
import ai.dokus.foundation.domain.model.Organization
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
private fun Modifier.widthInCompanyItem(): Modifier = widthIn(min = 140.dp, max = 320.dp)

@Composable
fun SelectionBody(
    state: DokusState<List<Organization>>,
    onCompanyClick: (Organization) -> Unit,
    onAddCompanyClick: () -> Unit,
) {
    SectionTitle(
        text = "Select your company",
        horizontalArrangement = Arrangement.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    StateDrivenContent(
        state = state,
        onCompanyClick = onCompanyClick,
        onAddCompanyClick = onAddCompanyClick
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StateDrivenContent(
    state: DokusState<List<Organization>>,
    onCompanyClick: (Organization) -> Unit,
    onAddCompanyClick: () -> Unit,
) {
    when (state) {
        is DokusState.Success -> {
            val companies = state.data
            // FlowRow adapts to the available width, suitable for both mobile and desktop.
            // Items use width constraints so they can expand when space allows.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = Int.MAX_VALUE
            ) {
                companies.forEach { organization ->
                    CompanyTile(
                        // Let tiles grow but keep a sensible minimum so rows wrap nicely on mobile.
                        modifier = Modifier.widthInCompanyItem(),
                        initial = organization.legalName.initialOrEmpty,
                        label = organization.legalName.value
                    ) { onCompanyClick(organization) }
                }
                // Always provide an option to add a company; participates in flow like others.
                AddCompanyTile(
                    modifier = Modifier.widthInCompanyItem(),
                    onClick = onAddCompanyClick
                )
            }
        }

        is DokusState.Error -> {
            ErrorBox(exception = state.exception) { state.retryHandler.retry() }
        }

        else -> {
            CircularProgressIndicator()
        }
    }
}
