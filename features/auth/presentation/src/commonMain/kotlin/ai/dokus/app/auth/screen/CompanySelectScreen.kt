package ai.dokus.app.auth.screen

import ai.dokus.app.auth.viewmodel.CompanySelectViewModel
import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.design.components.common.ErrorBox
import ai.dokus.foundation.design.components.text.AppNameText
import ai.dokus.foundation.design.components.text.CopyRightText
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.constrains.isLargeScreen
import ai.dokus.foundation.design.constrains.limitWidthCenteredContent
import ai.dokus.foundation.design.components.tiles.AddCompanyTile
import ai.dokus.foundation.design.components.tiles.CompanyTile
import ai.dokus.foundation.domain.model.Organization
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.navigation.replace
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CompanySelectScreen(
    viewModel: CompanySelectViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsState()

    if (isLargeScreen) {
        CompanySelectDesktop(
            state = state,
            onCompanyClick = { navController.replace(CoreDestination.Home) },
            onAddCompanyClick = { navController.navigateTo(AuthDestination.CompanyCreate) }
        )
    } else {
        CompanySelectMobile(
            state = state,
            onCompanyClick = { navController.replace(CoreDestination.Home) },
            onAddCompanyClick = { navController.navigateTo(AuthDestination.CompanyCreate) }
        )
    }
}

@Composable
private fun CompanySelectDesktop(
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

                when (state) {
                    is DokusState.Success -> {
                        val companies = state.data
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

                    is DokusState.Error -> {
                        ErrorBox(exception = state.exception) { state.retryHandler.retry() }
                    }

                    else -> {
                        CircularProgressIndicator()
                    }
                }
            }

            CopyRightText()
        }
    }
}

@Composable
private fun CompanySelectMobile(
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

            when (state) {
                is DokusState.Success -> {
                    val companies = state.data
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
                }

                is DokusState.Error -> {
                    ErrorBox(exception = state.exception) { state.retryHandler.retry() }
                }

                else -> {
                    CircularProgressIndicator()
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        CopyRightText()
    }
}