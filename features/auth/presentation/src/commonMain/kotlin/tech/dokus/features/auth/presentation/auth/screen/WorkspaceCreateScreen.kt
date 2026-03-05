@file:Suppress("TopLevelPropertyNaming")

package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.state_creating
import tech.dokus.aura.resources.workspace_create_button
import tech.dokus.domain.LegalName
import tech.dokus.features.auth.mvi.WorkspaceCreateIntent
import tech.dokus.features.auth.mvi.WorkspaceCreateState
import tech.dokus.features.auth.presentation.auth.components.steps.CompanyNameStep
import tech.dokus.features.auth.presentation.auth.components.steps.TypeSelectionStep
import tech.dokus.features.auth.presentation.auth.components.steps.VatAndAddressStep
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingCenteredShell
import tech.dokus.features.auth.presentation.auth.model.WorkspaceCreateType
import tech.dokus.features.auth.presentation.auth.model.WorkspaceWizardStep
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.background.WarpJumpEffect
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.dismissKeyboardOnTapOutside

private const val ContentFadeOutDurationMs = 600
private const val NavigationDelayMs = 100L
private const val CompanyLookupDebounceMs = 300L
private const val CompanyLookupMinCharacters = 3
private val StepContentMinHeight = 320.dp
private val WizardContentMaxWidth = 520.dp
private val WizardTypeSelectionMaxWidth = 560.dp
private val WizardLookupMaxWidth = 760.dp

@Composable
internal fun WorkspaceCreateScreen(
    state: WorkspaceCreateState,
    onIntent: (WorkspaceCreateIntent) -> Unit,
    onNavigateUp: () -> Unit,
    triggerWarp: Boolean,
    onWarpComplete: () -> Unit,
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
    val isSubmitting = state is WorkspaceCreateState.Loading || state is WorkspaceCreateState.Creating

    Scaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(ContentFadeOutDurationMs)),
            ) {
                if (wizardState != null) {
                    OnboardingCenteredShell(
                        modifier = Modifier.dismissKeyboardOnTapOutside()
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
                WorkspaceWizardStep.VatAndAddress -> WizardContentMaxWidth
            }
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = StepContentMinHeight)
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
                                onIntent(WorkspaceCreateIntent.UpdateCompanyName(LegalName(wizardState.companyName.value)))
                                onIntent(WorkspaceCreateIntent.EnterManually)
                            },
                            onBackPress = { onIntent(WorkspaceCreateIntent.BackClicked) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    WorkspaceWizardStep.VatAndAddress -> {
                        VatAndAddressStep(
                            vatNumber = wizardState.vatNumber,
                            address = wizardState.address,
                            onVatNumberChanged = { vatNumber ->
                                onIntent(WorkspaceCreateIntent.UpdateVatNumber(vatNumber))
                            },
                            onAddressChanged = { address ->
                                onIntent(WorkspaceCreateIntent.UpdateAddress(address))
                            },
                            onBackPress = { onIntent(WorkspaceCreateIntent.BackClicked) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (wizardState.step == WorkspaceWizardStep.VatAndAddress) {
            Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))

            PPrimaryButton(
                text = if (isSubmitting) {
                    stringResource(Res.string.state_creating)
                } else {
                    stringResource(Res.string.workspace_create_button)
                },
                enabled = wizardState.canProceed && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onIntent(WorkspaceCreateIntent.NextClicked) },
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun WorkspaceCreateScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        WorkspaceCreateScreen(
            state = WorkspaceCreateState.Wizard(),
            onIntent = {},
            onNavigateUp = {},
            triggerWarp = false,
            onWarpComplete = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Workspace Create Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun WorkspaceCreateScreenDesktopPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        WorkspaceCreateScreen(
            state = WorkspaceCreateState.Wizard(
                workspaceType = WorkspaceCreateType.Bookkeeper,
            ),
            onIntent = {},
            onNavigateUp = {},
            triggerWarp = false,
            onWarpComplete = {},
        )
    }
}
