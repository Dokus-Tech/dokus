package tech.dokus.features.auth.presentation.auth.screen

import tech.dokus.features.auth.presentation.auth.components.EntityConfirmationDialog
import tech.dokus.features.auth.presentation.auth.components.steps.CompanyNameStep
import tech.dokus.features.auth.presentation.auth.components.steps.TypeSelectionStep
import tech.dokus.features.auth.presentation.auth.components.steps.VatAndAddressStep
import tech.dokus.features.auth.presentation.auth.model.WorkspaceWizardStep
import tech.dokus.features.auth.mvi.WorkspaceCreateIntent
import tech.dokus.features.auth.mvi.WorkspaceCreateState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_continue
import tech.dokus.aura.resources.auth_step_of
import tech.dokus.aura.resources.state_creating
import tech.dokus.aura.resources.workspace_create_button
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.background.EnhancedFloatingBubbles
import tech.dokus.foundation.aura.components.background.WarpJumpEffect
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.components.text.CopyRightText
import tech.dokus.foundation.aura.constrains.limitWidth
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.constrains.withVerticalPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private val stepContentMinHeight = 320.dp

@Composable
internal fun WorkspaceCreateScreen(
    state: WorkspaceCreateState,
    onIntent: (WorkspaceCreateIntent) -> Unit,
    onNavigateUp: () -> Unit,
    triggerWarp: Boolean,
    onWarpComplete: () -> Unit,
) {
    // Warp animation state
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
            delay(100)
            onWarpComplete()
        }
    }

    val wizardState = state as? WorkspaceCreateState.Wizard
    val isSubmitting = state is WorkspaceCreateState.Loading || state is WorkspaceCreateState.Creating

    Scaffold { contentPadding ->
        Box(
            modifier = Modifier
                .padding(
                    bottom = contentPadding.calculateBottomPadding(),
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                    top = contentPadding.calculateTopPadding(),
                )
                .fillMaxSize()
        ) {
            // Background effects with fade animation
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(800))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    EnhancedFloatingBubbles()
                }
            }

            // Main content with fade animation
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(600))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (wizardState != null) {
                        WorkspaceCreateContent(
                            wizardState = wizardState,
                            isSubmitting = isSubmitting,
                            onIntent = onIntent,
                            onBackPress = onNavigateUp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            // Warp jump effect overlay
            WarpJumpEffect(
                isActive = isWarpActive,
                selectedItemPosition = null,
                onAnimationComplete = {
                    shouldNavigate = true
                }
            )

            // Entity confirmation dialog
            if (wizardState != null) {
                EntityConfirmationDialog(
                    state = wizardState.confirmationState,
                    onEntitySelected = { entity ->
                        onIntent(WorkspaceCreateIntent.SelectEntity(entity))
                    },
                    onEnterManually = {
                        onIntent(WorkspaceCreateIntent.EnterManually)
                    },
                    onDismiss = {
                        onIntent(WorkspaceCreateIntent.DismissConfirmation)
                    }
                )
            }
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
    // Pager state - number of pages depends on tenant type
    val steps = WorkspaceWizardStep.stepsForType(wizardState.tenantType)
    val pagerState = rememberPagerState(pageCount = { steps.size })

    // Sync pager with wizard state
    LaunchedEffect(wizardState.step) {
        val targetPage = steps.indexOf(wizardState.step)
        if (targetPage >= 0 && targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Column(
        modifier = modifier
            .withVerticalPadding()
            .limitWidth()
            .padding(horizontal = 16.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AppNameText()

        Column(
            modifier = Modifier.limitWidthCenteredContent(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step indicator
            Text(
                text = stringResource(
                    Res.string.auth_step_of,
                    wizardState.currentStepNumber,
                    wizardState.totalSteps
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step content with pager
            Box(
                modifier = Modifier
                    .heightIn(min = stepContentMinHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val step = steps[page]
                    when (step) {
                        WorkspaceWizardStep.TypeSelection -> {
                            TypeSelectionStep(
                                selectedType = wizardState.tenantType,
                                hasFreelancerWorkspace = wizardState.hasFreelancerWorkspace,
                                onTypeSelected = { type ->
                                    onIntent(WorkspaceCreateIntent.SelectType(type))
                                    onIntent(WorkspaceCreateIntent.NextClicked)
                                },
                                onBackPress = onBackPress,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        WorkspaceWizardStep.CompanyName -> {
                            CompanyNameStep(
                                companyName = wizardState.companyName,
                                lookupState = wizardState.lookupState,
                                onCompanyNameChanged = { name ->
                                    onIntent(WorkspaceCreateIntent.UpdateCompanyName(name))
                                },
                                onBackPress = { onIntent(WorkspaceCreateIntent.BackClicked) },
                                modifier = Modifier.fillMaxWidth()
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
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Navigation buttons (not needed for TypeSelection step)
            if (wizardState.step != WorkspaceWizardStep.TypeSelection) {
                Spacer(modifier = Modifier.height(24.dp))

                PPrimaryButton(
                    text = when (wizardState.step) {
                        WorkspaceWizardStep.VatAndAddress -> if (isSubmitting) {
                            stringResource(Res.string.state_creating)
                        } else {
                            stringResource(Res.string.workspace_create_button)
                        }

                        else -> stringResource(Res.string.action_continue)
                    },
                    enabled = wizardState.canProceed && !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onIntent(WorkspaceCreateIntent.NextClicked) }
                )
            }
        }

        CopyRightText()
    }
}
