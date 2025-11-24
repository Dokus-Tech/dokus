package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.CompanyCreateContent
import ai.dokus.app.auth.components.CompanyCreateLayout
import ai.dokus.app.auth.viewmodel.CompanyCreateViewModel
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.SpotlightFollowEffect
import ai.dokus.foundation.design.components.background.WarpJumpEffect
import ai.dokus.foundation.design.constrains.isLargeScreen
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.replace
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    var legalName by remember { mutableStateOf(LegalName("")) }
    var email by remember { mutableStateOf(Email("")) }
    var vatNumber by remember { mutableStateOf(VatNumber("")) }
    var country by remember { mutableStateOf(Country.Belgium) }

    val isSubmitting = state is ai.dokus.app.core.state.DokusState.Loading

    Box(modifier = Modifier.fillMaxSize()) {
        // Background effects with fade animation
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(800))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                EnhancedFloatingBubbles()
                SpotlightFollowEffect()
            }
        }

        // Main content with fade animation
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(600))
        ) {
            CompanyCreateContent(
                layout = if (isLargeScreen) CompanyCreateLayout.Desktop else CompanyCreateLayout.Mobile,
                state = state,
                legalName = legalName,
                email = email,
                vatNumber = vatNumber,
                country = country,
                isSubmitting = isSubmitting,
                onLegalNameChange = { legalName = it },
                onEmailChange = { email = it },
                onVatNumberChange = { vatNumber = it },
                onCountryChange = { country = it },
                onSubmit = {
                    viewModel.createOrganization(
                        legalName = legalName,
                        email = email,
                        plan = OrganizationPlan.Free,
                        country = country,
                        language = Language.En,
                        vatNumber = vatNumber
                    )
                }
            )
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