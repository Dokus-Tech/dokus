package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.EntityConfirmationDialog
import ai.dokus.app.auth.components.steps.CompanyNameStep
import ai.dokus.app.auth.components.steps.TypeSelectionStep
import ai.dokus.app.auth.components.steps.VatAndAddressStep
import ai.dokus.app.auth.model.WorkspaceWizardStep
import ai.dokus.app.auth.viewmodel.WorkspaceCreateAction
import ai.dokus.app.auth.viewmodel.WorkspaceCreateContainer
import ai.dokus.app.auth.viewmodel.WorkspaceCreateIntent
import ai.dokus.app.auth.viewmodel.WorkspaceCreateState
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
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.replace
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
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.foundation.app.mvi.container

private val stepContentMinHeight = 320.dp

@Composable
internal fun WorkspaceCreateScreen(
    container: WorkspaceCreateContainer = container()
) {
    val navController = LocalNavController.current

    // Warp animation state
    var isWarpActive by remember { mutableStateOf(false) }
    var shouldNavigate by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(true) }

    with(container.store) {
        val state by subscribe { action ->
            when (action) {
                WorkspaceCreateAction.NavigateHome -> {
                    isWarpActive = true
                    contentVisible = false
                }

                WorkspaceCreateAction.NavigateBack -> {
                    navController.navigateUp()
                }

                is WorkspaceCreateAction.ShowCreationError -> {
                    // Handle creation error - could show snackbar
                }
            }
        }

        // Handle navigation after warp animation
        LaunchedEffect(shouldNavigate) {
            if (shouldNavigate) {
                delay(100)
                navController.replace(CoreDestination.Home)
            }
        }

        // Extract wizard state for easier access
        val wizardState = state as? WorkspaceCreateState.Wizard
        val isSubmitting =
            state is WorkspaceCreateState.Loading || state is WorkspaceCreateState.Creating

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
                                onBackPress = { navController.navigateUp() },
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
                            intent(WorkspaceCreateIntent.SelectEntity(entity))
                        },
                        onEnterManually = {
                            intent(WorkspaceCreateIntent.EnterManually)
                        },
                        onDismiss = {
                            intent(WorkspaceCreateIntent.DismissConfirmation)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IntentReceiver<WorkspaceCreateIntent>.WorkspaceCreateContent(
    wizardState: WorkspaceCreateState.Wizard,
    isSubmitting: Boolean,
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
                                    intent(WorkspaceCreateIntent.SelectType(type))
                                    intent(WorkspaceCreateIntent.NextClicked)
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
                                    intent(WorkspaceCreateIntent.UpdateCompanyName(name))
                                },
                                onBackPress = { intent(WorkspaceCreateIntent.BackClicked) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        WorkspaceWizardStep.VatAndAddress -> {
                            VatAndAddressStep(
                                vatNumber = wizardState.vatNumber,
                                address = wizardState.address,
                                onVatNumberChanged = { vatNumber ->
                                    intent(WorkspaceCreateIntent.UpdateVatNumber(vatNumber))
                                },
                                onAddressChanged = { address ->
                                    intent(WorkspaceCreateIntent.UpdateAddress(address))
                                },
                                onBackPress = { intent(WorkspaceCreateIntent.BackClicked) },
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
                    onClick = { intent(WorkspaceCreateIntent.NextClicked) }
                )
            }
        }

        CopyRightText()
    }
}
