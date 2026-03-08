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
import tech.dokus.foundation.aura.components.background.WarpJumpEffect
import tech.dokus.foundation.aura.extensions.dismissKeyboardOnTapOutside
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private const val ContentFadeOutDurationMs = 600
private const val NavigationDelayMs = 100L
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
    var isWarpActive by remember { mutableStateOf(false) }
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

    val wizardState = state as? WorkspaceCreateState.Wizard
    val isSubmitting =
        state is WorkspaceCreateState.Loading || state is WorkspaceCreateState.Creating

    Scaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(ContentFadeOutDurationMs)),
            ) {
                if (wizardState != null) {
                    OnboardingCenteredShell(
                        modifier = Modifier.dismissKeyboardOnTapOutside(),
                        copyrightYear = copyrightYear,
                        contentMaxWidth = when (wizardState.step) {
                            WorkspaceWizardStep.CompanyName,
                            WorkspaceWizardStep.VatAndAddress -> WorkspaceCreateLookupShellMaxWidth

                            WorkspaceWizardStep.TypeSelection -> WorkspaceCreateDefaultShellMaxWidth
                        },
                    ) {
                        WorkspaceCreateContent(
                            wizardState = wizardState,
                            isSubmitting = isSubmitting,
                            onIntent = onIntent,
                            onBackPress = onNavigateUp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            WarpJumpEffect(
                isActive = isWarpActive,
                selectedItemPosition = null,
                onAnimationComplete = {
                    shouldNavigate = true
                },
            )
        }
    }
}

@Composable
private fun WorkspaceCreateContent(
    wizardState: WorkspaceCreateState.Wizard,
    isSubmitting: Boolean,
    onIntent: (WorkspaceCreateIntent) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier,
) {
    val steps = WorkspaceWizardStep.stepsForType(wizardState.workspaceType)
    val pagerState = rememberPagerState(pageCount = { steps.size })

    LaunchedEffect(wizardState.step) {
        val targetPage = steps.indexOf(wizardState.step)
        if (targetPage >= 0 && targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(wizardState.step, wizardState.companyName.value) {
        if (wizardState.step != WorkspaceWizardStep.CompanyName) return@LaunchedEffect

        val query = wizardState.companyName.value.trim()
        if (query.length < CompanyLookupMinCharacters) return@LaunchedEffect

        delay(CompanyLookupDebounceMs)
        onIntent(WorkspaceCreateIntent.LookupCompany)
    }

    Column(
        modifier = modifier.widthIn(
            max = when (wizardState.step) {
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
                            hasFreelancerWorkspace = wizardState.hasFreelancerWorkspace,
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
                            query = wizardState.companyName.value,
                            lookupState = wizardState.lookupState,
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
                                            wizardState.companyName.value
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
                            companyName = wizardState.companyName.value,
                            vatNumber = wizardState.vatNumber,
                            address = wizardState.address,
                            canCreate = wizardState.canProceed,
                            isSubmitting = isSubmitting,
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
            state = WorkspaceCreateState.Wizard(),
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
            state = WorkspaceCreateState.Wizard(
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
