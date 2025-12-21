package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.EntityConfirmationDialog
import ai.dokus.app.auth.components.steps.CompanyNameStep
import ai.dokus.app.auth.components.steps.TypeSelectionStep
import ai.dokus.app.auth.components.steps.VatAndAddressStep
import ai.dokus.app.auth.model.WorkspaceWizardStep
import ai.dokus.app.auth.viewmodel.WorkspaceCreateViewModel
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.WarpJumpEffect
import ai.dokus.foundation.design.components.text.AppNameText
import ai.dokus.foundation.design.components.text.CopyRightText
import ai.dokus.foundation.design.constrains.limitWidth
import ai.dokus.foundation.design.constrains.limitWidthCenteredContent
import ai.dokus.foundation.design.constrains.withVerticalPadding
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.replace
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import tech.dokus.foundation.app.state.DokusState

private val stepContentMinHeight = 320.dp

@Composable
internal fun WorkspaceCreateScreen(
    viewModel: WorkspaceCreateViewModel = koinViewModel()
) {
    val navController = LocalNavController.current

    val state by viewModel.state.collectAsState()
    val hasFreelancerWorkspace by viewModel.hasFreelancerWorkspace.collectAsState()
    val wizardState by viewModel.wizardState.collectAsState()
    val confirmationState by viewModel.confirmationState.collectAsState()

    // Warp animation state
    var isWarpActive by remember { mutableStateOf(false) }
    var shouldNavigate by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(true) }

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

    // Handle navigation after warp animation
    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            delay(100)
            navController.replace(CoreDestination.Home)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WorkspaceCreateViewModel.Effect.NavigateHome -> {
                    isWarpActive = true
                    contentVisible = false
                }
                is WorkspaceCreateViewModel.Effect.CreationFailed -> Unit
            }
        }
    }

    val isSubmitting = state is DokusState.Loading || wizardState.isCreating

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
                    Column(
                        modifier = Modifier
                            .withVerticalPadding()
                            .limitWidth()
                            .padding(horizontal = 16.dp)
                            .fillMaxHeight()
                            .align(Alignment.Center),
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
                                text = "Step ${wizardState.currentStepNumber} of ${wizardState.totalSteps}",
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
                                                hasFreelancerWorkspace = hasFreelancerWorkspace,
                                                onTypeSelected = { type ->
                                                    viewModel.onTypeSelected(type)
                                                    viewModel.goNext()
                                                },
                                                onBackPress = { navController.navigateUp() },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        WorkspaceWizardStep.CompanyName -> {
                                            CompanyNameStep(
                                                companyName = wizardState.companyName,
                                                lookupState = wizardState.lookupState,
                                                onCompanyNameChanged = viewModel::onCompanyNameChanged,
                                                onBackPress = { viewModel.goBack() },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        WorkspaceWizardStep.VatAndAddress -> {
                                            VatAndAddressStep(
                                                vatNumber = wizardState.vatNumber,
                                                address = wizardState.address,
                                                onVatNumberChanged = viewModel::onVatNumberChanged,
                                                onAddressChanged = viewModel::onAddressChanged,
                                                onBackPress = { viewModel.goBack() },
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
                                        WorkspaceWizardStep.VatAndAddress -> if (isSubmitting) "Creating..." else "Create workspace"
                                        else -> "Continue"
                                    },
                                    enabled = wizardState.canProceed && !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { viewModel.goNext() }
                                )
                            }
                        }

                        CopyRightText()
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
            EntityConfirmationDialog(
                state = confirmationState,
                onEntitySelected = viewModel::onEntitySelected,
                onEnterManually = viewModel::onEnterManually,
                onDismiss = viewModel::dismissConfirmation
            )
        }
    }
}
