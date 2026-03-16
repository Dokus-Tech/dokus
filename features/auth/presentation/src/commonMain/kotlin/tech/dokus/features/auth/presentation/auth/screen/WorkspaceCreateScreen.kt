@file:Suppress("TopLevelPropertyNaming")

package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tech.dokus.domain.LegalName
import tech.dokus.features.auth.mvi.WorkspaceCreateIntent
import tech.dokus.features.auth.mvi.WorkspaceCreateState
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingCenteredShell
import tech.dokus.features.auth.presentation.auth.components.steps.CompanyNameStep
import tech.dokus.features.auth.presentation.auth.components.steps.TypeSelectionStep
import tech.dokus.features.auth.presentation.auth.components.steps.VatAndAddressStep
import tech.dokus.features.auth.presentation.auth.model.WorkspaceCreateType
import tech.dokus.features.auth.presentation.auth.model.WorkspaceWizardStep
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.features.auth.mvi.WorkspaceCreateUserInfo
import tech.dokus.foundation.aura.components.background.RadialRevealEffect
import tech.dokus.foundation.aura.extensions.dismissKeyboardOnTapOutside
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private const val ContentFadeOutDurationMs = 600
private const val CompanyLookupDebounceMs = 300L
private const val CompanyLookupMinCharacters = 3
private val StepContentMinHeight = 320.dp
private val WizardContentMaxWidth = 520.dp
private val WizardTypeSelectionMaxWidth = 560.dp
private val WorkspaceCreateDefaultShellMaxWidth = 520.dp
private val WorkspaceCreateLookupShellMaxWidth = 980.dp
private val WizardLookupMaxWidth = 980.dp
private val LookupPaneMaxHeight = 540.dp

@Composable
internal fun WorkspaceCreateScreen(
    state: WorkspaceCreateState,
    onIntent: (WorkspaceCreateIntent) -> Unit,
    onNavigateUp: () -> Unit,
    triggerWarp: Boolean,
    onWarpComplete: () -> Unit,
    copyrightYear: String? = null,
) {
    var isRevealActive by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(true) }

    LaunchedEffect(triggerWarp) {
        if (triggerWarp) {
            isRevealActive = true
            contentVisible = false
        }
    }

    Scaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(ContentFadeOutDurationMs)),
            ) {
                if (state.isReady) {
                    OnboardingCenteredShell(
                        modifier = Modifier.dismissKeyboardOnTapOutside(),
                        copyrightYear = copyrightYear,
                        contentMaxWidth = when (state.step) {
                            WorkspaceWizardStep.CompanyName,
                            WorkspaceWizardStep.VatAndAddress -> WorkspaceCreateLookupShellMaxWidth

                            WorkspaceWizardStep.TypeSelection -> WorkspaceCreateDefaultShellMaxWidth
                        },
                    ) {
                        WorkspaceCreateContent(
                            state = state,
                            onIntent = onIntent,
                            onBackPress = onNavigateUp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            RadialRevealEffect(
                isActive = isRevealActive,
                onAnimationComplete = onWarpComplete,
            )
        }
    }
}

@Composable
private fun WorkspaceCreateContent(
    state: WorkspaceCreateState,
    onIntent: (WorkspaceCreateIntent) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier,
) {
    val steps = WorkspaceWizardStep.stepsForType(state.workspaceType)
    val pagerState = rememberPagerState(pageCount = { steps.size })

    LaunchedEffect(state.step) {
        val targetPage = steps.indexOf(state.step)
        if (targetPage >= 0 && targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(state.step, state.companyName.value) {
        if (state.step != WorkspaceWizardStep.CompanyName) return@LaunchedEffect

        val query = state.companyName.value.trim()
        if (query.length < CompanyLookupMinCharacters) return@LaunchedEffect

        delay(CompanyLookupDebounceMs)
        onIntent(WorkspaceCreateIntent.LookupCompany)
    }

    Column(
        modifier = modifier.widthIn(
            max = when (state.step) {
                WorkspaceWizardStep.TypeSelection -> WizardTypeSelectionMaxWidth
                WorkspaceWizardStep.CompanyName -> WizardLookupMaxWidth
                WorkspaceWizardStep.VatAndAddress -> WizardLookupMaxWidth
            }
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = StepContentMinHeight, max = LookupPaneMaxHeight)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                when (steps[page]) {
                    WorkspaceWizardStep.TypeSelection -> {
                        TypeSelectionStep(
                            hasFreelancerWorkspace = state.hasFreelancerWorkspace,
                            onTypeSelected = { type ->
                                onIntent(WorkspaceCreateIntent.SelectType(type))
                                onIntent(WorkspaceCreateIntent.NextClicked)
                            },
                            onBackPress = onBackPress,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    WorkspaceWizardStep.CompanyName -> {
                        CompanyNameStep(
                            query = state.companyName.value,
                            lookupState = state.lookupState,
                            onQueryChanged = { name ->
                                onIntent(WorkspaceCreateIntent.UpdateCompanyName(LegalName(name)))
                            },
                            onResultSelected = { entity ->
                                onIntent(WorkspaceCreateIntent.SelectEntity(entity))
                            },
                            onEnterManually = {
                                onIntent(
                                    WorkspaceCreateIntent.UpdateCompanyName(
                                        LegalName(
                                            state.companyName.value
                                        )
                                    )
                                )
                                onIntent(WorkspaceCreateIntent.EnterManually)
                            },
                            onBackPress = { onIntent(WorkspaceCreateIntent.BackClicked) },
                        )
                    }

                    WorkspaceWizardStep.VatAndAddress -> {
                        VatAndAddressStep(
                            companyName = state.companyName.value,
                            vatNumber = state.vatNumber,
                            address = state.address,
                            canCreate = state.canProceed,
                            isSubmitting = state.isCreating,
                            onCompanyNameChanged = { name ->
                                onIntent(WorkspaceCreateIntent.UpdateCompanyName(LegalName(name)))
                            },
                            onVatNumberChanged = { vatNumber ->
                                onIntent(WorkspaceCreateIntent.UpdateVatNumber(vatNumber))
                            },
                            onAddressChanged = { address ->
                                onIntent(WorkspaceCreateIntent.UpdateAddress(address))
                            },
                            onCreate = { onIntent(WorkspaceCreateIntent.NextClicked) },
                            onBackPress = { onIntent(WorkspaceCreateIntent.BackClicked) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun WorkspaceCreateScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        WorkspaceCreateScreen(
            state = WorkspaceCreateState(
                userInfo = DokusState.success(
                    WorkspaceCreateUserInfo(hasFreelancerWorkspace = false, userName = "John Doe")
                ),
            ),
            onIntent = {},
            onNavigateUp = {},
            triggerWarp = false,
            onWarpComplete = {},
            copyrightYear = "2026",
        )
    }
}

@Preview(name = "Workspace Create Desktop", widthDp = 1200, heightDp = 760)
@Composable
private fun WorkspaceCreateScreenDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        WorkspaceCreateScreen(
            state = WorkspaceCreateState(
                userInfo = DokusState.success(
                    WorkspaceCreateUserInfo(hasFreelancerWorkspace = false, userName = "John Doe")
                ),
                workspaceType = WorkspaceCreateType.Bookkeeper,
            ),
            onIntent = {},
            onNavigateUp = {},
            triggerWarp = false,
            onWarpComplete = {},
            copyrightYear = "2026",
        )
    }
}
