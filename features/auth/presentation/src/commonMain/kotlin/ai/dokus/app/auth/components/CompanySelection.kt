package ai.dokus.app.auth.components

import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.design.components.common.ErrorBox
import ai.dokus.foundation.design.components.text.AppNameText
import ai.dokus.foundation.design.components.text.CopyRightText
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.components.tiles.AddCompanyTile
import ai.dokus.foundation.design.components.tiles.CompanyTile
import ai.dokus.foundation.design.constrains.limitWidthCenteredContent
import ai.dokus.foundation.domain.model.Organization
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Layout variants for the company selection screen.
 */
enum class CompanySelectLayout { Desktop, Mobile }

/**
 * Reusable, adaptive content for the company selection screen.
 * Renders header, title, state-driven content and footer using Material theme.
 */
@Composable
fun CompanySelection(
    state: DokusState<List<Organization>>,
    layout: CompanySelectLayout,
    onCompanyClick: (Organization?) -> Unit,
    onAddCompanyClick: () -> Unit,
) {
    when (layout) {
        CompanySelectLayout.Desktop -> DesktopLayout(state, onCompanyClick, onAddCompanyClick)
        CompanySelectLayout.Mobile -> MobileLayout(state, onCompanyClick, onAddCompanyClick)
    }
}

@Composable
private fun DesktopLayout(
    state: DokusState<List<Organization>>,
    onCompanyClick: (Organization?) -> Unit,
    onAddCompanyClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxHeight().align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AppNameText()

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SectionTitle(
                    text = "Select your company",
                    horizontalArrangement = Arrangement.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                StateDrivenContent(state, isMobile = false, onCompanyClick, onAddCompanyClick)
            }

            CopyRightText()
        }
    }
}

@Composable
private fun MobileLayout(
    state: DokusState<List<Organization>>,
    onCompanyClick: (Organization?) -> Unit,
    onAddCompanyClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppNameText()

        Spacer(modifier = Modifier.height(32.dp))

        Column(modifier = Modifier.limitWidthCenteredContent()) {
            SectionTitle(
                text = "Select your company",
                horizontalArrangement = Arrangement.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            StateDrivenContent(state, isMobile = true, onCompanyClick, onAddCompanyClick)
        }

        Spacer(modifier = Modifier.height(32.dp))
        CopyRightText()
    }
}

@Composable
private fun StateDrivenContent(
    state: DokusState<List<Organization>>,
    isMobile: Boolean,
    onCompanyClick: (Organization?) -> Unit,
    onAddCompanyClick: () -> Unit,
) {
    when (state) {
        is DokusState.Success -> {
            val companies = state.data
            if (isMobile) {
                val rows = companies.chunked(2)
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pair.forEach { organization ->
                            CompanyTile(
                                modifier = Modifier.weight(1f),
                                initial = organization.legalName.initialOrEmpty,
                                label = organization.legalName.value
                            ) { onCompanyClick(organization) }
                        }
                        if (pair.size == 1) {
                            AddCompanyTile(
                                modifier = Modifier.weight(1f),
                                onClick = onAddCompanyClick
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (companies.isEmpty() || companies.size % 2 == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AddCompanyTile(onClick = onAddCompanyClick)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    companies.forEach { organization ->
                        CompanyTile(
                            modifier = Modifier,
                            initial = organization.legalName.initialOrEmpty,
                            label = organization.legalName.value
                        ) { onCompanyClick(organization) }
                    }
                    AddCompanyTile(onClick = onAddCompanyClick)
                }
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
