package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.CompanyCreateContent
import ai.dokus.app.auth.viewmodel.CompanyCreateViewModel
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.WarpJumpEffect
import ai.dokus.foundation.design.components.text.AppNameText
import ai.dokus.foundation.design.components.text.CopyRightText
import ai.dokus.foundation.design.constrains.limitWidth
import ai.dokus.foundation.design.constrains.limitWidthCenteredContent
import ai.dokus.foundation.design.constrains.withVerticalPadding
import ai.dokus.foundation.domain.DisplayName
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.ids.VatNumber
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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

@Composable
internal fun CompanyCreateScreen(
    viewModel: CompanyCreateViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsState()

    // Warp animation state
    var isWarpActive by remember { mutableStateOf(false) }
    var shouldNavigate by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(true) }

    // Handle navigation after warp animation
    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            delay(100) // Small delay for smooth transition
            navController.replace(CoreDestination.Home)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CompanyCreateViewModel.Effect.NavigateHome -> {
                    // Trigger warp animation instead of immediate navigation
                    isWarpActive = true
                    contentVisible = false
                }

                is CompanyCreateViewModel.Effect.CreationFailed -> Unit
            }
        }
    }

    var tenantType by remember { mutableStateOf(TenantType.Company) }
    var legalName by remember { mutableStateOf(LegalName("")) }
    var displayName by remember { mutableStateOf(DisplayName("")) }
    var vatNumber by remember { mutableStateOf(VatNumber("")) }
    var country by remember { mutableStateOf(Country.Belgium) }

    val isSubmitting = state is ai.dokus.app.core.state.DokusState.Loading

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

                        CompanyCreateContent(
                            tenantType = tenantType,
                            legalName = legalName,
                            displayName = displayName,
                            vatNumber = vatNumber,
                            country = country,
                            isSubmitting = isSubmitting,
                            onTenantTypeChange = { tenantType = it },
                            onLegalNameChange = { legalName = it },
                            onDisplayNameChange = { displayName = it },
                            onVatNumberChange = { vatNumber = it },
                            onCountryChange = { country = it },
                            onSubmit = {
                                viewModel.createTenant(
                                    type = tenantType,
                                    legalName = legalName,
                                    displayName = displayName,
                                    plan = TenantPlan.Free,
                                    country = country,
                                    language = Language.En,
                                    vatNumber = vatNumber
                                )
                            },
                            modifier = Modifier.limitWidthCenteredContent()
                        )

                        CopyRightText()
                    }
                }
            }

            // Warp jump effect overlay
            WarpJumpEffect(
                isActive = isWarpActive,
                selectedItemPosition = null, // Start from center for company creation
                onAnimationComplete = {
                    shouldNavigate = true
                }
            )
        }
    }
}
